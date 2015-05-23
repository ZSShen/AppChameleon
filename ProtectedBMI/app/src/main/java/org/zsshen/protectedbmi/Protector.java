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
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

public class Protector extends Application {
    static private String NAME_SRC_APK = "SourceApp.apk";
    static private String NAME_DIR_OPT_DEX = "DynamicOptDex";
    static private String NAME_DIR_LIB = "DynamicLib";
    static private String NAME_SRC_MAIN_ACTIVITY = "org.zsshen.bmi.MainActivity";
    static private String META_KEY_APP_CLASS = "SRC_APP_CLASS_NAME";
    static private String LOGD_TAG_ERR = "(Protector)Error";
    static private String LOGD_TAG_DBG = "(Protector)Debug";
    static private int SIZE_BUF = 1024;

    static private Context mCtxBase = null;

    /*--------------------------------------------------------*
     * Note that we term the protected BMI app as source app  *
     * and the currently launched app as protector app.       *
     *--------------------------------------------------------*/
    protected void attachBaseContext(Context ctxBase)
    {
        super.attachBaseContext(ctxBase);

        /* Save the current context for later manipulation. */
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

        /* Create the custom class loader to load the source APK and replace the
           class loader of the current protector application with that one. */
        replaceClassLoader(szPathSrcApk, szPathOptDex, szPathLib);
        return;
    }

    public void onCreate()
    {
        boolean bRtnCode = true;

        super.onCreate();
        StringBuffer sbAppClass = new StringBuffer();
        bRtnCode = getSrcAppClassName(sbAppClass);
        if (!bRtnCode)
            return;

        /* If the source APK contains application class, we should prepare the context
           environment to let it execute smoothly. */
        if (sbAppClass.length() > 0) {
            String szAppClass = sbAppClass.toString();
            bRtnCode = replaceApplicationClass(szAppClass);
            if (!bRtnCode)
                return;
        }

        /* Launch the main activity of the source APK. */
        launchSrcMainActivity();
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
            DexClassLoader ldrSrcApk = new DexClassLoader(szPathSrcApk, szPathOptDex,
                                       szPathLib, ldrProtector);
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

    private boolean getSrcAppClassName(StringBuffer sbAppClass)
    {
        try {
            PackageManager pkgMgr = mCtxBase.getPackageManager();
            String szApkPkg = mCtxBase.getPackageName();
            ApplicationInfo appInfo = pkgMgr.getApplicationInfo(szApkPkg,
                                      PackageManager.GET_META_DATA);
            if (appInfo == null) {
                Log.d(LOGD_TAG_DBG, "The source APK does not have application class.");
                return false;
            }

            Bundle bdlXml = appInfo.metaData;
            if (bdlXml == null)
                return false;
            if (!bdlXml.containsKey(META_KEY_APP_CLASS))
                return false;

            sbAppClass.append(bdlXml.getString(META_KEY_APP_CLASS));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean replaceApplicationClass(String szAppClass)
    {
        try {
            /* Create the instance of the source application class. */
            Class clsSrcApp = Class.forName(szAppClass, true, getClassLoader());
            Application appSrc = (Application) clsSrcApp.newInstance();

            /* Get the instance of the protector application. */
            Application appProtector = (Application) getApplicationContext();

        /*-------------------------------------------------------------------------------*
         * The type of mCtxBase is "android.app.ContextImpl" which is the implementation *
         * of "android.app.Context". We should replace all of its related references to  *
         * "android.app.Application". Now they all reference to the protector application*
         * class, and we should force them to reference to the source application class. *
         *-------------------------------------------------------------------------------*/
            Class clsCtxImpl = Class.forName("android.app.ContextImpl");

            /* For "android.app.ContextImpl.mOuterContext". */
            Field fidOutCtx = clsCtxImpl.getDeclaredField("mOuterContext");
            fidOutCtx.setAccessible(true);
            fidOutCtx.set(mCtxBase, appSrc);

            /* For "android.app.ContextImpl.mPackageInfo.mApplication". */
            Field fidPkgInfo = clsCtxImpl.getDeclaredField("objPkgInfo");
            fidPkgInfo.setAccessible(true);
            Object objPkgInfo = fidPkgInfo.get(mCtxBase);

            Class clsLoadedApk = Class.forName("android.app.LoadedApk");
            Field fidApp = clsLoadedApk.getDeclaredField("mApplication");
            fidApp.setAccessible(true);
            fidApp.set(objPkgInfo, appSrc);

            /* For "android.app.ContextImpl.mPackageInfo.mActivityThread.mInitialApplication". */
            Field fidActThrd = clsLoadedApk.getDeclaredField("mActivityThread");
            fidActThrd.setAccessible(true);
            Object objActThrd = fidActThrd.get(objPkgInfo);

            Class clsActThrd = Class.forName("android.app.ActivityThread");
            Field fidInitApp = clsActThrd.getDeclaredField("mInitialApplication");
            fidInitApp.setAccessible(true);
            fidInitApp.set(objActThrd, appSrc);

            /* For "android.app.ContextImpl.mPackageInfo.mActivityThread.mAllApplication". */
            Field fidAllApps = clsActThrd.getDeclaredField("mAllApplications");
            fidAllApps.setAccessible(true);
            ArrayList<Application> listApp = (ArrayList<Application>)fidAllApps.get(objActThrd);
            listApp.add(appSrc);
            listApp.remove(appProtector);

            /* Let the source application class attach th mCtxBase. */
            Method mtdAttach = Application.class.getDeclaredMethod("attach", Context.class);
            mtdAttach.setAccessible(true);
            mtdAttach.invoke(appSrc, mCtxBase);
            appSrc.onCreate();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean launchSrcMainActivity()
    {
        try {
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
