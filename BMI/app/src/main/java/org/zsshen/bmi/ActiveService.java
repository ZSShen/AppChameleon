package org.zsshen.bmi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.zsshen.bmi.background.MessageHandler;
import org.zsshen.bmi.background.TimerThread;

public class ActiveService extends Service {
    static private String LOGD_TAG_DEBUG = "(BMI:ActiveService)";
    private Context mCtxApp;
    private MessageHandler mHanlder;
    private TimerThread mTimer;

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mCtxApp = getApplicationContext();

        /* Currently, we offer the toast message 2 second to show up and limit the
           pop up counts to 20. */
        mHanlder = new MessageHandler(mCtxApp, CommonConstants.ID_ACTIVE_SERVICE, 2000);
        mTimer = new TimerThread(20, mHanlder);
        mTimer.start();
        Log.d(LOGD_TAG_DEBUG, "The active service is started.");

        return START_STICKY;
    }

    public void onDestroy()
    {
        if (mTimer.isAlive())
            mTimer.interrupt();

        super.onDestroy();
        Log.d(LOGD_TAG_DEBUG, "The active service is destroyed.");
    }
}
