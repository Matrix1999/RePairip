package com.Matrix.DexPatcher;

import static com.Matrix.DexPatcher.MethodT.patchLauncher.patchStartupLauncher;

import com.Matrix.DexPatcher.MethodT.patchM;
import com.Matrix.Logger;
import com.Matrix.Main;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class DexPatcher {

    // ─── Universal check-class matcher ───────────────────────────────────────
    // Matches ALL Pairip license / signature / auth check classes across every
    // SDK version (licensecheck, licensecheck2, licensecheck3, licensecheck4 …)
    private static boolean isPairipCheckClass(String type) {
        if (type == null) return false;
        if (!type.startsWith("Lcom/pairip/")) return false;
        // Never touch the core runtime classes — we handle those separately
        if ("Lcom/pairip/VMRunner;".equals(type))                    return false;
        if ("Lcom/pairip/StartupLauncher;".equals(type))             return false;
        if (type.startsWith("Lcom/pairip/application/"))             return false;
        if ("Lcom/pairip/PairipLog;".equals(type))                   return false;
        if ("Lcom/pairip/RestoreMethod;".equals(type))               return false;
        // Everything else under com.pairip is a check/license/verify class
        return true;
    }

    // ─── Strip ALL Pairip native .so libraries ────────────────────────────────
    private static void stripNativeLibs(ApkModule m) {
        List<String> toRemove = new ArrayList<String>();
        for (InputSource s : m.getInputSources()) {
            String n = s.getName();
            if (n.startsWith("lib/") && n.endsWith(".so") && (
                    n.contains("libpairip") ||
                    n.contains("libPairip") ||
                    n.contains("libvmrunner") ||
                    n.contains("libVmRunner") ||
                    n.contains("libdexjit")
            )) {
                toRemove.add(n);
            }
        }
        for (String name : toRemove) {
            try {
                m.getZipEntryMap().remove(name);
                Logger.log("[DexPatcher] 🗑️  Removed native lib: " + name);
            } catch (Exception e) {
                Logger.warn("[DexPatcher] Could not remove " + name + ": " + e.getMessage());
            }
        }
        if (toRemove.isEmpty()) {
            Logger.log("[DexPatcher] No Pairip native libs found");
        }
    }

    // ─── Stub VMRunner.invoke() → return null ─────────────────────────────────
    // Prevents any late-executed protected code from running at all
    private static ClassDef stubVMRunner(ClassDef cd) {
        List<Method> methods = new ArrayList<Method>();
        for (Method m : cd.getDirectMethods()) methods.add(m);
        for (Method m : cd.getVirtualMethods()) {
            if ("invoke".equals(m.getName())) {
                Logger.log("[DexPatcher] ✅ Stubbing VMRunner.invoke() → null");
                List<org.jf.dexlib2.iface.instruction.Instruction> ins = Arrays.asList(
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                        new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)
                );
                methods.add(new ImmutableMethod(m.getDefiningClass(), m.getName(),
                        m.getParameters(), m.getReturnType(), m.getAccessFlags(),
                        m.getAnnotations(), m.getHiddenApiRestrictions(),
                        new ImmutableMethodImplementation(2, ins, null, null)));
            } else {
                methods.add(m);
            }
        }
        return new ImmutableClassDef(cd.getType(), cd.getAccessFlags(),
                cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(),
                cd.getAnnotations(), cd.getStaticFields(), cd.getInstanceFields(),
                new ArrayList<Method>(), methods);
    }

    // ─── Clean Pairip Application class → just call super() ──────────────────
    // Prevents Pairip from hooking into app startup via Application.<init>
    private static ClassDef cleanApplication(ClassDef cd) {
        List<Method> methods = new ArrayList<Method>();
        for (Method m : cd.getDirectMethods()) {
            if ("<init>".equals(m.getName())) {
                Logger.log("[DexPatcher] ✅ Cleaning Application.<init>() → super only");
                String superclass = cd.getSuperclass() != null ? cd.getSuperclass() : "Ljava/lang/Object;";
                List<org.jf.dexlib2.iface.instruction.Instruction> ins = Arrays.asList(
                        new org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c(
                                Opcode.INVOKE_DIRECT, 1, 0, 0, 0, 0, 0,
                                new org.jf.dexlib2.immutable.reference.ImmutableMethodReference(
                                        superclass, "<init>", null, "V")),
                        new ImmutableInstruction10x(Opcode.RETURN_VOID)
                );
                methods.add(new ImmutableMethod(m.getDefiningClass(), "<init>",
                        m.getParameters(), "V", m.getAccessFlags(),
                        m.getAnnotations(), m.getHiddenApiRestrictions(),
                        new ImmutableMethodImplementation(1, ins, null, null)));
            } else {
                methods.add(m);
            }
        }
        for (Method m : cd.getVirtualMethods()) methods.add(m);
        return new ImmutableClassDef(cd.getType(), cd.getAccessFlags(),
                cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(),
                cd.getAnnotations(), cd.getStaticFields(), cd.getInstanceFields(),
                methods, new ArrayList<Method>());
    }

    // ─── Main patch entry point ───────────────────────────────────────────────
    public static void patch(ApkModule m) throws Exception {

        // 1. Remove Pairip native libraries
        stripNativeLibs(m);

        List<String> dx_ns = new ArrayList<String>();
        for (InputSource s : m.getInputSources()) {
            if (s.getName().endsWith(".dex")) dx_ns.add(s.getName());
        }

        List<ClassDef> l_cds = new ArrayList<ClassDef>();
        Set<String> a_ts    = new HashSet<String>();
        String pkg = "unknown";
        try { pkg = m.getPackageName(); } catch (Exception ignored) {}

        Logger.log("[DexPatcher] Target package  : " + pkg);
        Logger.log("[DexPatcher] Dex files found : " + dx_ns);

        // 2. Load the log.dex (PairipLog) and rewrite its DIR_PATH to sdcard
        try (InputStream i = Main.class.getResourceAsStream("/log.dex")) {
            if (i != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192]; int len;
                while ((len = i.read(buf)) != -1) baos.write(buf, 0, len);
                byte[] l_bs = baos.toByteArray();
                File t_l_dx = File.createTempFile("log", ".dex");
                Files.write(t_l_dx.toPath(), l_bs);
                DexFile l_df = DexFileFactory.loadDexFile(t_l_dx, Opcodes.getDefault());
                t_l_dx.delete();

                for (ClassDef c : l_df.getClasses()) {
                    if ("Lcom/pairip/PairipLog;".equals(c.getType())) {
                        List<org.jf.dexlib2.iface.Field> s_fs = new ArrayList<org.jf.dexlib2.iface.Field>();
                        for (org.jf.dexlib2.iface.Field f : c.getStaticFields()) {
                            if ("DIR_PATH".equals(f.getName())) {
                                String n_v = "/sdcard/RePairip/" + pkg;
                                Logger.log("[DexPatcher] Dump path → " + n_v);
                                s_fs.add(new org.jf.dexlib2.immutable.ImmutableField(
                                        f.getDefiningClass(), f.getName(), f.getType(),
                                        f.getAccessFlags(),
                                        new org.jf.dexlib2.immutable.value.ImmutableStringEncodedValue(n_v),
                                        f.getAnnotations(), f.getHiddenApiRestrictions()));
                            } else {
                                s_fs.add(f);
                            }
                        }
                        l_cds.add(new ImmutableClassDef(c.getType(), c.getAccessFlags(),
                                c.getSuperclass(), c.getInterfaces(), c.getSourceFile(),
                                c.getAnnotations(), s_fs, c.getInstanceFields(),
                                c.getDirectMethods(), c.getVirtualMethods()));
                    } else {
                        l_cds.add(c);
                    }
                    a_ts.add(c.getType());
                }
                Logger.log("[DexPatcher] log.dex injected for: " + pkg);
            } else {
                Logger.warn("[DexPatcher] log.dex resource not found — dump disabled");
            }
        }

        // 3. Detect obfuscated junk classes (string/method containers Pairip uses)
        List<String> j_ts = new ArrayList<String>();
        for (String dn : dx_ns) {
            InputSource s = m.getInputSource(dn);
            if (s == null) continue;
            byte[] d_bs;
            try (InputStream i = s.openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192]; int len;
                while ((len = i.read(buf)) != -1) baos.write(buf, 0, len);
                d_bs = baos.toByteArray();
            }
            File t_dx = File.createTempFile("temp", ".dex");
            Files.write(t_dx.toPath(), d_bs);
            DexFile d_f = DexFileFactory.loadDexFile(t_dx, Opcodes.getDefault());
            t_dx.delete();

            for (ClassDef cd : d_f.getClasses()) {
                if (cd.getMethods().iterator().hasNext()) continue;
                if (!cd.getFields().iterator().hasNext()) continue;
                if (!"Ljava/lang/Object;".equals(cd.getSuperclass())) continue;
                if (cd.getAccessFlags() != 1) continue;
                boolean ok = true;
                for (org.jf.dexlib2.iface.Field f : cd.getFields()) {
                    if (f.getAccessFlags() != 9) { ok = false; break; }
                    String t = f.getType();
                    if (!t.equals("Ljava/lang/String;") && !t.equals("Ljava/lang/reflect/Method;")) {
                        ok = false; break;
                    }
                    if (f.getInitialValue() != null) { ok = false; break; }
                }
                if (ok) j_ts.add(cd.getType());
            }
        }
        Logger.log("[DexPatcher] Junk classes   : " + j_ts.size());

        // 4. Main patch pass over every dex
        boolean foundLauncher   = false;
        boolean foundVMRunner   = false;
        boolean foundApplication = false;
        int checkClassesPatched = 0;

        for (String dn : dx_ns) {
            InputSource s = m.getInputSource(dn);
            if (s == null) continue;
            byte[] d_bs;
            try (InputStream i = s.openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192]; int len;
                while ((len = i.read(buf)) != -1) baos.write(buf, 0, len);
                d_bs = baos.toByteArray();
            }
            File t_dx = File.createTempFile("temp", ".dex");
            Files.write(t_dx.toPath(), d_bs);
            DexFile d_f = DexFileFactory.loadDexFile(t_dx, Opcodes.getDefault());
            t_dx.delete();

            List<ClassDef> cds = new ArrayList<ClassDef>();
            boolean mod = false;

            for (ClassDef cd : d_f.getClasses()) {
                if (a_ts.contains(cd.getType())) continue;

                if ("Lcom/pairip/StartupLauncher;".equals(cd.getType())) {
                    mod = true; foundLauncher = true;
                    Logger.log("[DexPatcher] ✅ Patching StartupLauncher in " + dn);
                    cds.add(patchStartupLauncher(cd, j_ts));

                } else if ("Lcom/pairip/VMRunner;".equals(cd.getType())) {
                    mod = true; foundVMRunner = true;
                    cds.add(stubVMRunner(cd));

                } else if (cd.getType().startsWith("Lcom/pairip/application/")) {
                    mod = true; foundApplication = true;
                    cds.add(cleanApplication(cd));

                } else if (isPairipCheckClass(cd.getType())) {
                    mod = true; checkClassesPatched++;
                    Logger.log("[DexPatcher] ✅ Bypassing check class: " + cd.getType() + " in " + dn);
                    List<Method> d_ms = new ArrayList<Method>();
                    for (Method method : cd.getDirectMethods())
                        d_ms.add(patchM.patchMethodIfTarget(method));
                    List<Method> v_ms = new ArrayList<Method>();
                    for (Method method : cd.getVirtualMethods())
                        v_ms.add(patchM.patchMethodIfTarget(method));
                    cds.add(new ImmutableClassDef(cd.getType(), cd.getAccessFlags(),
                            cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(),
                            cd.getAnnotations(), cd.getStaticFields(), cd.getInstanceFields(),
                            d_ms, v_ms));

                } else {
                    cds.add(cd);
                }
            }

            if ("classes.dex".equals(dn) && !l_cds.isEmpty()) {
                cds.addAll(l_cds);
                mod = true;
            }

            if (mod) {
                MemoryDataStore ds = new MemoryDataStore();
                DexPool dp = new DexPool(Opcodes.getDefault());
                for (ClassDef c : cds) dp.internClass(c);
                dp.writeTo(ds);
                byte[] r_bs = Arrays.copyOf(ds.getData(), ds.getSize());
                m.add(new ByteInputSource(r_bs, dn));
                Logger.log("[DexPatcher] ✅ Wrote patched " + dn);
            }
        }

        // 5. Summary
        if (!foundLauncher)    Logger.warn("[DexPatcher] ⚠️  StartupLauncher not found");
        if (!foundVMRunner)    Logger.warn("[DexPatcher] ⚠️  VMRunner not found");
        if (!foundApplication) Logger.warn("[DexPatcher] ℹ️  No Pairip Application class found (may be normal)");
        Logger.log("[DexPatcher] ─── Summary ───────────────────────────");
        Logger.log("[DexPatcher] StartupLauncher : " + foundLauncher);
        Logger.log("[DexPatcher] VMRunner stubbed: " + foundVMRunner);
        Logger.log("[DexPatcher] App cleaned     : " + foundApplication);
        Logger.log("[DexPatcher] Check classes   : " + checkClassesPatched);
    }
}
