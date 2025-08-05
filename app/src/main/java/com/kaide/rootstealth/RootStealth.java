package com.kaide.rootstealth;

import java.io.File;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class RootStealth implements IXposedHookLoadPackage {
    private static final String TARGET_PATH = "/system/product/etc/permissions/com.google.android.projection.gearhead.xml";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.ifast.gb")) return;

        // Hook java.io.File.exists
        XposedHelpers.findAndHookMethod(
            File.class,
            "exists",
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    File file = (File) param.thisObject;
                    if (file.getAbsolutePath().equals(TARGET_PATH)) {
                        return false;
                    }
                    return XposedHelpers.callMethod(param.thisObject, "exists");
                }
            }
        );
    }
}
