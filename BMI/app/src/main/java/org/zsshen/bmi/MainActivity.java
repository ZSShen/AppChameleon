package org.zsshen.bmi;

import java.text.DecimalFormat;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
    static private String LOGD_TAG_DEBUG = "(BMI:Activity)";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGD_TAG_DEBUG, "The main activity is created.");
        Log.d(LOGD_TAG_DEBUG, getApplicationContext().toString());
        setContentView(R.layout.activity_main);

        /* Listen for button clicks. */
        Button buttonCalc = (Button)findViewById(R.id.buttonCalculate);
        buttonCalc.setOnClickListener(clickCalc);

        Button buttonShow = (Button)findViewById(R.id.buttonShow);
        buttonShow.setOnClickListener(clickShow);
        Log.d(LOGD_TAG_DEBUG, "The UI is ready.");

        /* Start the passive service. */
        Intent intSrv = new Intent(getApplicationContext(), PassiveService.class);
        startService(intSrv);

        return;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private OnClickListener clickCalc = new OnClickListener() {
        public void onClick(View v)
        {
            /* Show the BMI value. */
            DecimalFormat formatter = new DecimalFormat("0.00");
            EditText txtHeight = (EditText)findViewById(R.id.inputHeight);
            EditText txtWeight = (EditText)findViewById(R.id.inputWeight);
            double dHeight = Double.parseDouble(txtHeight.getText().toString()) / 100;
            double dWeight = Double.parseDouble(txtWeight.getText().toString());
            double dBmi = dWeight / (dHeight * dHeight);
            TextView txtResult = (TextView)findViewById(R.id.textResult);
            txtResult.setText("Your BMI is " + formatter.format(dBmi));

            /* Give health advice. */
            TextView txtSuggest = (TextView)findViewById(R.id.textSuggest);
            if(dBmi > 25)
                txtSuggest.setText(R.string.advice_heavy);
            else if (dBmi < 20)
                txtSuggest.setText(R.string.advice_light);
            else
                txtSuggest.setText(R.string.advice_average);

            /* Insert the data into content provider. */
            int iHeight = (int)dHeight;
            int iWeight = (int)dWeight;

            ContentValues contentValues = new ContentValues();
            contentValues.put(CommonConstants.COL_HEIGHT, iHeight);
            contentValues.put(CommonConstants.COL_WEIGHT, iWeight);
            contentValues.put(CommonConstants.COL_BMI, dBmi);

            Uri uri = Uri.parse(CommonConstants.MAIN_URI);
            Uri uriInsert = getContentResolver().insert(uri, contentValues);
            Toast.makeText(getBaseContext(), uriInsert.toString(),
                    Toast.LENGTH_LONG).show();

            return;
        }
    };

    private OnClickListener clickShow = new OnClickListener() {
        public void onClick(View v)
        {
            /* Show all the records of content provider. */
            Uri uri = Uri.parse(CommonConstants.MAIN_URI);
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    String szId = cursor.getString(cursor.getColumnIndex(CommonConstants.COL_ID));
                    String szHeight = cursor.getString(cursor.getColumnIndex(CommonConstants.COL_HEIGHT));
                    String szWeight = cursor.getString(cursor.getColumnIndex(CommonConstants.COL_WEIGHT));
                    String szBMI = cursor.getString(cursor.getColumnIndex(CommonConstants.COL_BMI));
                    String szMsg = szId + ", " + szHeight + ", " + szWeight + ", " + szBMI;
                    Toast.makeText(getBaseContext(), szMsg, Toast.LENGTH_SHORT).show();
                } while (cursor.moveToNext());
            }
            return;
        }
    };

    protected void onDestroy()
    {
        super.onDestroy();

        /* Stop the passive service. */
        Intent intSrvP = new Intent(getApplicationContext(), PassiveService.class);
        stopService(intSrvP);

        /* Stop the active service. */
        Intent intSrvA = new Intent(getApplicationContext(), ActiveService.class);
        stopService(intSrvA);

        /* Delete all the records of content provider. */
        Uri uri = Uri.parse(CommonConstants.MAIN_URI);
        int iCountDel = getContentResolver().delete(uri, "1", null);
        String szMsg = iCountDel + " rows are cleaned from MainProvider.";
        Toast.makeText(getBaseContext(), szMsg, Toast.LENGTH_LONG).show();

        Log.d(LOGD_TAG_DEBUG, "The main activity is destroyed.");
    }
}
