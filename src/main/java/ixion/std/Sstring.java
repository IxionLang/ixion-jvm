package ixion.std;


public class Sstring {

    private String str;
    private int len;

    public Sstring(String str){
        this.str = str;
        this.len = this.getLen();
    }

    public String getStr() {
        return str;
    }

    public void join(String str) {
        this.str += str;
        this.len = this.getLen();
    }

    public String reverse() {
        StringBuilder sb = new StringBuilder(str);
        return sb.reverse().toString();
    }

    public int count(String symbol){

        int count = 0;
        String[] arr = str.split("");

        for (int i = 0; i < arr.length; i++) {
            if(arr[i].equals(symbol)){
                count++;
            }
        }
        return count;
    }

    public int getLen(){
        return str.length();
    }

}
