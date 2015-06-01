package org.zsshen.bmi;

public class CommonConstants {
    /* Constants related to service components. */
    public static final char ID_PASSIVE_SERVICE = 'P';
    public static final char ID_ACTIVE_SERVICE = 'A';
    public static final String MSG_KEY_PASSIVE_NOTIFY = "P";
    public static final String MSG_KEY_ACTIVE_NOTIFY = "A";
    public static final String MSG_VALUE_PASSIVE_NOTIFY = "PassiveService";
    public static final String MSG_VALUE_ACTIVE_NOTIFY = "ActiveService";
    public static final int TIMER_THREAD_SLEEP_PERIOD = 5000;

    /* Constants related to content provider components. */
    public static final String DB_NAME = "MainDB";
    public static final int DB_VERSION = 1;

    public static final String TBL_NAME = "MainTable";
    public static final String COL_ID = "Id";
    public static final String COL_WEIGHT = "Weight";
    public static final String COL_HEIGHT = "Height";
    public static final String COL_BMI = "BMI";

    public static final String AUTHORITY = "org.zsshen.bmi";
    public static final String RESOURCE = "MainProvider";
    public static final String MAIN_URI = "content://" + AUTHORITY + "/" + RESOURCE;
}
