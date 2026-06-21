package com.antik.utils;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.Archive;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.RenamedInputSource;
import com.reandroid.archive.WriteProgress;
import com.reandroid.archive.io.ArchiveFileEntrySource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ALL")
public class loading {

    private static final int BAR_SIZE = 36;

    public static void progress(final ApkModule apk, final File out) throws IOException {
        long passBytes = 0;
        long workBytes = 0;
        long headerBytes = 64;

        InputSource[] sources = apk.getZipEntryMap().toArray(true);

        for (InputSource source : sources) {
            String name = source.getAlias();
            headerBytes += 92 + (name.getBytes(StandardCharsets.UTF_8).length * 2L);

            ArchiveFileEntrySource oldSource = null;

            if (source instanceof ArchiveFileEntrySource) {
                oldSource = (ArchiveFileEntrySource) source;
            } else if (source instanceof RenamedInputSource) {
                oldSource = (ArchiveFileEntrySource) ((RenamedInputSource) source).getParentInputSource(ArchiveFileEntrySource.class);
            }

            long len = Math.max(0, source.getLength());

            if (oldSource != null && oldSource.getMethod() == source.getMethod()) {
                passBytes += len;
            } else if (source.getMethod() != Archive.DEFLATED) {
                workBytes += len;
            } else {
                long oldLen = oldSource != null ? oldSource.getLength() : 0;
                workBytes += Math.max(len, oldLen);
            }
        }

        final long totalEstimate = Math.max(1, passBytes + workBytes + headerBytes);
        final long compressEstimate = Math.max(1, workBytes);

        final AtomicLong compressNow = new AtomicLong(0);
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    apk.writeApk(out, new WriteProgress() {
                        @Override
                        public void onCompressFile(String path, int method, long bytes) {
                            compressNow.set(bytes);
                        }
                    });
                } catch (Throwable t) {
                    error.set(t);
                }
            }
        });

        writer.start();

        int shown = 0;

        try {
            while (writer.isAlive()) {
                long fileSize = out.exists() ? out.length() : 0;
                long compressed = compressNow.get();

                int realPercent;

                if (workBytes <= 0) {
                    realPercent = (int) Math.round(
                            (Math.min(fileSize, totalEstimate) * 99.0) / totalEstimate
                    );
                } else if (fileSize <= 0) {
                    realPercent = (int) Math.round(
                            (Math.min(compressed, compressEstimate) * 72.0) / compressEstimate
                    );
                } else {
                    long dynamicTotal = Math.max(
                            passBytes + Math.max(compressed, workBytes) + headerBytes,
                            fileSize
                    );

                    realPercent = 72 + (int) Math.round(
                            (Math.min(fileSize, dynamicTotal) * 27.0) / dynamicTotal
                    );
                }

                if (realPercent > 99) {
                    realPercent = 99;
                }

                if (realPercent > shown) {
                    int gap = realPercent - shown;

                    if (gap >= 20) {
                        shown += 4;
                    } else if (gap >= 10) {
                        shown += 3;
                    } else if (gap >= 4) {
                        shown += 2;
                    } else {
                        shown++;
                    }

                    if (shown > realPercent) {
                        shown = realPercent;
                    }
                }

                StringBuilder bar = new StringBuilder("\r[");
                int fill = shown * BAR_SIZE / 100;

                for (int i = 0; i < BAR_SIZE; i++) {
                    bar.append(i < fill ? "#" : "-");
                }

                bar.append("] ");
                bar.append(String.format("%3d%%", shown));

                System.out.print(bar.toString());
                System.out.flush();

                Thread.sleep(100);
            }

            writer.join();

        } catch (InterruptedException e) {
            writer.interrupt();
            throw new IOException(e);
        }

        Throwable t = error.get();

        if (t != null) {
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            throw new IOException(t);
        }

        while (shown < 100) {
            shown++;

            StringBuilder bar = new StringBuilder("\r[");
            int fill = shown * BAR_SIZE / 100;

            for (int i = 0; i < BAR_SIZE; i++) {
                bar.append(i < fill ? "#" : "-");
            }

            bar.append("] ");
            bar.append(String.format("%3d%%", shown));

            System.out.print(bar.toString());
            System.out.flush();

            try {
                Thread.sleep(18);
            } catch (Exception ignored) {
            }
        }

        System.out.println();
    }
}
