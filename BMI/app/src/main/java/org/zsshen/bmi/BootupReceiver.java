package org.zsshen.bmi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootupReceiver extends BroadcastReceiver {
    static private String LOGD_TAG_DEBUG = "(BMI:BootupReceiver)";

    public void onReceive(Context context, Intent intent)
    {
        /* Start the active service. */
        Log.d(LOGD_TAG_DEBUG, "The bootup receiver is created.");
        Intent intSrv = new Intent(context, ActiveService.class);
        context.startService(intSrv);
    }
}
