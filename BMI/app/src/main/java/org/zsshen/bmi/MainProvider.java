package org.zsshen.bmi;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.zsshen.bmi.helpgadget.SQLiteDBHelper;

import java.util.HashMap;

public class MainProvider extends ContentProvider {
    private SQLiteDBHelper mDbHelper;
    private HashMap<String, String> mMapProject;

    static private String LOGD_TAG_DEBUG = "(BMI:MainProvider)";
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int MULTIPLE_ROW = 1;
    private static final int SINGLE_ROW = 2;
    private static final String AUTHORITY = "org.zsshen.bmi.MainProvider";

    static {
        URI_MATCHER.addURI(AUTHORITY, CommonConstants.TBL_NAME, MULTIPLE_ROW);
        URI_MATCHER.addURI(AUTHORITY, CommonConstants.TBL_NAME + "/#", SINGLE_ROW);
    }

    public boolean onCreate()
    {
        Log.d(LOGD_TAG_DEBUG, "The main content provider is created.");
        mDbHelper = new SQLiteDBHelper(getContext());
        return (mDbHelper != null)? true : false;
    }

    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder)
    {
        SQLiteDatabase dbSQLite = mDbHelper.getWritableDatabase();
        SQLiteQueryBuilder dbQueryBuilder = new SQLiteQueryBuilder();
        dbQueryBuilder.setTables(CommonConstants.TBL_NAME);

        switch (URI_MATCHER.match(uri)) {
            case MULTIPLE_ROW:
                dbQueryBuilder.setProjectionMap(mMapProject);
                break;
            case SINGLE_ROW:
                dbQueryBuilder.appendWhere(CommonConstants.COL_ID + "=" +
                        uri.getPathSegments().get(1));
                break;
            default:
                Log.d(LOGD_TAG_DEBUG, "Unknown URI: " + uri.toString());
                return null;
        }

        Cursor cursor = dbQueryBuilder.query(dbSQLite, projection, selection,
                selectionArgs, null, null, sortOrder);
        return cursor;
    }

    public Uri insert(Uri uri, ContentValues values)
    {
        SQLiteDatabase dbSQLite = mDbHelper.getWritableDatabase();
        long lRowId = dbSQLite.insert(CommonConstants.TBL_NAME, "", values);
        if (lRowId > 0) {
            Uri uriInsert = ContentUris.withAppendedId(uri, lRowId);
            getContext().getContentResolver().notifyChange(uriInsert, null);
            return uriInsert;
        }

        Log.d(LOGD_TAG_DEBUG, "Fail to insert a record to: " + uri.toString());
        return null;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        int iCountDel = 0;
        SQLiteDatabase dbSQLite = mDbHelper.getWritableDatabase();

        switch (URI_MATCHER.match(uri)) {
            case MULTIPLE_ROW:
                iCountDel = dbSQLite.delete(CommonConstants.TBL_NAME, selection, selectionArgs);
                break;
            case SINGLE_ROW:
                String szId = uri.getPathSegments().get(1);
                String szCond = TextUtils.isEmpty(selection)? "" : " AND (" + selection + ")";
                szCond = CommonConstants.COL_ID + "=" + szId + szCond;
                iCountDel = dbSQLite.delete(CommonConstants.TBL_NAME, szCond, selectionArgs);
                break;
            default:
                Log.d(LOGD_TAG_DEBUG, "Fail to delete a record from: " + uri.toString());
                return iCountDel;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return iCountDel;
    }

    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs)
    {
        int iCountUpd = 0;
        SQLiteDatabase dbSQLite = mDbHelper.getWritableDatabase();

        switch (URI_MATCHER.match(uri)) {
            case MULTIPLE_ROW:
                iCountUpd = dbSQLite.update(CommonConstants.TBL_NAME, values, selection,
                            selectionArgs);
                break;
            case SINGLE_ROW:
                String szId = uri.getPathSegments().get(1);
                String szCond = TextUtils.isEmpty(selection)? "" : " AND (" + selection + ")";
                szCond = CommonConstants.COL_ID + "=" + szId + szCond;
                iCountUpd = dbSQLite.update(CommonConstants.TBL_NAME, values, szCond,
                            selectionArgs);
                break;
            default:
                Log.d(LOGD_TAG_DEBUG, "Fail to update a record from: " + uri.toString());
                return iCountUpd;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return iCountUpd;
    }

    public String getType(Uri uri)
    {
        switch (URI_MATCHER.match(uri)) {
            case MULTIPLE_ROW:
                return "vnd.android.cursor.dir/" + CommonConstants.TBL_NAME;
            case SINGLE_ROW:
                return "vnd.android.cursor.item/" + CommonConstants.TBL_NAME;
            default:
                Log.d(LOGD_TAG_DEBUG, "UnKnown URI: " + uri.toString());
        }

        return null;
    }
}
