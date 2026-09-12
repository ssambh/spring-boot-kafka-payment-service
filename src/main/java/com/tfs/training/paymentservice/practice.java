package com.tfs.training.paymentservice;

import ch.qos.logback.core.net.SyslogOutputStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

interface interfaceA {
    default void play() {
        System.out.println("this is Interface A");
    }
}
interface interfaceB {
    default void play() {
        System.out.println("this is Interface B");
    }
}

public final class practice{

    static final int a = 20;
    static void reverseArray(int[] arr, int start, int end){
        if(start >= end) {
            return;
        }
        int temp = arr[start];
        arr[start] = arr[end];
        arr[end] = temp;

        reverseArray(arr, start + 1, end - 1);
    }

    public static void main(String[] args){
        Set<Integer> set = new HashSet<>();

    }
}

class dog{

}
