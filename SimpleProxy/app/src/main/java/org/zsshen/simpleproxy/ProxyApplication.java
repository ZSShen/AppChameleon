package org.zsshen.simpleproxy;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import dalvik.system.DexClassLoader;

public class ProxyApplication extends Application {
    static private String NAME_ORIGINAL_APK = "Original.apk";
    static private String NAME_DIR_OPTMIZE_DEX = "DynamicOptDex";
    static private String NAME_DIR_LIB = "DynamicLib";
    static private String STRING_META_APK_CLASS = "ORIGINAL_APPLICATION_CLASS_NAME";
    static private String LOGD_TAG_ERROR = "Error(SimpleProxy)";
    static private String LOGD_TAG_DEBUG = "Debug(SimpleProxy)";
    static private int SIZE_BUF = 1024;

    static private Context mCtxBase = null;
    static private Application mOrigApp = null;

    protected void attachBaseContext(Context ctxBase) {
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

        /* Save the current context for later utilization. */
        mCtxBase = ctxBase;
        return;
    }

    public void onCreate() {
        boolean bRtnCode = true;

        StringBuffer sbApkClass = new StringBuffer();
        bRtnCode = getOrigApkClassName(sbApkClass);
        if (!bRtnCode)
            return;

        String szApkClass = sbApkClass.toString();
        bRtnCode = patchRuntimeForOrigApk(szApkClass);
        if (!bRtnCode)
            return;

        mOrigApp.onCreate();
        return;
    }

    private boolean copyOrigApkFromAssetToInternal(Context ctxBase, String szOrigApk) {
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

    private boolean assignClassLoaderToOrigApk(Context ctxBase, String szOrigApk, String szOptDex, String szLib) {
        Object objCurActThrd = ReflectionWrapper.invokeStaticMethod("android.app.ActivityThread",
                "currentActivityThread", new Class[]{}, new Object[]{});
        if (objCurActThrd == null)
            return false;
        ArrayMap mapPkg = (ArrayMap) ReflectionWrapper.getFieldObject("android.app.ActivityThread",
                objCurActThrd, "mPackages");
        if (mapPkg == null)
            return false;
        String szPkg = ctxBase.getPackageName();
        WeakReference wrefPkg = (WeakReference) mapPkg.get(szPkg);
        if (wrefPkg == null)
            return false;

        ClassLoader ldProxyApk = (ClassLoader) ReflectionWrapper.getFieldObject("android.app.LoadedApk",
                wrefPkg.get(), "mClassLoader");
        if (ldProxyApk == null)
            return false;
        DexClassLoader dxldOrigApk = new DexClassLoader(szOrigApk, szOptDex, szLib, ldProxyApk);
        if (dxldOrigApk == null)
            return false;
        ReflectionWrapper.setFieldObject("android.app.LoadedApk", wrefPkg.get(), "mClassLoader", dxldOrigApk);

        return true;
    }

    private boolean getOrigApkClassName(StringBuffer sbApk) {
        try {
            PackageManager pkgMgr = mCtxBase.getPackageManager();
            if (pkgMgr == null)
                return false;
            String szApkPkg = mCtxBase.getPackageName();
            ApplicationInfo appInfo = pkgMgr.getApplicationInfo(szApkPkg, PackageManager.GET_META_DATA);
            if (appInfo == null)
                return false;

            Bundle bdlXml = appInfo.metaData;
            if (bdlXml == null)
                return false;
            if (!bdlXml.containsKey(STRING_META_APK_CLASS))
                return false;

            sbApk.append(bdlXml.getString(STRING_META_APK_CLASS));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean patchRuntimeForOrigApk(String szApkClass) {
        Object objCurActThrd = ReflectionWrapper.invokeStaticMethod("android.app.ActivityThread",
                "currentActivityThread", new Class[]{}, new Object[]{});
        if (objCurActThrd == null)
            return false;

        Object objBoundApp = ReflectionWrapper.getFieldObject("android.app.ActivityThread", objCurActThrd,
                "mBoundApplication");
        if (objBoundApp == null)
            return false;
        Object objLoadedApk = ReflectionWrapper.getFieldObject("android.app.ActivityThread$AppBindData",
                objBoundApp, "info");
        if (objLoadedApk == null)
            return false;
        ReflectionWrapper.setFieldObject("android.app.LoadedApk", objLoadedApk, "mApplication", null);

        Object objProxyApp = ReflectionWrapper.getFieldObject("android.app.ActivityThread", objCurActThrd,
                "mInitialApplication");
        if (objProxyApp == null)
            return false;
        ArrayList listAllApp = (ArrayList) ReflectionWrapper.getFieldObject("android.app.ActivityThread",
                objCurActThrd, "mAllApplications");
        if (listAllApp == null)
            return false;
        listAllApp.remove(objProxyApp);

        ApplicationInfo infoOrigApp = (ApplicationInfo) ReflectionWrapper.getFieldObject("android.app.LoadedApk",
                objLoadedApk, "mApplicationInfo");
        if (infoOrigApp == null)
            return false;
        ApplicationInfo infoBindDataOrigApp = (ApplicationInfo) ReflectionWrapper.getFieldObject(
                "android.app.ActivityThread$AppBindData", objBoundApp, "appInfo");
        if (infoBindDataOrigApp == null)
            return false;
        infoOrigApp.className = szApkClass;
        infoBindDataOrigApp.className = szApkClass;

        mOrigApp = (Application) ReflectionWrapper.invokeMethod("android.app.LoadedApk", "makeApplication",
                objLoadedApk,new Class[] {boolean.class, Instrumentation.class}, new Object[] {false, null});
        if (mOrigApp == null) {
            Log.e(LOGD_TAG_ERROR, "Cannot create the original app.");
            return false;
        }
        ReflectionWrapper.setFieldObject("android.app.ActivityThread", objCurActThrd, "mInitialApplication",
                mOrigApp);

        ArrayMap mapProvider = (ArrayMap) ReflectionWrapper.getFieldObject("android.app.ActivityThread",
                objCurActThrd, "mProviderMap");
        if (mapProvider == null)
            return false;
        Iterator iterMap = mapProvider.values().iterator();
        while (iterMap.hasNext()) {
            Object objClientRecord = iterMap.next();
            Object objLocalProvider = ReflectionWrapper.getFieldObject("android.app.ActivityThread$ProviderClientRecord",
                    objClientRecord, "mLocalProvider");
            ReflectionWrapper.setFieldObject("android.content.ContentProvider", objLocalProvider, "mContext",
                    mOrigApp);
        }

        return true;
    }
}
