package com.kaide.rootstealth;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class RootStealth implements IXposedHookLoadPackage {

    private static final String TAG = "RootStealthLSP";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        // 只拦截银行类 App
        if (!lpparam.packageName.contains("bank") &&
            !lpparam.packageName.contains("finance") &&
            !lpparam.packageName.equals("com.ifast.gb")) {
            return;
        }

        Log.i(TAG, "Hooking package: " + lpparam.packageName);

        try {
            hookFileExists();
            hookRuntimeExec();
            hookProcessBuilder();
            hookSystemGetProperty();
            hookBuildTags();
            hookClassForName();
        } catch (Throwable t) {
            XposedBridge.log("RootStealth Hook Error: " + Log.getStackTraceString(t));
        }
    }

    private void hookFileExists() {
        XposedHelpers.findAndHookMethod(
            File.class,
            "exists",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String path = ((File) param.thisObject).getAbsolutePath();
                    if (shouldBlockPath(path)) {
                        Log.d(TAG, "Blocked exists() check for: " + path);
                        param.setResult(false);
                    }
                }
            }
        );
    }

    private void hookRuntimeExec() {
        XposedHelpers.findAndHookMethod(
            Runtime.class, "exec",
            String[].class, String[].class, File.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String[] cmds = (String[]) param.args[0];
                    for (String cmd : cmds) {
                        if (containsRootKeyword(cmd)) {
                            Log.d(TAG, "Blocked Runtime.exec command: " + Arrays.toString(cmds));
                            param.setThrowable(new IOException("Blocked by RootStealth"));
                            return;
                        }
                    }
                }
            }
        );
    }

    private void hookProcessBuilder() {
        XposedHelpers.findAndHookConstructor(
            ProcessBuilder.class,
            List.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    List<?> cmdList = (List<?>) param.args[0];
                    for (Object c : cmdList) {
                        if (c.toString().toLowerCase().contains("su")) {
                            Log.d(TAG, "Blocked ProcessBuilder command: " + cmdList);
                            param.setThrowable(new IOException("Blocked by RootStealth"));
                        }
                    }
                }
            }
        );
    }

    private void hookSystemGetProperty() {
        XposedHelpers.findAndHookMethod(
            System.class,
            "getProperty",
            String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if ("ro.build.tags".equals(key)) {
                        Log.d(TAG, "Blocked getProperty for: " + key);
                        param.setResult("release-keys");
                    }
                }
            }
        );
    }

    private void hookBuildTags() {
        try {
            XposedHelpers.setStaticObjectField(Build.class, "TAGS", "release-keys");
        } catch (Throwable t) {
            Log.w(TAG, "Failed to set Build.TAGS: " + t);
        }
    }

    private void hookClassForName() {
        XposedHelpers.findAndHookMethod(
            Class.class,
            "forName",
            String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String className = (String) param.args[0];
                    if (className.toLowerCase().contains("magisk") || className.toLowerCase().contains("zygisk")) {
                        Log.d(TAG, "Blocked reflection on: " + className);
                        throw new ClassNotFoundException("Blocked by RootStealth");
                    }
                }
            }
        );
    }

    private boolean shouldBlockPath(String path) {
        String[] suspiciousPaths = new String[] {
            "/system/xbin/su", "/system/bin/su",
            "/system/priv-app/AndroidAuto",
            "/system/app/Superuser", "/sbin/su",
            "/data/adb/modules/aa4mg",
            "/system/bin/magisk", "/system/bin/.magisk",
            "/system/bin/busybox", "/system/xbin/busybox"
        };
        for (String p : suspiciousPaths) {
            if (path.equals(p)) return true;
        }
        return false;
    }

    private boolean containsRootKeyword(String command) {
        String[] keywords = new String[] { "su", "magisk", "busybox", "sh", "getprop" };
        for (String keyword : keywords) {
            if (command.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
