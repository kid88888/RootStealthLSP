package com.kaide.rootstealth;

import android.util.Log;

import java.io.File;
import java.lang.reflect.Member;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class RootStealth implements IXposedHookLoadPackage {

    private static final String TAG = "RootStealthLSP";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        // 只针对银行类 App，可以修改为具体包名列表
        if (!lpparam.packageName.contains("bank") && !lpparam.packageName.contains("finance") && !lpparam.packageName.equals("com.ifast.gb")) {
            return;
        }

        Log.i(TAG, "Hooking package: " + lpparam.packageName);

        try {
            XposedHelpers.findAndHookMethod(
                File.class,
                "exists",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String path = ((File) param.thisObject).getAbsolutePath();
                        if (shouldBlockPath(path)) {
                            Log.d(TAG, "Blocked exists() check for: " + path);
                            param.setResult(false);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Failed to hook File.exists(): " + t);
        }

        // 可扩展更多检测（如 Runtime.exec、getRuntime().exec() 等）
    }

    private boolean shouldBlockPath(String path) {
        String[] suspiciousPaths = new String[] {
            "/system/xbin/su", "/system/bin/su",
            "/system/priv-app/AndroidAuto",
            "/system/app/Superuser", "/sbin/su",
            "/data/adb/modules/aa4mg", // 模块本体
            "/system/bin/magisk", "/system/bin/.magisk",
            "/system/bin/busybox", "/system/xbin/busybox"
        };

        for (String p : suspiciousPaths) {
            if (path.equals(p)) return true;
        }
        return false;
    }
}
