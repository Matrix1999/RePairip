package com.Matrix.DexPatcher.MethodT;

import com.Matrix.Logger;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class patchM {

    // Methods that return true / 1 (boolean license/verify checks)
    private static final List<String> BOOL_BYPASS = Arrays.asList(
            "verifySignatureMatches",
            "validateSignature",
            "checkSignature",
            "isValid",
            "isLicensed",
            "isVerified",
            "isAuthenticated",
            "isAllowed",
            "isPurchased",
            "verify",
            "check"
    );

    // Methods that return void / null (license callbacks, activity lifecycle hooks)
    private static final List<String> VOID_BYPASS = Arrays.asList(
            "verifyIntegrity",
            "checkLicense",
            "doCheck",
            "onCheckComplete",
            "onLicenseChecked",
            "startCheck",
            "runCheck",
            "checkIntegrity",
            "validateIntegrity",
            "onActivityCreate",    // Pairip re-checks license on Activity.onCreate
            "onActivityResume",
            "onActivityStart"
    );

    public static Method patchMethodIfTarget(Method m) {
        String n = m.getName();
        String rt = m.getReturnType();

        if (BOOL_BYPASS.contains(n)) {
            Logger.log("[patchM] ✅ Bypassing (→ true): " + m.getDefiningClass() + "->" + n);
            List<Instruction> ins = Arrays.asList(
                    new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                    new ImmutableInstruction11x(Opcode.RETURN, 0)
            );
            return new ImmutableMethod(m.getDefiningClass(), n, m.getParameters(), rt,
                    m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(1, ins, null, null));
        }

        if (VOID_BYPASS.contains(n)) {
            Logger.log("[patchM] ✅ Bypassing (→ void/null): " + m.getDefiningClass() + "->" + n);
            List<Instruction> ins;
            if ("V".equals(rt)) {
                ins = Collections.singletonList(new ImmutableInstruction10x(Opcode.RETURN_VOID));
            } else if (rt.startsWith("L") || rt.startsWith("[")) {
                ins = Arrays.asList(
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                        new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)
                );
            } else {
                ins = Arrays.asList(
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                        new ImmutableInstruction11x(Opcode.RETURN, 0)
                );
            }
            return new ImmutableMethod(m.getDefiningClass(), n, m.getParameters(), rt,
                    m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(1, ins, null, null));
        }

        return m;
    }
}
