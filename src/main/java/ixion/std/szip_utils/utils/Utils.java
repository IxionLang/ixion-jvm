package ixion.std.szip_utils.utils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;

public final class Utils {

    private static final int YEAR_POSITION = 9;
    private static final int MONTH_POSITION = 5;

    private static final int HOUR_POSITION = 11;
    private static final int MINUTE_POSITION = 5;

    public static byte[] makeArray(byte[]... srcs) {
        int length = 0;
        for (byte[] src : srcs) {
            length += src.length;
        }
        return new byte[length];
    }

    public static void arraysCopy(byte[] dest, byte[]... srcs) {
        int destPos = 0;
        for (byte[] src : srcs) {
            destPos = arrayCopy(dest, src, destPos);
        }
    }

    private static int arrayCopy(byte[] dest, byte[] src, int destPos) {
        System.arraycopy(src, 0, dest, destPos, src.length);
        return destPos + src.length;
    }

    public static byte[] toByteArray(long value, int length) {
        return Arrays.copyOf(BigInteger.valueOf(Long.reverseBytes(value)).toByteArray(), length);
    }

    public static byte[] toLittleEndianByteArray(long value, int length) {
        return Arrays.copyOf(BigInteger.valueOf(value).toByteArray(), length);
    }

    public static long getDate(long modified) {
        long result = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(modified);

        long year = calendar.get(Calendar.YEAR);
        long month = calendar.get(Calendar.MONTH) + 1;
        long day = calendar.get(Calendar.DAY_OF_MONTH);

        result = (year - 1980) << YEAR_POSITION;
        result = result | month << MONTH_POSITION;
        result = result | day;

        return result;
    }

    public static long getTime(long modified) {
        long result = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(modified);

        long hour = calendar.get(Calendar.HOUR_OF_DAY);
        long minute = calendar.get(Calendar.MINUTE);
        long second = calendar.get(Calendar.SECOND);

        result = hour << HOUR_POSITION;
        result = result | minute << MINUTE_POSITION;
        result = result | (second / 2);

        return result;
    }

}
