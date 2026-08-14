package com.sdcard.createhook;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed 模块主入口
 * 精确记录是哪个 App 在 sdcard 根目录创建了文件/文件夹
 */
public class HookMain implements IXposedHookLoadPackage {

    private static final String TAG = "SdcardCreateHook";
    private static final String LOG_PATH = "/storage/emulated/0/Download/sdcard_create_hook.log";
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    // 只关心根目录（可根据需要扩展）
    private static final String[] WATCH_PREFIXES = {
            "/storage/emulated/0/",
            "/sdcard/",
            "/storage/self/primary/"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 记录模块被加载
        XposedBridge.log(TAG + " loaded in: " + lpparam.packageName);

        // 1. Hook java.io.File 的创建方法（最常用）
        hookFileMethods(lpparam);

        // 2. Hook MediaStore / ContentResolver 相关插入（很多 App 走这条路径）
        hookMediaStore(lpparam);

        // 3. 可选：Hook 一些系统工具类
        if ("android".equals(lpparam.packageName) || lpparam.packageName.contains("media")) {
            hookSystemHelpers(lpparam);
        }
    }

    private void hookFileMethods(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // File.mkdir()
            XposedHelpers.findAndHookMethod(File.class, "mkdir", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    File f = (File) param.thisObject;
                    if (Boolean.TRUE.equals(param.getResult()) && isRootLevel(f)) {
                        logCreate(lpparam.packageName, "目录", f.getAbsolutePath(), "File.mkdir");
                    }
                }
            });

            // File.mkdirs()
            XposedHelpers.findAndHookMethod(File.class, "mkdirs", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    File f = (File) param.thisObject;
                    if (Boolean.TRUE.equals(param.getResult()) && isRootLevel(f)) {
                        logCreate(lpparam.packageName, "目录", f.getAbsolutePath(), "File.mkdirs");
                    }
                }
            });

            // File.createNewFile()
            XposedHelpers.findAndHookMethod(File.class, "createNewFile", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    File f = (File) param.thisObject;
                    if (Boolean.TRUE.equals(param.getResult()) && isRootLevel(f)) {
                        logCreate(lpparam.packageName, "文件", f.getAbsolutePath(), "File.createNewFile");
                    }
                }
            });

            XposedBridge.log(TAG + " File hooks installed for " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " File hook failed: " + t.getMessage());
        }
    }

    private void hookMediaStore(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // ContentResolver.insert — 很多媒体/文件创建走这里
            Class<?> crClass = XposedHelpers.findClass("android.content.ContentResolver", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(crClass, "insert",
                    android.net.Uri.class, android.content.ContentValues.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.getResult() == null) return;
                            android.net.Uri uri = (android.net.Uri) param.args[0];
                            if (uri == null) return;
                            String uriStr = uri.toString();
                            // 只关心 external / files 相关
                            if (uriStr.contains("external") || uriStr.contains("media") || uriStr.contains("downloads")) {
                                android.content.ContentValues values = (android.content.ContentValues) param.args[1];
                                String path = null;
                                if (values != null) {
                                    path = values.getAsString("_data");
                                    if (path == null) path = values.getAsString("relative_path");
                                    if (path == null) path = values.getAsString(android.provider.MediaStore.MediaColumns.DISPLAY_NAME);
                                }
                                if (path != null && (path.startsWith("/storage") || path.startsWith("/sdcard") || !path.contains("/"))) {
                                    logCreate(lpparam.packageName, "媒体/文件", path, "ContentResolver.insert → " + uriStr);
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            // 部分进程可能没有这些类，忽略
        }
    }

    private void hookSystemHelpers(XC_LoadPackage.LoadPackageParam lpparam) {
        // 预留扩展点
    }

    /** 判断是否为 sdcard 根目录下的直接创建（一级） */
    private boolean isRootLevel(File f) {
        if (f == null) return false;
        String path = f.getAbsolutePath();
        if (path == null) return false;

        // 统一路径
        path = path.replace("/storage/self/primary", "/storage/emulated/0")
                   .replace("/sdcard", "/storage/emulated/0");

        // 只要是 /storage/emulated/0/xxx （没有更多斜杠）就算根目录创建
        if (path.startsWith("/storage/emulated/0/")) {
            String relative = path.substring("/storage/emulated/0/".length());
            // 允许一级目录或文件（不包含 /）
            return !relative.isEmpty() && !relative.contains("/");
        }
        return false;
    }

    private void logCreate(String packageName, String type, String path, String method) {
        logExecutor.execute(() -> {
            try {
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
                String appName = getAppName(packageName);

                StringBuilder sb = new StringBuilder();
                sb.append("[创建] ").append(time).append("\n");
                sb.append("应用：").append(appName).append("\n");
                sb.append("包名：").append(packageName).append("\n");
                sb.append("类型：").append(type).append("\n");
                sb.append("路径：").append(path).append("\n");
                sb.append("方法：").append(method).append("\n");
                sb.append("\n");

                // 写入文件
                File logFile = new File(LOG_PATH);
                File parent = logFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                try (FileWriter fw = new FileWriter(logFile, true);
                     BufferedWriter bw = new BufferedWriter(fw);
                     PrintWriter out = new PrintWriter(bw)) {
                    out.print(sb.toString());
                }

                // 同时打到 Xposed 日志方便调试
                XposedBridge.log(TAG + " " + packageName + " 创建了 " + type + " → " + path);
            } catch (Throwable t) {
                XposedBridge.log(TAG + " 写日志失败: " + t.getMessage());
            }
        });
    }

    private String getAppName(String packageName) {
        // 简单映射，可自行扩展
        switch (packageName) {
            case "com.tencent.mm": return "微信";
            case "com.tencent.mobileqq": return "QQ";
            case "com.eg.android.AlipayGphone": return "支付宝";
            case "com.ss.android.ugc.aweme": return "抖音";
            case "com.smile.gifmaker": return "快手";
            case "com.xingin.xhs": return "小红书";
            case "tv.danmaku.bili": return "哔哩哔哩";
            case "com.miui.gallery": return "小米相册";
            case "com.android.fileexplorer": return "文件管理";
            case "com.android.providers.media.module":
            case "com.android.providers.media": return "媒体存储";
            case "android": return "系统";
            default: return packageName;
        }
    }
}
