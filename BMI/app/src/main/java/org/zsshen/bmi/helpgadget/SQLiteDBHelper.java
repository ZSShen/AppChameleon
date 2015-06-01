package org.zsshen.bmi.helpgadget;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.zsshen.bmi.CommonConstants;

public class SQLiteDBHelper extends SQLiteOpenHelper {

    public SQLiteDBHelper(Context context) {
        super(context, CommonConstants.DB_NAME, null, CommonConstants.DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db)
    {
        String command =
        "CREATE TABLE " + CommonConstants.TBL_NAME +
        " (" + CommonConstants.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        CommonConstants.COL_WEIGHT + " INTEGER, " +
        CommonConstants.COL_HEIGHT + " INTEGER, " +
        CommonConstants.COL_BMI + " REAL);";
        db.execSQL(command);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        String command = "DROP TABLE IF EXISTS " + CommonConstants.TBL_NAME;
        db.execSQL(command);
        onCreate(db);
    }
}
