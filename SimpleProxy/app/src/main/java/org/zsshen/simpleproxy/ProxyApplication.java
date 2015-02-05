package org.zsshen.simpleproxy;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.ArrayMap;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import dalvik.system.DexClassLoader;

public class ProxyApplication extends Application {
    static private String NAME_ORIGINAL_APK = "Original.apk";
    static private String NAME_DIR_OPTMIZE_DEX = "DynamicOptDex";
    static private String NAME_DIR_LIB = "DynamicLib";
    static private String LOGD_TAG_ERROR = "Error(SimpleProxy)";
    static private int SIZE_BUF = 1024;

    protected void attachBaseContext(Context ctxBase)
    {
        boolean bRtnCode = true;

        /* Prepare the private storage for the dynamically loaded APK. */
        File fileOptDex = ctxBase.getDir(NAME_DIR_OPTMIZE_DEX, MODE_PRIVATE);
        File fileLib = ctxBase.getDir(NAME_DIR_LIB, MODE_PRIVATE);
        String szOptDex = fileOptDex.getAbsolutePath();
        String szLib = fileLib.getAbsolutePath();
        File fileApk = new File(szOptDex, NAME_ORIGINAL_APK);
        String szOrigApk = fileApk.getAbsolutePath();

        /* Copy the original APK in the asset folder to the newly prepared path. */
        bRtnCode = copyOrigApkFromAssetToInternal(ctxBase, szOrigApk);
        if (!bRtnCode)
            return;

        /*  Assign the class loader of this proxy application to the original application. */
        bRtnCode = assignClassLoaderToOrigApk(ctxBase, szOrigApk, szOptDex, szLib);
        if (!bRtnCode)
            return;

        return;
    }

    public void onCreate()
    {
        return;
    }

    private boolean copyOrigApkFromAssetToInternal(Context ctxBase, String szOrigApk)
    {
        boolean bRtnCode = true;

        File fileApk = new File(szOrigApk);
        AssetManager astMgr = ctxBase.getAssets();
        InputStream isSrc = null;
        OutputStream osTge = null;
        try {
            isSrc = astMgr.open(NAME_ORIGINAL_APK);
            osTge = new FileOutputStream(fileApk);
            byte[] aPayload = new byte[SIZE_BUF];
            int iCntRead;
            while (true) {
                iCntRead = isSrc.read(aPayload);
                if (iCntRead < 0)
                    break;
                osTge.write(aPayload, 0, iCntRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            bRtnCode = false;
        } finally {
            if (isSrc != null) {
                try {
                    isSrc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    bRtnCode = false;
                }
            }
            if (osTge != null) {
                try {
                    osTge.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    bRtnCode = false;
                }
            }
        }

        return bRtnCode;
    }

    private boolean assignClassLoaderToOrigApk(Context ctxBase, String szOrigApk, String szOptDex, String szLib)
    {
        Object objCurActThrd = ReflectionWrapper.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread",
                                                            new Class[]{}, new Object[]{});
        if (objCurActThrd == null) {
            Log.e(LOGD_TAG_ERROR, "Cannot get the current activity thread.");
            return false;
        }

        ArrayMap mapPkg = (ArrayMap) ReflectionWrapper.getFieldObject("android.app.ActivityThread", objCurActThrd, "mPackages");
        if (mapPkg == null) {
            Log.e(LOGD_TAG_ERROR, "Cannot get the running package map.");
            return false;
        }

        String szPkg = ctxBase.getPackageName();
        WeakReference wrefPkg = (WeakReference)mapPkg.get(szPkg);
        if (wrefPkg == null) {
            Log.e(LOGD_TAG_ERROR, "Cannot get the weak reference of this proxy app.");
            return false;
        }

        ClassLoader ldProxyApk = (ClassLoader)ReflectionWrapper.getFieldObject("android.app.LoadedApk", wrefPkg.get(),
                                                                     "mClassLoader");
        if (ldProxyApk == null) {
            Log.e(LOGD_TAG_ERROR, "Cannot get the class loader of this proxy app.");
            return false;
        }
        DexClassLoader dxldOrigApk = new DexClassLoader(szOrigApk, szOptDex, szLib, ldProxyApk);
        if (dxldOrigApk == null) {
            Log.e(LOGD_TAG_ERROR, "Cannot create the class loader for the original app.");
            return false;
        }

        ReflectionWrapper.setFieldObject("android.app.LoadedApk", wrefPkg.get(), "mClassLoader", dxldOrigApk);

        return true;
    }
}
