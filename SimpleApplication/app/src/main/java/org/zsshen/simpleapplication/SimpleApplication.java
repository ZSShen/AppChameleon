package org.zsshen.simpleapplication;


import android.app.Application;
import android.util.Log;

public class SimpleApplication extends Application {
    static private String LOGD_TAG_DEBUG = "Debug(SimpleApplication)";

    public void onCreate() {
        Log.d(LOGD_TAG_DEBUG, "The SimpleApplication is created.");
        return;
    }
}
