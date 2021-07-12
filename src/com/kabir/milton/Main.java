package com.kabir.milton;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        file();
    }

    public static void file() {
        Filer file = new Filer();
        final Scanner scanner = new Scanner(System.in);

        System.out.println("Write a mode: ");
        switch (scanner.nextLine()) {
            case "encode":
                file.encodeFileHamming("send.txt",
                        "encoded.txt");
                break;
            case "send":
                file.sendFileWithErrors("encoded.txt",
                        "received.txt");
                break;
            case "decode":
                file.decodeFileHamming("received.txt",
                        "decoded.txt");
                break;
            default:
                break;
        }
    }
}

class Filer {
    private final int[] BIT_PAIRS = {0b11000000, 0b00110000, 0b00001100, 0b00000011};
    private final int[] BIT_PATTS = {0b10000000, 0b01000000, 0b00100000, 0b00010000,
            0b00001000, 0b00000100, 0b00000010, 0b00000001};

    public void sendFileWithErrors(String sendFile, String receiveFile) {
        Random rnd = new Random();
        try (FileInputStream infile = new FileInputStream(sendFile);
             FileOutputStream outfile = new FileOutputStream(receiveFile)) {
            for (int data = infile.read(); data != -1; data = infile.read()) {
                data ^= 1 << rnd.nextInt(8);
                outfile.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void encodeFile(String readFile, String writeFile) {
        byte data_out = 0;
        int parity = 0;
        int pairs = 0;
        boolean data_written = false;

        try (FileInputStream infile = new FileInputStream(readFile);
             FileOutputStream outfile = new FileOutputStream(writeFile)) {
            for (int data_in = infile.read(); data_in != -1; data_in = infile.read()) {
                for (int bit = 0; bit < 8; bit++) {
                    data_written = false;
                    if ((data_in & BIT_PATTS[bit]) != 0) {
                        data_out += BIT_PAIRS[pairs];
                        parity++;
                    }
                    if (++pairs == 3) {
                        if (parity % 2 == 1) {
                            data_out += BIT_PAIRS[pairs];
                        }
                        outfile.write(data_out);
                        data_written = true;
                        pairs = 0;
                        parity = 0;
                        data_out = 0b00000000;
                    }
                }
            }

            if (!data_written) {
                if (parity % 2 == 1) {
                    data_out += BIT_PAIRS[3];
                }
                outfile.write(data_out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void decodeFile(String receiveFile, String decodeFile) {
        int[] pairs = {0, 0, 0, 0};
        int bit = 0;
        byte data_out = 0;
        int parity;

        try (FileInputStream infile = new FileInputStream(receiveFile);
             FileOutputStream outfile = new FileOutputStream(decodeFile)) {
            for (int data_in = infile.read(); data_in != -1; data_in = infile.read()) {
                parity = 0;
                for (int pair = 0; pair < 3; pair++) {
                    pairs[pair] = data_in & BIT_PAIRS[pair];
                    parity += pairs[pair] == BIT_PAIRS[pair] ? 1 : 0;   // add to parity if the bit pair is correct
                }

                pairs[3] = data_in & BIT_PAIRS[3];
                parity %= 2;

                if (pairs[3] == 0 || pairs[3] == BIT_PAIRS[3]) { // parity pair is correct, so a data pair is wrong
                    int data_parity = pairs[3] == 0 ? 0 : 1;
                    for (int pair = 0; pair < 3; pair++) {
                        if (pairs[pair] > 0 && pairs[pair] != BIT_PAIRS[pair]) {
                            // found the incorrect data pair, set to 0 if the calculated parity and data parity match
                            pairs[pair] = (parity ^ data_parity) == 0 ? 0 : BIT_PAIRS[pair];
                            break;
                        }
                    }
                }
                for (int pair = 0; pair < 3; pair++) {
                    data_out += pairs[pair] == 0 ? 0 : BIT_PATTS[bit];
                    if (++bit == 8) {
                        outfile.write(data_out);
                        bit = 0;
                        data_out = 0;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void encodeFileHamming(String readFile, String writeFile) {
        int data_out;
        int nybble;

        try (FileInputStream infile = new FileInputStream(readFile);
             FileOutputStream outfile = new FileOutputStream(writeFile)) {
            for (int data_in = infile.read(); data_in != -1; data_in = infile.read()) {
                nybble = data_in & 0b11110000;
                nybble = nybble >>> 4;
                data_out = encodeHamming(nybble);
                outfile.write(data_out);
                nybble = data_in & 0b00001111;
                data_out = encodeHamming(nybble);
                outfile.write(data_out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void decodeFileHamming(String readFile, String writeFile) {
        int data_out;
        byte[] bytes_in = new byte[2];

        try (FileInputStream infile = new FileInputStream(readFile);
             FileOutputStream outfile = new FileOutputStream(writeFile)) {
            for (int data_in = infile.read(bytes_in); data_in != -1; data_in = infile.read(bytes_in)) {
                data_out = decodeHamming(bytes_in[0]);
                data_out = data_out << 4;
                data_out += decodeHamming(bytes_in[1]);
                outfile.write(data_out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int encodeHamming(int nybble) {
        int encoded = 0;
        int p1 = 0;
        int p2 = 0;
        int p4 = 0;

        if ((nybble & BIT_PATTS[4]) != 0) {
            encoded += BIT_PATTS[2];
            p1 ^= 1;
            p2 ^= 1;
        }

        if ((nybble & BIT_PATTS[5]) != 0) {
            encoded += BIT_PATTS[4];
            p1 ^= 1;
            p4 ^= 1;
        }

        if ((nybble & BIT_PATTS[6]) != 0) {
            encoded += BIT_PATTS[5];
            p2 ^= 1;
            p4 ^= 1;
        }

        if ((nybble & BIT_PATTS[7]) != 0) {
            encoded += BIT_PATTS[6];
            p1 ^= 1;
            p2 ^= 1;
            p4 ^= 1;
        }

        if (p1 != 0) {
            encoded += BIT_PATTS[0];
        }

        if (p2 != 0) {
            encoded += BIT_PATTS[1];
        }

        if (p4 != 0) {
            encoded += BIT_PATTS[3];
        }

        return encoded;
    }

    private int decodeHamming(int encoded) {
        int decoded = 0;
        int p1 = 0;
        int p2 = 0;
        int p4 = 0;

        if ((encoded & BIT_PATTS[2]) != 0) {
            decoded += BIT_PATTS[4];
            p1 ^= 1;
            p2 ^= 1;
        }

        if ((encoded & BIT_PATTS[4]) != 0) {
            decoded += BIT_PATTS[5];
            p1 ^= 1;
            p4 ^= 1;
        }

        if ((encoded & BIT_PATTS[5]) != 0) {
            decoded += BIT_PATTS[6];
            p2 ^= 1;
            p4 ^= 1;
        }

        if ((encoded & BIT_PATTS[6]) != 0) {
            decoded += BIT_PATTS[7];
            p1 ^= 1;
            p2 ^= 1;
            p4 ^= 1;
        }

        int error = 0;

        if ((p1 != 0) ^ ((encoded & BIT_PATTS[0]) != 0)) {
            error += 1;
        }

        if ((p2 != 0) ^ ((encoded & BIT_PATTS[1]) != 0)) {
            error += 2;
        }

        if ((p4 != 0) ^ ((encoded & BIT_PATTS[3]) != 0)) {
            error += 4;
        }

        switch (error) {
            case 3:
                decoded += (encoded & BIT_PATTS[2]) == 0 ? BIT_PATTS[4] : -BIT_PATTS[4];
                break;
            case 5:
                decoded += (encoded & BIT_PATTS[4]) == 0 ? BIT_PATTS[5] : -BIT_PATTS[5];
                break;
            case 6:
                decoded += (encoded & BIT_PATTS[5]) == 0 ? BIT_PATTS[6] : -BIT_PATTS[6];
                break;
            case 7:
                decoded += (encoded & BIT_PATTS[6]) == 0 ? BIT_PATTS[7] : -BIT_PATTS[7];
                break;
            default:
                break;
        }

        return decoded;
    }
}

class Message {
    private StringBuilder message;

    Message(String message) {
        this.message = new StringBuilder(message);
    }

    public void encodeMessage() {
        for (int i = message.length() - 1; i >= 0; i--) {
            message.insert(i, String.valueOf(message.charAt(i)).repeat(2));
        }
    }

    public void makeErrors() {
        Random rnd = new Random();
        String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < message.length(); i += 3) {
            message.setCharAt(i + rnd.nextInt(3),
                    charset.charAt(rnd.nextInt(charset.length())));
        }
    }

    public void decodeMessage() {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < message.length(); i += 3) {
            if (message.charAt(i) == message.charAt(i + 1)) {
                decoded.append(message.charAt(i));
            } else if (message.charAt(i) == message.charAt(i + 2)) {
                decoded.append(message.charAt(i));
            } else {
                decoded.append(message.charAt(i + 1));
            }
        }
        message = decoded;
    }

    public void print() {
        System.out.println(message.toString());
    }
}