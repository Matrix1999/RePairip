package com.antik.crc32.hyper;

public enum DHeader {
    CRC32_OFFSET(16),
    FILE_NAME_LENGTH(28),
    EXTRA_FIELD_LENGTH(30),
    COMMENT_LENGTH(32),
    LOCAL_HEADER_OFFSET(42),
    HEADER_SIZE(46);

    public final int offset;

    DHeader(int offset) {
        this.offset = offset;
    }
}
