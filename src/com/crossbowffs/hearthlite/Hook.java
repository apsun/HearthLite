package com.crossbowffs.hearthlite;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.io.IOException;

public class Hook implements IXposedHookLoadPackage {
    private static final String TAG = "HearthLite";

    private static String getMainObbPath(Object loadingScreen) {
        return (String)XposedHelpers.getObjectField(loadingScreen, "m_mainObbPath");
    }

    private static String getPatchObbPath(Object loadingScreen) {
        return (String)XposedHelpers.getObjectField(loadingScreen, "m_patchObbPath");
    }

    private static String getObbTokenPath(String obbPath) {
        return obbPath + ".extracted";
    }

    private static boolean isObbExtracted(Object loadingScreen) {
        String mainObbPath = getMainObbPath(loadingScreen);
        String patchObbPath = getPatchObbPath(loadingScreen);
        boolean mainExtracted = new File(getObbTokenPath(mainObbPath)).exists();
        boolean patchExtracted = new File(getObbTokenPath(patchObbPath)).exists();
        return mainExtracted && patchExtracted;
    }

    private static Thread startDummyThread() {
        Thread dummyThread = new Thread();
        dummyThread.start();
        return dummyThread;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.blizzard.wtcg.hearthstone".equals(lpparam.packageName)) {
            return;
        }

        Class<?> cls = XposedHelpers.findClass("com.blizzard.wtcg.hearthstone.LoadingScreen", lpparam.classLoader);

        // Check for existence of download tokens; if they exist,
        // force skip the download process
        Log.i(TAG, "Hooking DownloadObbFromGoogle...");
        XposedHelpers.findAndHookMethod(cls, "DownloadObbFromGoogle", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (isObbExtracted(param.thisObject)) {
                    Log.i(TAG, "OBB tokens exist, skipping OBB download process");
                    XposedHelpers.setObjectField(param.thisObject, "m_downloadObbThread", startDummyThread());
                    param.setResult(null);
                }
            }
        });

        // If OBB files have already been extracted (and deleted),
        // don't try to extract them again
        Log.i(TAG, "Hooking ShouldReExtractArchives...");
        XposedHelpers.findAndHookMethod(cls, "ShouldReExtractArchives", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (isObbExtracted(param.thisObject)) {
                    Log.i(TAG, "OBB tokens exist, overriding ShouldReExtractArchives -> false");
                    param.setResult(false);
                }
            }
        });

        // Delete OBB files after they've been extracted, and
        // create a token so we can know if we should skip this
        // process next time.
        Log.i(TAG, "Hooking ExtractAssetsFromObb...");
        XposedHelpers.findAndHookMethod(cls, "ExtractAssetsFromObb", String.class, Thread.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Object self = param.thisObject;
                final String obbPath = (String)param.args[0];
                final Thread thread = (Thread)param.getResult();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Waiting for extraction thread to finish...");
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Thread join interrupted", e);
                            return;
                        }

                        if (isObbExtracted(self)) {
                            Log.i(TAG, "OBB download tokens already exist");
                            return;
                        }

                        String tokenPath = getObbTokenPath(obbPath);
                        Log.i(TAG, "Creating OBB download token: " + tokenPath);
                        try {
                            new File(tokenPath).createNewFile();
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to create OBB download token", e);
                            return;
                        }

                        Log.i(TAG, "Deleting OBB file: " + obbPath);
                        if (!new File(obbPath).delete()) {
                            Log.e(TAG, "Could not delete OBB file");
                        }
                    }
                }).start();
            }
        });

        Log.i(TAG, "HearthLite initialization successful!");
    }
}
