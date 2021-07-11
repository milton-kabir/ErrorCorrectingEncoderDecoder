package com.kabir.milton;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
	// write your code here
        Scanner sc=new Scanner(System.in);
        String st=sc.nextLine(),ts="";
        System.out.println(st);
        for(int i=0;i<st.length();i++){
            ts+=st.charAt(i);
            ts+=st.charAt(i);
            ts+=st.charAt(i);
        }
        System.out.println(ts);
        for(int i=0;i<ts.length();i+=3){
            if(ts.charAt(i)=='a'){
                ts=ts.substring(0,i)+"b"+ts.substring(i+1);
            }
            else{
                ts=ts.substring(0,i)+"a"+ts.substring(i+1);
            }
        }
        System.out.println(ts);
        System.out.println(st);
    }
}
