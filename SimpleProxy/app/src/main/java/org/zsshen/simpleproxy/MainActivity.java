package org.zsshen.simpleproxy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    static private String LOGD_TAG_DEBUG = "Debug(SimpleProxy)";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGD_TAG_DEBUG, "The Proxy Activity is created.");

        finish();
    }
}
