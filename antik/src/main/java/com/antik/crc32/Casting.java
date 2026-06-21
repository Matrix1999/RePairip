package com.antik.crc32;

import com.antik.logCat;

public final class Casting implements logCat {

   /* static {
    System.out.println(TAG + " Casting loaded  {} >>> ");
    }
*/
    private Casting() {
       /////////////////////
    }

    public static final long UNSIGNED_INT_MASK = 0xffffffffL;

    public static final int BYTE_MASK = 0xFF;

    public static final int BYTE_SHIFT_1 = 8;
    public static final int BYTE_SHIFT_2 = 16;
    public static final int BYTE_SHIFT_3 = 24;

    public static final int INT_BYTE_COUNT = 4;

    public static final long ZIP_MAX_COMMENT_SIZE = 65535L;
    public static final long ZIP_EOCD_MIN_SIZE = 22L;

    public static final long ZIP_EOCD_SEARCH_RANGE = ZIP_MAX_COMMENT_SIZE + ZIP_EOCD_MIN_SIZE;

    public static long unsignedInt(int value) {
        return ( (long) value) & UNSIGNED_INT_MASK;
    }
}