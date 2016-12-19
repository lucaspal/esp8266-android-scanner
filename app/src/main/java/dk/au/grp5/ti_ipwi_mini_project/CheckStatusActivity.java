package dk.au.grp5.ti_ipwi_mini_project;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CheckStatusActivity extends AppCompatActivity {

    private static final String API_ENDPOINT  = "https://tiipwilab.eng.au.dk/exercises/group5/app.php";
    private TextView mDevicesTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_status);

        mDevicesTv = (TextView) findViewById(R.id.devices_tv);
    }

    public void onRefreshClick(View v) {
        try {
            new DoGetStatusAsync().execute(new URL(API_ENDPOINT));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void onJsonResult(JSONObject jsonObject) {
        mDevicesTv.setText(jsonObject == null ? "None" : jsonObject.toString());
    }

    private class DoGetStatusAsync extends AsyncTask<URL, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(URL... urls) {
            URL url = urls[0];
            JSONObject result = null;

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);

                urlConnection.setDoOutput(true);

                urlConnection.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }

                br.close();
                result  = new JSONObject(sb.toString());
            } catch(IOException io) {
                io.printStackTrace();
            } catch(JSONException json) {
                json.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            onJsonResult(jsonObject);
            super.onPostExecute(jsonObject);
        }
    }
}
