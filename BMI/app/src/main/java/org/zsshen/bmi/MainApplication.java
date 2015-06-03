package org.zsshen.bmi;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MainApplication extends Application {
    static private String LOGD_TAG_DEBUG = "(BMI:Application)";

    protected void attachBaseContext(Context ctxBase)
    {
        super.attachBaseContext(ctxBase);
        Log.d(LOGD_TAG_DEBUG, "Attach to the base context.");
        return;
    }

    public void onCreate()
    {
        super.onCreate();
        Log.d(LOGD_TAG_DEBUG, "The main application is created.");

        int iNative = calcInManiApplication('+', 10, 20);
        Log.d(LOGD_TAG_DEBUG, "Call JNI in main application: " + String.valueOf(iNative));

        return;
    }

    static {
        System.loadLibrary("JniMainApplication");
        Log.d(LOGD_TAG_DEBUG, "The JNI for main application is loaded.");
    }
    public native int calcInManiApplication(char code, int op1, int op2);
}
