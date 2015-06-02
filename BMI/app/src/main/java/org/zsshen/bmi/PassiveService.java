package org.zsshen.bmi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.zsshen.bmi.background.MessageHandler;
import org.zsshen.bmi.background.TimerThread;

public class PassiveService extends Service {
    static private String LOGD_TAG_DEBUG = "(BMI:PassiveService)";
    private Context mCtxApp;
    private MessageHandler mHanlder;
    private TimerThread mTimer;

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mCtxApp = getApplicationContext();

        /* Currently, we offer the toast message 1 second to show up and limit the
           pop up counts to 10. */
        mHanlder = new MessageHandler(mCtxApp, CommonConstants.ID_PASSIVE_SERVICE, 1000);
        mTimer = new TimerThread(10, mHanlder);
        mTimer.start();
        Log.d(LOGD_TAG_DEBUG, "The passive service is started.");
        Log.d(LOGD_TAG_DEBUG, mCtxApp.toString());

        return START_STICKY;
    }

    public void onDestroy()
    {
        if (mTimer.isAlive())
            mTimer.interrupt();

        super.onDestroy();
        Log.d(LOGD_TAG_DEBUG, "The passive service is destroyed.");
    }
}
