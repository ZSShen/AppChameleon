package org.zsshen.bmi.background;

import org.zsshen.bmi.CommonConstants;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;


public class MessageHandler extends Handler {
    static private String LOGD_TAG_DEBUG = "(BMI:Background:MessageHandler)";
    private Context mCtxApp;
    private char mIdService;
    private int mDuration;

    public MessageHandler(Context ctxApp, char cIdService, int iDuration)
    {
        mCtxApp = ctxApp;
        mIdService = cIdService;
        mDuration = iDuration;
    }

    public void handleMessage(Message msg)
    {
        super.handleMessage(msg);
        String szMsg = null;
        switch (mIdService) {
            case CommonConstants.ID_PASSIVE_SERVICE:
                szMsg = msg.getData().getString(CommonConstants.MSG_KEY_PASSIVE_NOTIFY);
                break;
            case CommonConstants.ID_ACTIVE_SERVICE:
                szMsg = msg.getData().getString(CommonConstants.MSG_KEY_ACTIVE_NOTIFY);
                break;
            default:
                Log.d(LOGD_TAG_DEBUG, "Invalid service ID.");
        }

        if (szMsg == null)
            return;

        Toast toast = Toast.makeText(mCtxApp, szMsg, mDuration);
        switch (mIdService) {
            case CommonConstants.ID_PASSIVE_SERVICE:
                toast.setGravity(Gravity.BOTTOM | Gravity.LEFT, 0, 0);
                break;
            case CommonConstants.ID_ACTIVE_SERVICE:
                toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 0, 0);
                break;
        }
        toast.show();
    }

    public char getServiceId() {
        return mIdService;
    }
}
