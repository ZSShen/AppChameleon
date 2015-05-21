package org.zsshen.protectedbmi;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class Protector extends Application {
    static private String NAME_SRC_APK = "SourceApp.apk";
    static private String NAME_DIR_OPT_DEX = "DynamicOptDex";
    static private String NAME_DIR_LIB = "DynamicLib";
    static private String NAME_SRC_MAIN_ACTIVITY = "org.zsshen.bmi.MainActivity";
    static private String META_KEY_APK_CLASS = "SRC_APP_CLASS_NAME";
    static private String LOGD_TAG_ERR = "(Protector)Error";
    static private String LOGD_TAG_DBG = "(Protector)Debug";
    static private int SIZE_BUF = 1024;

    static private Context mCtxBase = null;
    static private Application mSrcApp = null;

    /* Note that we term the protected BMI app as source app. */

    protected void attachBaseContext(Context ctxBase)
    {
        super.attachBaseContext(ctxBase);

        /* Save the current context for later utilization. */
        mCtxBase = ctxBase;

        /* Prepare the private storage for the dynamically loaded source APK. */
        File fileOptDex = ctxBase.getDir(NAME_DIR_OPT_DEX, MODE_PRIVATE);
        File fileLib = ctxBase.getDir(NAME_DIR_LIB, MODE_PRIVATE);
        String szPathOptDex = fileOptDex.getAbsolutePath();
        String szPathLib = fileLib.getAbsolutePath();
        File fileApk = new File(szPathOptDex, NAME_SRC_APK);
        String szPathSrcApk = fileApk.getAbsolutePath();

        /* Copy the source APK in the asset folder to the newly prepared path. */
        boolean bRtnCode = prepareSrcApk(ctxBase, szPathSrcApk);
        if (!bRtnCode)
            return;

        /* Create the custom class loader to load the source APK and replace the class loader
           of the current protector application with that one. */
        bRtnCode = replaceClassLoader(szPathSrcApk, szPathOptDex, szPathLib);
        if (!bRtnCode)
            return;

        return;
    }

    public void onCreate() {
        boolean bRtnCode = true;

        super.onCreate();
        StringBuffer sbApkClass = new StringBuffer();
        bRtnCode = getOrigApkClassName(sbApkClass);
        if (!bRtnCode)
            return;

        String szApkClass = sbApkClass.toString();
        bRtnCode = patchRuntimeForOrigApk(szApkClass);
        if (!bRtnCode)
            return;

        return;
    }

    private boolean prepareSrcApk(Context ctxBase, String szPathSrcApk)
    {
        boolean bRtnCode = true;

        File fileApk = new File(szPathSrcApk);
        AssetManager astMgr = ctxBase.getAssets();
        InputStream isSrc = null;
        OutputStream osTge = null;
        try {
            isSrc = astMgr.open(NAME_SRC_APK);
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

    private boolean replaceClassLoader(String szPathSrcApk, String szPathOptDex, String szPathLib)
    {
        try {
            Class clsActThrd = Class.forName("android.app.ActivityThread");
            Class clsLoaded = Class.forName("android.app.LoadedApk");

            Method mtdCurActTrd = clsActThrd.getMethod("currentActivityThread", new Class[]{});
            Object objCurActThrd = mtdCurActTrd.invoke(null, new Object[]{});

            Field fidPkg = clsActThrd.getDeclaredField("mPackages");
            fidPkg.setAccessible(true);
            ArrayMap mapPkg = (ArrayMap) fidPkg.get(objCurActThrd);

            String szPkg = getPackageName();
            WeakReference wrefPkg = (WeakReference) mapPkg.get(szPkg);

            Field fidLoader = clsLoaded.getDeclaredField("mClassLoader");
            fidLoader.setAccessible(true);
            ClassLoader ldrProtector = (ClassLoader) fidLoader.get(wrefPkg.get());
            DexClassLoader ldrSrcApk = new DexClassLoader(szPathSrcApk, szPathOptDex, szPathLib, ldrProtector);
            fidLoader.set(wrefPkg.get(), ldrSrcApk);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean getOrigApkClassName(StringBuffer sbApk) {
        try {
            PackageManager pkgMgr = mCtxBase.getPackageManager();
            if (pkgMgr == null)
                return false;
            String szApkPkg = mCtxBase.getPackageName();
            ApplicationInfo appInfo = pkgMgr.getApplicationInfo(szApkPkg, PackageManager.GET_META_DATA);
            if (appInfo == null) {
                Log.d(LOGD_TAG_DBG, "The original app does not have application class.");
                return false;
            }

            Bundle bdlXml = appInfo.metaData;
            if (bdlXml == null)
                return false;
            if (!bdlXml.containsKey(META_KEY_APK_CLASS))
                return false;

            sbApk.append(bdlXml.getString(META_KEY_APK_CLASS));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean patchRuntimeForOrigApk(String szApkClass) {
        try {
            /* Create the instance of the original app (appOrig). */
            /*
            Class clsOrig = Class.forName(szApkClass, true, getClassLoader());
            Application appOrig = (Application) clsOrig.newInstance();
            Log.d(LOGD_TAG_DBG, clsOrig.getName());
            */
            /* Get the instance of the proxy app (appProxy). */
            /*
            Application appProxy = (Application) getApplicationContext();
            */
            /* Force mbaseContext.mOuterContext to refer to appOrig. */
            /*
            Class clsCtxImpl = Class.forName("android.app.ContextImpl");
            Field fidOutCtx = clsCtxImpl.getDeclaredField("mOuterContext");
            fidOutCtx.setAccessible(true);
            fidOutCtx.set(mCtxBase, appOrig);
            */
            /* Get mPackageInfo of the context of the proxy app. */
            /*
            Field fidPkgInfo = clsCtxImpl.getDeclaredField("mPackageInfo");
            fidPkgInfo.setAccessible(true);
            Object mPackageInfo = fidPkgInfo.get(mCtxBase);
            */
            /* Force mPackageInfo.mApplication to refer to appOrig. */
            /*
            Class clsLoadedApk = Class.forName("android.app.LoadedApk");
            Field fidApp = clsLoadedApk.getDeclaredField("mApplication");
            fidApp.setAccessible(true);
            fidApp.set(mPackageInfo, appOrig);
            */
            /* Get mPackageInfo.mActivityThread object. */
            /*
            Class clsActThrd = Class.forName("android.app.ActivityThread");
            Field fidActThrd = clsLoadedApk.getDeclaredField("mActivityThread");
            fidActThrd.setAccessible(true);
            Object objActThrd = fidActThrd.get(mPackageInfo);
            */
            /* Force mActivityThread to refer to appOrig. */
            /*
            Field fidInitApp = clsActThrd.getDeclaredField("mInitialApplication");
            fidInitApp.setAccessible(true);
            fidInitApp.set(objActThrd, appOrig);
            */
            /* Replace the appProxy with the appOrig for mActivityThread.mAllApplications. */
            /*
            Field fidAllApps = clsActThrd.getDeclaredField("mAllApplications");
            fidAllApps.setAccessible(true);
            ArrayList<Application> alApp = (ArrayList<Application>)fidAllApps.get(objActThrd);
            alApp.add(appOrig);
            alApp.remove(appProxy);
            */

            /* Launch appOrig. */
            /*
            Method mtdAttach = Application.class.getDeclaredMethod("attach", Context.class);
            mtdAttach.setAccessible(true);
            mtdAttach.invoke(appOrig, mCtxBase);
            appOrig.onCreate();
            */

            /* Launch the source main activity. */
            ClassLoader ldrSrcApk = getClassLoader();
            Class clsAct = ldrSrcApk.loadClass(NAME_SRC_MAIN_ACTIVITY);
            Intent intAct = new Intent();
            intAct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intAct.setAction(Intent.ACTION_MAIN);
            intAct.setClass(this.getApplicationContext(), clsAct);
            intAct.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intAct);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
