package org.zsshen.protectedbmi;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        Log.d(LOGD_TAG_DBG, "Attatch to the base context of protector app.");
        Log.d(LOGD_TAG_DBG, ctxBase.toString());

        /* Save the current context for later manipulation. */
        mCtxBase = ctxBase;

        /* Prepare the private storage for the dynamically loaded source APK. */
        File fileOptDex = ctxBase.getDir(NAME_DIR_OPT_DEX, MODE_PRIVATE);
        File fileLib = ctxBase.getDir(NAME_DIR_LIB, MODE_PRIVATE);
        String szPathOptDex = fileOptDex.getAbsolutePath();
        String szPathLibRoot = fileLib.getAbsolutePath();
        File fileApk = new File(szPathOptDex, NAME_SRC_APK);
        String szPathSrcApk = fileApk.getAbsolutePath();

        /* Copy the source APK in the asset folder to the newly prepared path. */
        boolean bRtnCode = prepareSrcApk(szPathSrcApk);
        if (!bRtnCode)
            return;
        /* Copy the ELF libraries from the source APK to the newly prepared path. */
        StringBuffer sbLib = new StringBuffer();
        bRtnCode = prepareSrcLib(szPathSrcApk, szPathLibRoot, sbLib);
        if (!bRtnCode)
            return;
        Log.d(LOGD_TAG_DBG, "Unpack the source APK.");

        /* Create the custom class loader to load the source APK and replace the
           class loader of the current protector application with that one. */
        String szPathLibTree = sbLib.toString();
        replaceClassLoader(szPathSrcApk, szPathOptDex, szPathLibTree);
        Log.d(LOGD_TAG_DBG, "Replace the class loader.");

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
        Log.d(LOGD_TAG_DBG, "Get the name of source application class.");

        /* If the source APK contains application class, we should prepare the context
           environment to let it execute smoothly. */
        if (sbAppClass.length() > 0) {
            String szAppClass = sbAppClass.toString();
            bRtnCode = replaceApplicationClass(szAppClass);
            if (!bRtnCode)
                return;
            Log.d(LOGD_TAG_DBG, "Launch the source application class.");
        }

        /* Launch the main activity of the source APK. */
        launchSrcMainActivity();
        Log.d(LOGD_TAG_DBG, "Launch the source main activity.");

        return;
    }

    private boolean prepareSrcApk(String szPathSrcApk)
    {
        boolean bRtnCode = true;

        File fileApk = new File(szPathSrcApk);
        AssetManager astMgr = mCtxBase.getAssets();
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

    private boolean prepareSrcLib(String szPathSrcApk, String szPathLibRoot, StringBuffer sbLib)
    {
        boolean bRtnCode = true;

        /* Get the processor architecture. */
        String[] aszABI = Build.SUPPORTED_ABIS;
        ArrayList<String> listABI = new ArrayList<String>();
        for (String szABI : aszABI) {
            File fileLibDir = new File("lib", szABI);
            listABI.add(fileLibDir.toString());
        }

        File fileApk = new File(szPathSrcApk);
        ZipInputStream zipIs = null;
        try {
            zipIs = new ZipInputStream(new FileInputStream(fileApk));
            ZipEntry entry;
            while ((entry = zipIs.getNextEntry()) != null) {
                String szEntryName = entry.getName();
                boolean bTgeABI = false;
                for (String szPrefix : listABI) {
                    if (szEntryName.startsWith(szPrefix) == true) {
                        bTgeABI = true;
                        break;
                    }
                }
                if (bTgeABI == false)
                    continue;

                /* Extract the processor dependent library from the zip entry. */
                ByteArrayOutputStream baosEntry = new ByteArrayOutputStream();
                byte[] aRead = new byte[SIZE_BUF];
                int iCount;
                while ((iCount = zipIs.read(aRead)) != -1)
                    baosEntry.write(aRead, 0, iCount);
                byte[] aLibBin = baosEntry.toByteArray();

                /* Copy the library into the private folder. */
                File fileLib = new File(szPathLibRoot, szEntryName);
                String szPathLib = fileLib.getAbsolutePath();
                String szPathParent = fileLib.getParent();
                File fileParent = new File(szPathParent);
                fileParent.mkdirs();
                fileLib = new File(szPathLib);
                OutputStream osLib = new FileOutputStream(fileLib);
                osLib.write(aLibBin, 0, aLibBin.length);
                osLib.close();
                sbLib.append(szPathParent);
                sbLib.append(File.pathSeparator);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            bRtnCode = false;
        } catch (IOException e) {
            e.printStackTrace();
            bRtnCode = false;
        } finally {
            if (zipIs != null) {
                try {
                    zipIs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    bRtnCode = false;
                }
            }
        }
        return bRtnCode;
    }

    private boolean replaceClassLoader(String szPathSrcApk, String szPathOptDex, String szPathLibTree)
    {
        try {
            Class clsActThrd = Class.forName("android.app.ActivityThread");
            Class clsLoaded = Class.forName("android.app.LoadedApk");

            Method mtdCurActTrd = clsActThrd.getMethod("currentActivityThread", new Class[]{});
            Object objCurActThrd = mtdCurActTrd.invoke(null, new Object[]{});

            Field fidPkg = clsActThrd.getDeclaredField("mPackages");
            fidPkg.setAccessible(true);
            ArrayMap mapPkg = (ArrayMap) fidPkg.get(objCurActThrd);

            String szPkg = mCtxBase.getPackageName();
            WeakReference wrefPkg = (WeakReference) mapPkg.get(szPkg);

            Field fidLoader = clsLoaded.getDeclaredField("mClassLoader");
            fidLoader.setAccessible(true);
            ClassLoader ldrProtector = (ClassLoader) fidLoader.get(wrefPkg.get());
            DexClassLoader ldrSrcApk = new DexClassLoader(szPathSrcApk, szPathOptDex,
                    szPathLibTree, ldrProtector);
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
            return false;
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
            Log.d(LOGD_TAG_DBG, getClassLoader().toString());
            Class clsSrcApp = Class.forName(szAppClass, true, getClassLoader());
            Application appSrc = (Application) clsSrcApp.newInstance();

            /* Get the instance of the protector application. */
            Application appProtector = (Application) getApplicationContext();

            Log.d(LOGD_TAG_DBG, "Check the context of protector app.");
            Log.d(LOGD_TAG_DBG, appProtector.toString());
            Log.d(LOGD_TAG_DBG, "Check the base context of protector app.");
            Log.d(LOGD_TAG_DBG, mCtxBase.toString());
            Log.d(LOGD_TAG_DBG, "Check the context of source app.");
            Log.d(LOGD_TAG_DBG, appSrc.toString());

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
            Field fidPkgInfo = clsCtxImpl.getDeclaredField("mPackageInfo");
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
            Log.d(LOGD_TAG_DBG, "Create the original application.");
            Log.d(LOGD_TAG_DBG, appSrc.toString());
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

    /*--------------------------------------------------------*
     * Override this function to force the content provider   *
     * of the source app to create its own context. We do not *
     * want that content provider reference to the protector  *
     * app.                                                   *
     *--------------------------------------------------------*/
    public String getPackageName() {
        return "";
    }
}
