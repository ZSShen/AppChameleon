package org.zsshen.simpleproxy;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ProxyApplication extends Application {
    static private String NAME_ORIGINAL_APK = "Original.apk";

    protected void attachBaseContext(Context ctxBase)
    {
        boolean bRtnCode = true;

        bRtnCode = getOriginalDex(ctxBase);
        if (!bRtnCode)
            return;

        return;
    }

    public void onCreate()
    {
        return;
    }

    private boolean getOriginalDex(Context ctxBase)
    {
        boolean bRtnCode = true;

        AssetManager astMgr = ctxBase.getAssets();
        try {
            InputStream isAst = astMgr.open(NAME_ORIGINAL_APK);
            ZipInputStream zisApk = new ZipInputStream(new BufferedInputStream(isAst));
            while (true) {
                ZipEntry entry = zisApk.getNextEntry();
                if (entry == null)
                    break;
                String szEntry = entry.getName();
                Log.d("Proxy Unpack", szEntry);
            }

        } catch (IOException e) {
            e.printStackTrace();
            bRtnCode = false;
        }
        return bRtnCode;
    }
}
