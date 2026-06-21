package com.Matrix;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static boolean enabled = true;
    private static PrintWriter writer = null;
    private static String logFilePath = null;

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void init(String pkg) {
        if (!enabled) return;
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String dir = "/sdcard/RePairip/logs";
            Files.createDirectories(Paths.get(dir));
            logFilePath = dir + "/" + pkg + "_" + ts + ".log";
            writer = new PrintWriter(new FileWriter(logFilePath, true), true);
            log("[LogFox] Session started — " + new Date());
            log("[LogFox] Package: " + pkg);
            log("[LogFox] Log saved to: " + logFilePath);
        } catch (Exception e) {
            System.err.println("[LogFox] Could not init log file: " + e.getMessage());
        }
    }

    public static void log(String msg) {
        System.out.println(msg);
        if (enabled && writer != null) {
            writer.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg);
        }
    }

    public static void warn(String msg) {
        log("[WARN] " + msg);
    }

    public static void error(String msg) {
        log("[ERROR] " + msg);
    }

    public static void close() {
        if (writer != null) {
            log("[LogFox] Session ended — " + new Date());
            writer.close();
            writer = null;
        }
        if (logFilePath != null) {
            System.out.println("[LogFox] Full log saved to: " + logFilePath);
        }
    }
}
