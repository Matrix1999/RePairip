package com.antik.crc32.hyper;

public enum ZipSignature {
    CENTRAL_DIRECTORY_HEADER(0x02014b50),
    END_OF_CENTRAL_DIRECTORY(0x06054b50);

    public final int value;

    ZipSignature(int value) {
        this.value = value;
    }
}
