package org.zsshen.bmi.background;

import org.zsshen.bmi.CommonConstants;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;


public class TimerThread implements Runnable {
    static private String LOGD_TAG_DEBUG = "(BMI:Background:TimerThread)";
    private int mLoopCount;
    private MessageHandler mMsgHandler;

    public TimerThread(int iLoopCount, MessageHandler msgHandler)
    {
        mLoopCount = iLoopCount;
        mMsgHandler = msgHandler;
    }

    public void run()
    {
        try {
            char cIdService = mMsgHandler.getServiceId();
            for (int i = 0 ; i < mLoopCount ; i++) {
                Thread.sleep(CommonConstants.TIMER_THREAD_SLEEP_PERIOD);
                Bundle bdlMsg = new Bundle();
                switch (cIdService) {
                    case CommonConstants.ID_PASSIVE_SERVICE:
                        bdlMsg.putString(CommonConstants.MSG_KEY_PASSIVE_NOTIFY,
                                         CommonConstants.MSG_VALUE_PASSIVE_NOTIFY);
                        break;
                    case CommonConstants.ID_ACTIVE_SERVICE:
                        bdlMsg.putString(CommonConstants.MSG_KEY_ACTIVE_NOTIFY,
                                         CommonConstants.MSG_VALUE_ACTIVE_NOTIFY);
                        break;
                    default:
                        Log.d(LOGD_TAG_DEBUG, "Invalid service ID.");
                        return;
                }

                Message msg = new Message();
                msg.setData(bdlMsg);
                mMsgHandler.sendMessage(msg);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
