package com.kabir.milton;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
	// write your code here
        Scanner sc=new Scanner(System.in);
        String st=sc.nextLine();
        for(int i=0;i<st.length();i+=3){
            if(st.charAt(i)=='a'){
                st=st.substring(0,i)+"b"+st.substring(i+1);
            }
            else{
                st=st.substring(0,i)+"a"+st.substring(i+1);
            }
        }
        System.out.println(st);
    }
}
