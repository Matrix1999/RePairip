package com.antik.crc32;

import static com.antik.crc32.Casting.unsignedInt;

import com.antik.crc32.hyper.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * Created by aantik
 * 3/16/2026 9:00 PM
 *
 *   ⋆    ႔ ႔
 *     ᠸ^ ^ ⸝⸝
 *       |、˜〵
 *      じしˍ,)⁐̤ᐷ
 *
 * Fox Mode 🍺
 */

public class crc32  extends  JVMCasting {

    public static Map<String, long[]> readDexCrcInfo(File apkFile) throws IOException {
        Map<String, long[]> dexInfoMap = new LinkedHashMap<String, long[]>();
        RandomAccessFile file = new RandomAccessFile(apkFile, "r");

        try {
            long endOfCentralDirectory = findEndOfCentralDirectory(file);
            if (endOfCentralDirectory < 0) {
                return dexInfoMap;
            }

            file.seek(endOfCentralDirectory + Directory.CENTRAL_DIRECTORY_SIZE.offset);
            long centralDirectorySize = unsignedInt(readIntLE(file));

            file.seek(endOfCentralDirectory + Directory.CENTRAL_DIRECTORY_OFFSET.offset);
            long centralDirectoryOffset = unsignedInt(readIntLE(file));

            long headerPosition = centralDirectoryOffset;
            long centralDirectoryEnd = centralDirectoryOffset + centralDirectorySize;

            while (headerPosition < centralDirectoryEnd) {
                file.seek(headerPosition);

                if (readIntLE(file) != ZipSignature.CENTRAL_DIRECTORY_HEADER.value) {
                    break;
                }
                file.seek(headerPosition + DHeader.CRC32_OFFSET.offset);
                long crc32Value = unsignedInt(readIntLE(file));

                file.seek(headerPosition + DHeader.FILE_NAME_LENGTH.offset);
                int fileNameLength = readShortLE(file);

                file.seek(headerPosition + DHeader.EXTRA_FIELD_LENGTH.offset);
                int extraFieldLength = readShortLE(file);

                file.seek(headerPosition + DHeader.COMMENT_LENGTH.offset);
                int commentLength = readShortLE(file);

                file.seek(headerPosition + DHeader.LOCAL_HEADER_OFFSET.offset);
                long localHeaderOffset = unsignedInt(readIntLE(file));

                byte[] fileNameBytes = new byte[fileNameLength];
                file.seek(headerPosition + DHeader.HEADER_SIZE.offset);
                file.readFully(fileNameBytes);

                String fileName = new String(fileNameBytes, "UTF-8");

                if (fileName.startsWith("classes") && fileName.endsWith(".dex")) {

                    long[] info = new long[3];
                    info[0] = headerPosition + DHeader.CRC32_OFFSET.offset;
                    info[1] = localHeaderOffset + TFileHeader.CRC32_OFFSET.offset;
                    info[2] = crc32Value;
                    dexInfoMap.put(fileName, info);

                }
                headerPosition = headerPosition + DHeader.HEADER_SIZE.offset + fileNameLength + extraFieldLength + commentLength;
            }
        } finally {
            file.close();
        }
        return dexInfoMap;
    }

    public static void patch(File sourceApk, File targetApk) throws IOException {
        Map<String, long[]> sourceDexInfo = readDexCrcInfo(sourceApk);
        Map<String, long[]> targetDexInfo = readDexCrcInfo(targetApk);

        if (sourceDexInfo.isEmpty() || targetDexInfo.isEmpty()) {
            System.out.println("[CRC] No dex files found");
            return;
        }

        RandomAccessFile targetFile = new RandomAccessFile(targetApk, "rw");
        boolean patched = false;

        try {
            Iterator<String> it = sourceDexInfo.keySet().iterator();
            while (it.hasNext()) {
                String dexName = it.next();
                long[] sourceInfo = sourceDexInfo.get(dexName);
                long[] targetInfo = targetDexInfo.get(dexName);

                if (targetInfo != null && targetInfo[2] != sourceInfo[2]) {
                    int newCrc32 = (int) sourceInfo[2];

                    targetFile.seek(targetInfo[0]);
                    writeIntLE(targetFile, newCrc32);

                    targetFile.seek(targetInfo[1]);
                    writeIntLE(targetFile, newCrc32);

                    patched = true;
                    System.out.println("[CRC] Patched: " + dexName);
                }
            }
        } finally {
            targetFile.close();
        }

        if (patched) {
            System.out.println("[CRC] Patched dex CRC values from source APK");
        }
    }
}
