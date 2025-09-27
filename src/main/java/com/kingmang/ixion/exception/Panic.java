package com.kingmang.ixion.exception;


public class Panic {
    private final String message;
    private static final String R = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    public Panic(String message){
        this.message = message;
    }

    public void send(){
        System.out.println(R.concat("panic: ".concat(message)).concat(RESET));
        System.exit(1);
    }
}
