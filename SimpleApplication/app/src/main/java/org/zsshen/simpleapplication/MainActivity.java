package org.zsshen.simpleapplication;

import java.text.DecimalFormat;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity {
    static private String LOGD_TAG_DEBUG = "Debug(SimpleApplication)";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGD_TAG_DEBUG, "The MainActivity is created.");
        setContentView(R.layout.activity_main);

        /* Listen for button clicks. */
        Button button = (Button)findViewById(R.id.buttonCalculate);
        Log.d(LOGD_TAG_DEBUG, button.toString());
        button.setOnClickListener(clickBmi);

        Log.d(LOGD_TAG_DEBUG, "The UI is prepared.");
        return;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private OnClickListener clickBmi = new OnClickListener() {
        public void onClick(View v) {
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
            return;
        }
    };
}