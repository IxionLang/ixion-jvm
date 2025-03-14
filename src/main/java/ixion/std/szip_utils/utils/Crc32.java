package ixion.std.szip_utils.utils;

import ixion.std.szip_utils.Table;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

public final class Crc32 {

    static Table ta = new Table();
    public static int getCrc(File file) throws IOException {
        long t1 = new Date().getTime();
        int result = 0x0;
        byte[] b = new byte[65536];
        FileInputStream inputStream = new FileInputStream(file);
        int c = 0;
        while ((c = inputStream.read(b)) != -1) {
            result = crc(b, c, result);
        }
        result = ~result;
        inputStream.close();
        long t2 = new Date().getTime();
        System.out.println("CRC32 (via table lookup)       = " + file + ";" + Integer.toHexString(result)+";"+(t2-t1));
        return result;
    }

    private static int crc(byte[] bytes, int c, int initCrc) {
        int crc2 = initCrc != 0 ? initCrc : 0xffffffff;
        for (int i = 0; i < c; i++) {
            byte b = bytes[i];
            crc2 = (crc2 >>> 8) ^ ta.table[(crc2 ^ b) & 0xff];
        }
        return crc2;
    }
}
