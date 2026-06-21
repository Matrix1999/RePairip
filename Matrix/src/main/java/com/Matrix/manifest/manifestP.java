package com.Matrix.manifest;

import com.reandroid.apk.ApkModule;
import com.reandroid.app.AndroidManifest;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class manifestP {

    public static void patch(ApkModule m) {
        AndroidManifestBlock mf = m.getAndroidManifest();
        if (mf == null) return;

        // Fix split APK flags so merged APK installs cleanly
        Boolean extractNativeLibs = mf.isExtractNativeLibs();
        if (extractNativeLibs != null && !extractNativeLibs) {
            mf.setExtractNativeLibs(true);
        }

        ResXmlElement manifestEl = mf.getManifestElement();
        if (manifestEl != null) {
            manifestEl.removeAttributesWithId(AndroidManifest.ID_requiredSplitTypes);
            manifestEl.removeAttributesWithId(AndroidManifest.ID_splitTypes);
            manifestEl.removeAttributesWithId(AndroidManifest.ID_isSplitRequired);
            manifestEl.removeAttributesWithId(AndroidManifest.ID_isFeatureSplit);
            manifestEl.removeAttributesWithName("requiredSplitTypes");
            manifestEl.removeAttributesWithName("splitTypes");
            manifestEl.removeAttributesWithName("isSplitRequired");
            manifestEl.removeAttributesWithName("isFeatureSplit");
            manifestEl.removeAttributesWithName("split");
        }

        // Remove Play Store stamp meta-data
        RElementsByName(mf, "meta-data", Arrays.asList(
                "com.android.stamp.source",
                "com.android.stamp.type",
                "com.android.vending.splits",
                "com.android.vending.derived.apk.id",
                "com.android.dynamic.apk.fused.modules",
                "com.android.vending.splits.required"
        ));

        // Remove ALL com.pairip.* components universally across all SDK versions
        // (licensecheck, licensecheck2, licensecheck3, licensecheck4 …)
        removePairipComponents(mf, "activity");
        removePairipComponents(mf, "provider");
        removePairipComponents(mf, "service");
        removePairipComponents(mf, "receiver");

        // Remove CHECK_LICENSE permission
        RElementsByName(mf, "uses-permission", Collections.singletonList(
                "com.android.vending.CHECK_LICENSE"
        ));

        m.setManifest(mf);
    }

    // Removes every manifest component whose android:name starts with com.pairip.
    private static void removePairipComponents(AndroidManifestBlock mf, String tag) {
        Iterator<ResXmlElement> it = mf.getApplicationElementsByTag(tag);
        List<ResXmlElement> toRemove = new ArrayList<>();
        while (it.hasNext()) {
            ResXmlElement el = it.next();
            String name = AndroidManifestBlock.getAndroidNameValue(el);
            if (name != null && name.startsWith("com.pairip.")) {
                toRemove.add(el);
            }
        }
        for (ResXmlElement el : toRemove) el.removeSelf();
        if (!toRemove.isEmpty()) {
            System.out.println("[Manifest] Removed " + toRemove.size() + " pairip <" + tag + "> entries");
        }
    }

    private static void RElementsByName(AndroidManifestBlock mf, String tag, List<String> names) {
        Iterator<ResXmlElement> it = "uses-permission".equals(tag)
                ? mf.getManifestElement().getElements(tag)
                : mf.getApplicationElementsByTag(tag);
        List<ResXmlElement> toRemove = new ArrayList<>();
        while (it.hasNext()) {
            ResXmlElement el = it.next();
            String name = AndroidManifestBlock.getAndroidNameValue(el);
            if (names.contains(name)) toRemove.add(el);
        }
        for (ResXmlElement el : toRemove) el.removeSelf();
    }
}
