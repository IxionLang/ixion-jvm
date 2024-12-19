package ixion.std;


import lombok.Getter;

public class string{

    @Getter
    private String str;
    private int len;

    public string(String str){
        this.str = str;
        this.len = this.getLen();
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

    public void removeItem(String symbol){
        this.str = this.str.replace(symbol, "");
    }

    public void joinFont(String symbol){
        this.str = symbol + this.str;
    }

    public int getLen(){
        return str.length();
    }

}
