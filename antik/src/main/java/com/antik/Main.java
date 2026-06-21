package com.antik;

import com.antik.DexPatcher.DexPatcher;
import com.antik.DexPatcher.Translation.TranslationPatcher;
import com.antik.crc32.crc32;
import com.antik.manifest.manifestP;
import com.antik.ui.*;
import com.antik.utils.*;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.WriteProgress;
import java.io.*;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) {

        if (args.length < 2) {
            help.help();
            return;
        }

        String inputPath    = null;
        String translatePath = null;
        boolean logEnabled  = true;   // LogFox ON by default

        for (int i = 0; i < args.length; i++) {
            if ("-i".equals(args[i]) && i + 1 < args.length) {
                inputPath = args[++i];
            } else if ("-t".equals(args[i]) && i + 1 < args.length) {
                translatePath = args[++i];
            } else if ("--no-log".equals(args[i])) {
                logEnabled = false;
            }
        }

        if (inputPath == null) {
            help.help();
            return;
        }

        Logger.setEnabled(logEnabled);

        File inputApk = new File(inputPath);
        if (!inputApk.exists()) {
            System.err.println("Input file not found: " + inputPath);
            return;
        }

        banner.banner();

        File tempDir = null;

        try {
            ApkModule module;
            File mergedApkFile;

            // Init logger after we know the package (done after loading module)
            if (inputPath.endsWith(".apk")) {
                Logger.log("[INFO] Loading APK...");
                module = ApkModule.loadApkFile(inputApk);
                mergedApkFile = inputApk;
            } else {
                tempDir = Files.createTempDirectory("antik_merge").toFile();
                Logger.log("[MERGE] Extracting APKS...");
                AntikUtils.ex_apks(inputApk, tempDir);

                Logger.log("[MERGE] Merging APK...");
                ApkBundle bundle = new ApkBundle();
                bundle.loadApkDirectory(tempDir);
                module = bundle.mergeModules();

                Logger.log("[INFO] Patching AndroidManifest.xml");
                try {
                    manifestP.patch(module);
                } catch (Exception e) {
                    Logger.warn("Manifest patching failed: " + e.getMessage());
                }

                String name = inputApk.getName();
                int dot = name.lastIndexOf('.');
                name = (dot > 0 ? name.substring(0, dot) : name) + "_merged.apk";
                mergedApkFile = new File(output.get_out(inputApk, name));

                Logger.log("[BUILD] Writing merged APK...");
                loading.progress(module, mergedApkFile);
                Logger.log("[MERGE] APK merged successfully: " + mergedApkFile.getAbsolutePath());
            }

            // Init LogFox file logger now that we have the package name
            String pkg = "unknown";
            try { pkg = module.getPackageName(); } catch (Exception ignored) {}
            Logger.init(pkg);

            if (translatePath != null) {
                File jsonFile = new File(translatePath);
                if (jsonFile.exists()) {
                    Logger.log("[INFO] Starting Translation Patch...");
                    TranslationPatcher.patch(module, jsonFile);

                    String tn = mergedApkFile.getName();
                    int td = tn.lastIndexOf('.');
                    tn = (td > 0 ? tn.substring(0, td) : tn) + "_translated.apk";
                    File transFile = new File(output.get_out(inputApk, tn));

                    Logger.log("[BUILD] Building Translated APK...");
                    output.write(module, transFile);
                    Logger.log("[BUILD] Translated APK: " + transFile.getAbsolutePath());
                } else {
                    Logger.error("Translation file not found: " + translatePath);
                }
            } else if (!inputPath.endsWith(".apk")) {
                Logger.log("[INFO] Patching classes.dex for logging...");
                try {
                    DexPatcher.patch(module);
                } catch (Exception e) {
                    Logger.error("Patching failed: " + e.getMessage());
                    e.printStackTrace();
                }

                String pn = mergedApkFile.getName();
                int pd = pn.lastIndexOf('.');
                pn = (pd > 0 ? pn.substring(0, pd) : pn) + "_pairip.apk";
                File paiFile = new File(output.get_out(inputApk, pn));

                Logger.log("[BUILD] Building Logging APK...");
                output.write(module, paiFile);
                crc32.patch(mergedApkFile, paiFile);
                Logger.log("[BUILD] Logging APK: " + paiFile.getAbsolutePath());
            }

            Logger.log("[BUILD] ✅ Process completed successfully");

        } catch (Exception e) {
            Logger.error("Process failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (tempDir != null) deleteDir.del_dir(tempDir);
            Logger.close();
        }
    }
}
