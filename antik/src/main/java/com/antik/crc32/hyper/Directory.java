package com.antik.crc32.hyper;

public enum Directory {
    CENTRAL_DIRECTORY_SIZE(12),
    CENTRAL_DIRECTORY_OFFSET(16);

    public final int offset;

    Directory(int offset) {
        this.offset = offset;
    }
}
