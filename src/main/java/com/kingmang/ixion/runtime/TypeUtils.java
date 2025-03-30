package com.kingmang.ixion.runtime;

public class TypeUtils {

    /*Строки*/
    public static String asString(char arg){
        return String.valueOf(arg);
    }
    public static String asString(int arg){
        return String.valueOf(arg);
    }
    public static String asString(float arg){
        return String.valueOf(arg);
    }
    public static String asString(double arg){
        return String.valueOf(arg);
    }
    public static String asString(short arg){
        return String.valueOf(arg);
    }
    public static String asString(long arg){
        return String.valueOf(arg);
    }
    /**/

    //Примитивы в объекты
    public static Integer Int(int arg){
        return arg;
    }

    public static Float Float(float arg){
        return arg;
    }
    /**/


    //Объекты в примитивы
    public static int toInt(Object arg){
        return (int) arg;
    }

    public static float toFloat(Object arg){
        return (float) arg;
    }
    /**/
}
