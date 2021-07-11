package com.kabir.milton;

import java.io.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        stage4();
    }

    private static void stage4() {
        Scanner scanner = new Scanner(System.in);
        String inFileName;
        String outFileName;
        Transmitter transmitter = null;
        switch (scanner.next()) {
            case "encode":
                inFileName = "send.txt";
                outFileName = "encoded.txt";
                transmitter = new HammingEncoder(inFileName, outFileName);
                break;
            case "send":
                inFileName = "encoded.txt";
                outFileName = "received.txt";
                transmitter = new Scrambler(inFileName, outFileName);
                break;
            case "decode":
                inFileName = "received.txt";
                outFileName = "decoded.txt";
                transmitter = new HammingDecoder(inFileName, outFileName);
                break;
            default:
                break;
        }
        if (transmitter != null) {
            transmitter.transmit();
        }
    }
}

abstract class Transmitter {

    private final String inFileName;
    private final String outFileName;

    Transmitter(String inFileName, String outFileName) {
        this.inFileName = inFileName;
        this.outFileName = outFileName;
    }

    void transmit() {
        try (InputStream inFileStream = new FileInputStream(this.inFileName);
             OutputStream outFileStream = new FileOutputStream(this.outFileName);
             BufferedInputStream inStream = new BufferedInputStream(inFileStream);
             BufferedOutputStream outStream = new BufferedOutputStream(outFileStream)
        ) {
            send(inStream, outStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract void send(InputStream inputStream, OutputStream outputStream);
}

class Encoder extends Transmitter {

    Encoder(String inFileName, String outFileName) {
        super(inFileName, outFileName);
    }

    @Override
    public void send(InputStream inputStream, OutputStream outputStream) {

        int targetByte = 0;
        int targetBitmask = (1 << 7) | (1 << 6);
        int sourceBitmask = 1 << 7;

        int parity = 0;
        try {
            int sourceByte = inputStream.read();
            while (sourceByte != -1) {
                // if we've scanned 3 bits, set parity bits
                if (targetBitmask == 3) {
                    if (parity == 1) {
                        targetByte |= 3;
                    }
                    outputStream.write(targetByte);
                    parity = 0;
                    targetByte = 0;
                    targetBitmask = 3 << 6;
                } else {
                    if ((sourceByte & sourceBitmask) != 0) {
                        targetByte |= targetBitmask;
                        parity ^= 1;
                    }
                    targetBitmask >>= 2;
                    sourceBitmask >>= 1;
                    if (sourceBitmask == 0) {
                        sourceByte = inputStream.read();
                        sourceBitmask = 1 << 7;
                    }
                }
            }
            // write the unfinished byte, if any
            if (targetBitmask != (3 << 6)) {
                if (parity != 0) {
                    targetByte |= 3;
                }
                outputStream.write(targetByte);
            }
            outputStream.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class HammingEncoder extends Transmitter {

    HammingEncoder(String inFileName, String outFileName) {
        super(inFileName, outFileName);
    }

    @Override
    public void send(InputStream inputStream, OutputStream outputStream) {

        // testHammingEncoder();

        try {
            int sourceByte = inputStream.read();
            while (sourceByte != -1) {
                outputStream.write(createHammingByte((sourceByte & (0xf << 4)) >> 4));
                outputStream.write(createHammingByte(sourceByte & 0xf));
                sourceByte = inputStream.read();
            }
            outputStream.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // [p1, p2, d1, p3, d2, d3, d4, 0]
    // p1 ^ d1 ^ d2 ^ d4
    // p2 ^ d1 ^ d3 ^ d4
    // p3 ^ d2 ^ d3 ^ d4

    private int createHammingByte(int sourceByte) {

        int[] bits = new int[]{0, 0, 0, 0};
        int targetByte = 0;
        int targetByteMask = 2;
        int parity = 0;
        int val;

        for (int i = 3; i >= 1; i--) {
            val = sourceByte & 1;
            bits[i] = val;
            parity ^= val;
            sourceByte >>= 1;
            if (val == 1) targetByte |= targetByteMask;
            targetByteMask <<= 1;
        }

        // set p3
        if (parity == 1) targetByte |= targetByteMask;
        targetByteMask <<= 1;

        // set d1
        bits[0] = sourceByte & 1;
        if (bits[0] != 0) {
            targetByte |= targetByteMask;
        }

        // set p2
        targetByteMask <<= 1;
        parity = bits[0] ^ bits[2] ^ bits[3];
        if (parity == 1) targetByte |= targetByteMask;

        // set p1
        targetByteMask <<= 1;
        parity = bits[0] ^ bits[1] ^ bits[3];
        if (parity == 1) targetByte |= targetByteMask;

        return targetByte;
    }
}

class Scrambler extends Transmitter {

    Scrambler(String inFileName, String outFileName) {
        super(inFileName, outFileName);
    }

    @Override
    public void send(InputStream inputStream, OutputStream outputStream) {
        Random random = new Random(System.currentTimeMillis());
        try {
            int sourceByte = inputStream.read();
            while (sourceByte != -1) {
                int n = random.nextInt(8);

                int mask = 1 << n;
                if ((sourceByte & mask) == 0) {
                    sourceByte |= mask;
                } else {
                    sourceByte &= (~mask);
                }
                outputStream.write(sourceByte);
                sourceByte = inputStream.read();
            }
            outputStream.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class Decoder extends Transmitter {

    Decoder(String inFileName, String outFileName) {
        super(inFileName, outFileName);
    }

    @Override
    public void send(InputStream inputStream, OutputStream outputStream) {
        int targetByte = 0;
        int targetBitmask = 1 << 7;
        try {
            int sourceByte = inputStream.read();
            while (sourceByte != -1) {

                int[] bitPairs = new int[]{0, 0, 0, 0};

                // Read off pairs of bits
                int invalidPairIdx = 0;
                int parityCheck = 0;
                for (int i = 3; i >= 0; i--) {
                    int val = sourceByte & 3;
                    sourceByte >>= 2;
                    if (val == 3) bitPairs[i] = 1;
                    else if (val != 0) invalidPairIdx = i;
                    parityCheck ^= bitPairs[i];
                }
                if (parityCheck != 0) bitPairs[invalidPairIdx] = 1;

                for (int i = 0; i < 3; i++) {
                    if (bitPairs[i] == 1) targetByte |= targetBitmask;
                    targetBitmask >>= 1;
                    // target byte full, write it
                    if (targetBitmask == 0) {
                        outputStream.write(targetByte);
                        targetByte = 0;
                        targetBitmask = 1 << 7;
                    }
                }
                sourceByte = inputStream.read();
            }
            outputStream.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class HammingDecoder extends Transmitter {

    HammingDecoder(String inFileName, String outFileName) {
        super(inFileName, outFileName);
    }

    @Override
    public void send(InputStream inputStream, OutputStream outputStream) {
        int targetByte;
        byte[] sourceBytes = new byte[] {0, 0};
        try {
            while (inputStream.read(sourceBytes) == 2) {
                targetByte = decodeHalfByte(sourceBytes[0]) << 4;
                targetByte |= decodeHalfByte(sourceBytes[1]);
                outputStream.write(targetByte);
            }
            outputStream.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private int decodeHalfByte(byte sourceByte) {
        int targetByte = 0;
        int[] bits = new int[8];
        for (int i = 7; i >= 0; i--) {
            bits[i] = sourceByte & 1;
            sourceByte >>= 1;
        }

        // Correct error
        int g1 = bits[0] ^ bits[2] ^ bits[4] ^ bits[6];
        int g2 = bits[1] ^ bits[2] ^ bits[5] ^ bits[6];
        int g3 = bits[3] ^ bits[4] ^ bits[5] ^ bits[6];

        if ((g1 & g2 & g3) == 1) {
            bits[6] = ~bits[6];
        } else if ((g1 & g2) == 1) {
            bits[2] = ~bits[2];
        } else if ((g2 & g3) == 1) {
            bits[5] = ~bits[5];
        } else if ((g1 & g3) == 1) {
            bits[4] = ~bits[4];
        }

        // write corrected half-byte to targetByte
        targetByte |= bits[6] & 1;
        targetByte |= (bits[5] & 1) << 1;
        targetByte |= (bits[4] & 1) << 2;
        targetByte |= (bits[2] & 1) << 3;

        return targetByte;
    }
}