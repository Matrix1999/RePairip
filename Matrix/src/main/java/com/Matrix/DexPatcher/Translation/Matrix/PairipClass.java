package com.Matrix.DexPatcher.Translation.Matrix;

public enum PairipClass {
    VM_RUNNER("Lcom/pairip/VMRunner;"),
    APPLICATION("Lcom/pairip/application/Application;"),
    STARTUP_LAUNCHER("Lcom/pairip/StartupLauncher;"),
    LICENSE_CLIENT_V1("Lcom/pairip/licensecheck/LicenseClient;"),
    LICENSE_CLIENT_V2("Lcom/pairip/licensecheck2/LicenseClientV2;"),
    LICENSE_CLIENT_V3("Lcom/pairip/licensecheck3/LicenseClientV3;"),
    LICENSE_CLIENT_V4("Lcom/pairip/licensecheck4/LicenseClientV4;");

    public final String type;

    PairipClass(String type) {
        this.type = type;
    }

    public static boolean contains(String type) {
        for (PairipClass pc : values()) {
            if (pc.type.equals(type)) return true;
        }
        return false;
    }
}
