package com.kingmang.ixion.exception;

public class Panic {

    private final String R = "\u001B[31m";
    private final String RESET = "\u001B[0m";
    private final String message;

    public Panic(String message){
        this.message = message;
    }

    public void send(){
        System.out.println(R + ("panic: " + message) + RESET);
        System.exit(1);
    }
}
