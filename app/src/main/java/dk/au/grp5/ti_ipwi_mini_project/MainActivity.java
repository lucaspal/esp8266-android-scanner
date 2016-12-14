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
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    List<ScanResult> wifiList;
    WifiManager wm;
    WifiScanReceiver wifiReceiver;

    String  espPass, espSSID, netPass, netSSID;
    String espAddress;
    int espPort = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();
    }

    public void setup() {
        //Initiate wifi manager
        wm = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);

        // Requests permission if on 6.0 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
            }
        }

        //Enable wifi if disabled
        if (!wm.isWifiEnabled()) {
            wm.setWifiEnabled(true);
            Toast.makeText(this, "Enabling WiFi.", Toast.LENGTH_SHORT).show();
        }

        //Register broadcast receiver and start scan.
        wifiReceiver = new WifiScanReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wm.startScan();
        Toast.makeText(this, "Scan started.", Toast.LENGTH_LONG).show();
    }


    private void connect() {
        //Establishes a WPA connection
        //All data is set, checked in onConnectClick
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + getNetworkName() + "\"";

        conf.preSharedKey = "\"" + espPass + "\"";
        //Add network to the WifiManager
        wm.addNetwork(conf);
        //Connect using the configuration
        List<WifiConfiguration> list = wm.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + espSSID + "\"")) {
                wm.disconnect();
                wm.enableNetwork(i.networkId, true);
                wm.reconnect();
                break;
            }
        }
        //Get IP address
        DhcpInfo dhcpInfo = wm.getDhcpInfo();
        espAddress = Helper.formatIpAddress(dhcpInfo.serverAddress);
    }

    private void sendData() {
        PrintWriter out;

        //Check if connected to the right device
        WifiInfo wifiInfo = wm.getConnectionInfo();
        //Send data
        if(wifiInfo.getSSID().equals(espSSID)) try {
            Socket socket = new Socket(espAddress, espPort);

            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            out.println(Helper.formatData(netSSID, netPass));
            out.flush();

            //Close connection
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            Toast.makeText(this, "Cannot find host.", Toast.LENGTH_SHORT).show();
            Log.d("SOCKET EXCEPTION", e.toString());
        }catch (IOException e) {
            Toast.makeText(this, "Cannot open connection.", Toast.LENGTH_SHORT).show();
            Log.d("SOCKET EXCEPTION", e.toString());
        }  catch (Exception e) {
            Toast.makeText(this, "Something bad happened.", Toast.LENGTH_SHORT).show();
            Log.d("SOCKET EXCEPTION", e.toString());
        }
    }

    public class WifiScanReceiver extends BroadcastReceiver {

        //This method is called when the number of networks change
        public void onReceive (Context c, Intent intent) {
            List<String> wifiListString = new ArrayList<>();

            wifiList = wm.getScanResults();
            for(ScanResult net : wifiList) {
                wifiListString.add(net.SSID);
            }

            //Fill spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getApplicationContext(), android.R.layout.simple_spinner_item, wifiListString);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
            Spinner sWifi = (Spinner) findViewById(R.id.wifiSpinner);
            sWifi.setAdapter(adapter);
        }
    }

    private String getNetworkName() {
        String _name;
        //TODO: Check for null
        Spinner selectedValue = (Spinner) findViewById(R.id.wifiSpinner);
        _name = selectedValue.getSelectedItem().toString();
        return _name;
    }


    public void onConnectClick(View v) {
        Log.d("NETWORK NAME","Network to connect: " + getNetworkName());
        Log.d("NETWORK NAME","Password to use: " + espPass);
        Log.d("NETWORK NAME","SSID to send: " + netSSID);
        Log.d("NETWORK NAME","Password to send: " + netPass);

        if (getNetworkName() != null && espPass != null && netPass != null && netSSID != null) {
            connect();
            sendData();
        } else {
            Toast.makeText(this, "Insert credentials first.", Toast.LENGTH_LONG).show();
        }
    }
    public void onCheckConnection(View v) {
        if (!wm.isWifiEnabled()) {
            wm.setWifiEnabled(true);
            Toast.makeText(this, "Enabling WiFi.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "WiFi is on.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onEspPwClick(View v) {
        EditText text = (EditText) findViewById(R.id.epsPw_text);
        espPass = text.getText().toString();
    }

    public void onNetSsidClick(View v) {
        EditText text = (EditText) findViewById(R.id.netSsid_text);
        netSSID = text.getText().toString();
    }

    public void onNetPwClick(View v) {
        EditText text = (EditText) findViewById(R.id.netPw_text);
        netPass = text.getText().toString();
    }

    public void onChkStatusClick(View v) {
        Intent chkStatusActivity = new Intent(this, CheckStatusActivity.class);
        startActivity(chkStatusActivity);
    }
}
