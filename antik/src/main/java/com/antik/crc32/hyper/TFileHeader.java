package com.antik.crc32.hyper;

public enum TFileHeader {
    CRC32_OFFSET(14);

    public final int offset;

    TFileHeader(int offset) {
        this.offset = offset;
    }
}
