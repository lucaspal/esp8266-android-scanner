package dk.au.grp5.ti_ipwi_mini_project;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<ScanResult> mApList;
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiReceiver;

    private String mEspApPassword, mTargetApPassword, mTargetApSssid;

    private String mEspIpAddress;
    private static final int ESP_PORT = 8000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeWifi();
    }

    public void initializeWifi() {
        mWifiManager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);

        // Requests permission if on 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
            }
        }

        // Enable WiFi if disabled
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Enabling WiFi.", Toast.LENGTH_SHORT).show();
        }

        // Register broadcast receiver and start scan
        mWifiReceiver = new WifiScanReceiver();
        registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
        Toast.makeText(this, "Scan started.", Toast.LENGTH_LONG).show();
    }


    private void connect() {
        // Establishes a WPA connection
        WifiConfiguration conf = new WifiConfiguration();

        conf.SSID = "\"" + getNetworkName() + "\"";
        conf.preSharedKey = "\"" + mEspApPassword + "\"";
        conf.status = WifiConfiguration.Status.ENABLED;
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

        // Add network to the WifiManager

        final int netId = mWifiManager.addNetwork(conf);

        if (netId > -1) {
            mWifiManager.disconnect();
            mWifiManager.enableNetwork(netId, true);
            mWifiManager.reconnect();
        }

        // Get IP address
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        mEspIpAddress = Helper.formatIpAddress(dhcpInfo.serverAddress);
    }

    private void sendData() {
        new DoSendDataAsync().execute();
    }

    private String getNetworkName() {
        Spinner selectedValue = (Spinner) findViewById(R.id.wifiSpinner);
        return selectedValue.getSelectedItem().toString();
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        // This method is called when the number of networks change
        public void onReceive(Context c, Intent intent) {
            List<String> wifiListString = new ArrayList<>();

            mApList = mWifiManager.getScanResults();
            for (ScanResult net : mApList) {
                wifiListString.add(net.SSID);
            }

            // Fill spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getApplicationContext(), android.R.layout.simple_spinner_item, wifiListString);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
            Spinner sWifi = (Spinner) findViewById(R.id.wifiSpinner);
            sWifi.setAdapter(adapter);
        }
    }

    private class DoSendDataAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (android.os.Debug.isDebuggerConnected())
                android.os.Debug.waitForDebugger();

            PrintWriter out;

            // Check if connected to the right device
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            //Send data
            if (wifiInfo.getSSID().equals("\"" + getNetworkName() + "\"")) try {
                Socket s = new Socket();
                s.setSoTimeout(10000);
                s.connect(new InetSocketAddress(mEspIpAddress, ESP_PORT));

                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
                String packet = Helper.formatData(mTargetApSssid, mTargetApPassword);
                out.println(packet);
                out.flush();

                //Close connection
                out.close();
                s.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }


    public void onConnectClick(View v) {
        if (getNetworkName() != null && mEspApPassword != null && mTargetApPassword != null && mTargetApSssid != null) {
            connect();
            sendData();
        } else {
            Toast.makeText(this, "Insert credentials first.", Toast.LENGTH_LONG).show();
        }
    }

    public void onCheckConnection(View v) {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Enabling WiFi.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "WiFi is on.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onEspPwClick(View v) {
        EditText text = (EditText) findViewById(R.id.epsPw_text);
        mEspApPassword = text.getText().toString();
    }

    public void onNetSsidClick(View v) {
        EditText text = (EditText) findViewById(R.id.netSsid_text);
        mTargetApSssid = text.getText().toString();
    }

    public void onNetPwClick(View v) {
        EditText text = (EditText) findViewById(R.id.netPw_text);
        mTargetApPassword = text.getText().toString();
    }

    public void onChkStatusClick(View v) {
        Intent chkStatusActivity = new Intent(this, CheckStatusActivity.class);
        startActivity(chkStatusActivity);
    }
}
