package com.antik.crc32;

import com.antik.crc32.hyper.ZipSignature;
import java.io.IOException;
import java.io.RandomAccessFile;

public class JVMCasting {

    public static int readIntLE(RandomAccessFile file) throws IOException {
        int b1 = readByte(file);
        int b2 = readByte(file);
        int b3 = readByte(file);
        int b4 = readByte(file);
        return b1 | (b2 << Casting.BYTE_SHIFT_1) | (b3 << Casting.BYTE_SHIFT_2) | (b4 << Casting.BYTE_SHIFT_3);
    }
    public static int readShortLE(RandomAccessFile file) throws IOException {
        int b1 = readByte(file);
        int b2 = readByte(file);
        return b1 | (b2 << Casting.BYTE_SHIFT_1);
    }

    public static void writeIntLE(RandomAccessFile file, int value) throws IOException {
        file.write(value & Casting.BYTE_MASK);
        file.write((value >> Casting.BYTE_SHIFT_1) & Casting.BYTE_MASK);
        file.write((value >> Casting.BYTE_SHIFT_2) & Casting.BYTE_MASK);
        file.write((value >> Casting.BYTE_SHIFT_3) & Casting.BYTE_MASK);
    }

    public static long findEndOfCentralDirectory(RandomAccessFile file) throws IOException {
        long fileSize = file.length();
        long searchStart = fileSize - Casting.ZIP_EOCD_SEARCH_RANGE;
        if (searchStart < 0) {
            searchStart = 0;
        }
        long position = fileSize - Casting.INT_BYTE_COUNT;
        while (position >= searchStart) {
            file.seek(position);
            if (readIntLE(file) == ZipSignature.END_OF_CENTRAL_DIRECTORY.value) {
                return position;
            }
            position--;
        }

        return -1;
    }

    private static int readByte(RandomAccessFile file) throws IOException {
        int value = file.read();
        if (value < 0) {
            throw new IOException("Unexpected end of file");
        }
        return value & Casting.BYTE_MASK;
    }
}