package com.darryncampbell.enterprise.android.iot.client;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import static com.darryncampbell.enterprise.android.iot.client.MQTTGCP.MQTT_ALGORITHM;
import static com.darryncampbell.enterprise.android.iot.client.MQTTGCP.MQTT_CLOUD_REGION;
import static com.darryncampbell.enterprise.android.iot.client.MQTTGCP.MQTT_DEVICE_ID;
import static com.darryncampbell.enterprise.android.iot.client.MQTTGCP.MQTT_PRIVATE_KEY_NAME;
import static com.darryncampbell.enterprise.android.iot.client.MQTTGCP.MQTT_PROJECT_ID;
import static com.darryncampbell.enterprise.android.iot.client.MQTTGCP.MQTT_REGISTRY_ID;
import static com.darryncampbell.enterprise.android.iot.client.MQTTGCP.MQTT_SERVER_ENDPOINT;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    public static String TAG = "ent-iot-client";
    private static String GCP_DEVICE_ID = "test-device";
    private static String GCP_PROJECT_ID = "ent-android-iot-server-gcp";
    private static String GCP_REGISTRY_ID = "ent-android-iot-registry-gcp";
    private static String GCP_PRIVATE_KEY_NAME = "rsa_private_pkcs8";
    private static String GCP_ALGORITHM = "RS256";
    private static String GCP_CLOUD_REGION = "europe-west1";
    private MQTTInterface mqtt;
    private UUID sendRealDataWorkId;
    public static String LOCAL_BROADCAST_MESSAGE = "LOCAL_BROADCAST";

    //  UI elements
    TextView statusText, lblDeviceId, txtDeviceId, lblTestConnection;
    Button btnConnect, btnDisconnect, btnTestConnection;
    RadioGroup radioGroup;
    TextView lblProjectId, txtProjectId, lblRegistryId, txtRegistryId, lblPrivateKeyName, txtPrivateKeyName;
    TextView lblAlgorithm, txtAlgorithm, lblCloudRegion, txtCloudRegion;
    Switch switchSendDummyData, switchSendRealData;
    TextView lblModel, txtModel, lblLatitude, txtLatitude, lblLongitude, txtLongitude, lblBatteryLevel, txtBatteryLevel;
    TextView lblBatteryHealth, txtBatteryHealth, lblOSVersion, txtOSVersion, lblPatchLevel, txtPatchLevel, lblReleaseVersion, txtReleaseVersion;
    Button btnSendDummyData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.txtStatus);
        lblDeviceId = findViewById(R.id.lblDeviceId);
        txtDeviceId = findViewById(R.id.txtDeviceId);
        lblTestConnection = findViewById(R.id.lblTestConnection);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        radioGroup = findViewById(R.id.radioGroup);
        lblProjectId = findViewById(R.id.lblProjectId);
        txtProjectId = findViewById(R.id.txtProjectId);
        lblRegistryId = findViewById(R.id.lblRegistryId);
        txtRegistryId = findViewById(R.id.txtRegistryId);
        lblPrivateKeyName = findViewById(R.id.lblPrivateKeyName);
        txtPrivateKeyName = findViewById(R.id.txtPrivateKeyName);
        lblAlgorithm = findViewById(R.id.lblAlgorithm);
        txtAlgorithm = findViewById(R.id.txtAlgorithm);
        lblCloudRegion = findViewById(R.id.lblCloudRegion);
        txtCloudRegion = findViewById(R.id.txtCloudRegion);
        switchSendDummyData = findViewById(R.id.switchSendDummyData);
        switchSendRealData = findViewById(R.id.switchSendRealData);
        lblModel = findViewById(R.id.lblModel);
        txtModel = findViewById(R.id.txtModel);
        lblLatitude = findViewById(R.id.lblLatitude);
        txtLatitude = findViewById(R.id.txtLatitude);
        lblLongitude = findViewById(R.id.lblLongitude);
        txtLongitude = findViewById(R.id.txtLongitude);
        lblBatteryLevel = findViewById(R.id.lblBatteryLevel);
        txtBatteryLevel = findViewById(R.id.txtBatteryLevel);
        lblBatteryHealth = findViewById(R.id.lblBatteryHealth);
        txtBatteryHealth = findViewById(R.id.txtBatteryHealth);
        lblOSVersion = findViewById(R.id.lblOSVersion);
        txtOSVersion = findViewById(R.id.txtOSVersion);
        lblPatchLevel = findViewById(R.id.lblPatchLevel);
        txtPatchLevel = findViewById(R.id.txtPatchLevel);
        lblReleaseVersion = findViewById(R.id.lblReleaseVersion);
        txtReleaseVersion = findViewById(R.id.txtReleaseVersion);
        btnSendDummyData = findViewById(R.id.btnSendDummyData);

        if (radioGroup.getCheckedRadioButtonId() == R.id.radioGCP)
        {
            mqtt = new MQTTGCP();
        }
        else
        {
            //  todo
        }

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        configureUi(R.id.navigation_connect);
        prePopulateConnectionInfo(radioGroup.getCheckedRadioButtonId());
        prePopulateDummyData();

        switchSendDummyData.setOnCheckedChangeListener(this);
        switchSendRealData.setOnCheckedChangeListener(this);

        btnConnect.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnTestConnection.setOnClickListener(this);
        btnSendDummyData.setOnClickListener(this);

        //  Request permission to read external storage (needed to read the private key)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(LOCAL_BROADCAST_MESSAGE));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        sendRealData(false);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_connect:
                    configureUi(R.id.navigation_connect);
                    return true;
                case R.id.navigation_dummy_data:
                    configureUi(R.id.navigation_dummy_data);
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED && grantResults[1]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Required permissions were granted");
        }
        else if (requestCode == 0)
        {
            String errorMsg = "Read Storage permission is required for MQTTGCP Connection.  Location permission is required for Real location data";
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            showMessage(errorMsg);
            Log.e(TAG, errorMsg);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (compoundButton.getId() == R.id.switchSendDummyData)
        {
            if (isChecked)
                updateUIEnableDummyData(true);
            else
                updateUIEnableDummyData(false);
        }
        else if (compoundButton.getId() == R.id.switchSendRealData)
        {
            if (isChecked)
            {
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(false);
                btnTestConnection.setEnabled(false);
                sendRealData(true);
            }
            else
            {
                btnConnect.setEnabled(true);
                btnDisconnect.setEnabled(true);
                btnTestConnection.setEnabled(true);
                sendRealData(false);
            }
        }
    }

    @Override
    public void onClick(View view) {
        //  Button handlers
        if (view.getId() == R.id.btnConnect)
        {
            switchSendRealData.setEnabled(false);
            //  Test connection to MQTTGCP server
            String deviceId = txtDeviceId.getText().toString();
            String projectId = txtProjectId.getText().toString();
            String cloudRegion = txtCloudRegion.getText().toString();
            String registryId = txtRegistryId.getText().toString();
            String algorithm = txtAlgorithm.getText().toString();
            String privateKeyFile = txtPrivateKeyName.getText().toString();
            //todo
            Intent connectionConfiguration = new Intent();
            connectionConfiguration.putExtra(MQTTInterface.MQTT_DEVICE_ID, deviceId);
            connectionConfiguration.putExtra(MQTTInterface.MQTT_PROJECT_ID, projectId);
            connectionConfiguration.putExtra(MQTTInterface.MQTT_CLOUD_REGION, cloudRegion);
            connectionConfiguration.putExtra(MQTTInterface.MQTT_REGISTRY_ID, registryId);
            connectionConfiguration.putExtra(MQTTInterface.MQTT_ALGORITHM, algorithm);
            connectionConfiguration.putExtra(MQTTInterface.MQTT_PRIVATE_KEY_NAME, privateKeyFile);
            mqtt.initialise(connectionConfiguration);
            boolean connectSuccess = mqtt.connect();
            //Boolean connectSuccess =
            //        mqtt.connectToGoogleCloudIot(deviceId, projectId, cloudRegion, registryId, algorithm, privateKeyFile);
            if (connectSuccess)
                showMessage("Test client connected to " + projectId);
            else
                showMessage("Test client connect failed: " + mqtt.getLastConnectionError());
        }
        else if (view.getId() == R.id.btnDisconnect)
        {
            mqtt.disconnect();
            switchSendRealData.setEnabled(true);
            showMessage("MQTT test client is disconnected");
        }
        else if (view.getId() == R.id.btnTestConnection)
        {
            boolean isConnected = mqtt.isConnected();
            if (isConnected)
                showMessage("MQTT test client is connected");
            else
                showMessage("MQTT test client is NOT connected");
        }
        else if (view.getId() == R.id.btnSendDummyData)
        {
            if (mqtt.isConnected())
            {
                int iBatteryLevel = Integer.parseInt(txtBatteryLevel.getText().toString());
                int iBatteryHealth = Integer.parseInt(txtBatteryHealth.getText().toString());
                boolean publishSuccess = mqtt.publish(txtDeviceId.getText().toString(), txtModel.getText().toString(),
                        txtLatitude.getText().toString(), txtLongitude.getText().toString(),
                        iBatteryLevel, iBatteryHealth, txtOSVersion.getText().toString(),
                        txtPatchLevel.getText().toString(), txtReleaseVersion.getText().toString());
                if (publishSuccess)
                {
                    long dt = System.currentTimeMillis();
                    DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                    String formattedTime = formatter.format(dt);
                    showMessage("Message sent to cloud at " + formattedTime);
                }
                else
                    showMessage("Error publishing message: " + mqtt.getLastPublishError());
            }
            else
            {
                showMessage("Did not send message, MQTT is not connected");
            }
        }
    }

    private void sendRealData(boolean startSending)
    {
        if (startSending)
        {
            PeriodicWorkRequest.Builder sendRealDataBuilder =
                    new PeriodicWorkRequest.Builder(SendRealDataWorker.class,
                            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
            //  Pass the MQTT connection information to the worker.  The long-term worker is responsible for
            //  making and breaking the MQTT connection when needed.
            Data metaData = new Data.Builder().putInt(MQTT_SERVER_ENDPOINT, radioGroup.getCheckedRadioButtonId())
                    .putString(MQTT_DEVICE_ID, txtDeviceId.getText().toString())
                    .putString(MQTT_PROJECT_ID, txtProjectId.getText().toString())
                    .putString(MQTT_REGISTRY_ID, txtRegistryId.getText().toString())
                    .putString(MQTT_PRIVATE_KEY_NAME, txtPrivateKeyName.getText().toString())
                    .putString(MQTT_ALGORITHM, txtAlgorithm.getText().toString())
                    .putString(MQTT_CLOUD_REGION, txtCloudRegion.getText().toString())
                    .build();
            PeriodicWorkRequest sendRealDataWork = sendRealDataBuilder.
                    setInputData(metaData).build();
            WorkManager.getInstance().enqueue(sendRealDataWork);
            sendRealDataWorkId = sendRealDataWork.getId();
        }
        else
        {
            showMessage("Stopping real data from being sent");
            WorkManager.getInstance().cancelWorkById(sendRealDataWorkId);
        }
    }


    private void prePopulateConnectionInfo(int cloudServerType)
    {
        if (cloudServerType == R.id.radioGCP)
        {
            txtDeviceId.setText(GCP_DEVICE_ID);
            txtProjectId.setText(GCP_PROJECT_ID);
            txtRegistryId.setText(GCP_REGISTRY_ID);
            txtPrivateKeyName.setText(GCP_PRIVATE_KEY_NAME);
            txtAlgorithm.setText(GCP_ALGORITHM);
            txtCloudRegion.setText(GCP_CLOUD_REGION);
        }
    }

    private void prePopulateDummyData()
    {
        txtModel.setText(Build.MODEL);
        txtLatitude.setText("-36.8485");
        txtLongitude.setText("174.7633");
        txtBatteryLevel.setText("90");
        txtBatteryHealth.setText("80");
        txtOSVersion.setText(Build.VERSION.RELEASE);
        txtPatchLevel.setText(Build.VERSION.SECURITY_PATCH);
        txtReleaseVersion.setText(Build.DISPLAY);
    }

    private void updateUIEnableDummyData(boolean bEnable)
    {
        lblModel.setEnabled(bEnable);
        txtModel.setEnabled(bEnable);
        lblLatitude.setEnabled(bEnable);
        txtLatitude.setEnabled(bEnable);
        lblLongitude.setEnabled(bEnable);
        txtLongitude.setEnabled(bEnable);
        lblBatteryLevel.setEnabled(bEnable);
        txtBatteryLevel.setEnabled(bEnable);
        lblBatteryHealth.setEnabled(bEnable);
        txtBatteryHealth.setEnabled(bEnable);
        lblOSVersion.setEnabled(bEnable);
        txtOSVersion.setEnabled(bEnable);
        lblPatchLevel.setEnabled(bEnable);
        txtPatchLevel.setEnabled(bEnable);
        lblReleaseVersion.setEnabled(bEnable);
        txtReleaseVersion.setEnabled(bEnable);
        btnSendDummyData.setEnabled(bEnable);
    }

    private void configureUi(int menuId)
    {
        if (menuId == R.id.navigation_connect)
        {
            lblDeviceId.setVisibility(View.VISIBLE);
            txtDeviceId.setVisibility(View.VISIBLE);
            lblTestConnection.setVisibility(View.VISIBLE);
            btnConnect.setVisibility(View.VISIBLE);
            btnDisconnect.setVisibility(View.VISIBLE);
            btnTestConnection.setVisibility(View.VISIBLE);
            radioGroup.setVisibility(View.VISIBLE);
            lblProjectId.setVisibility(View.VISIBLE);
            txtProjectId.setVisibility(View.VISIBLE);
            lblRegistryId.setVisibility(View.VISIBLE);
            txtRegistryId.setVisibility(View.VISIBLE);
            lblPrivateKeyName.setVisibility(View.VISIBLE);
            txtPrivateKeyName.setVisibility(View.VISIBLE);
            lblAlgorithm.setVisibility(View.VISIBLE);
            txtAlgorithm.setVisibility(View.VISIBLE);
            lblCloudRegion.setVisibility(View.VISIBLE);
            txtCloudRegion.setVisibility(View.VISIBLE);
            switchSendDummyData.setVisibility(View.GONE);
            switchSendRealData.setVisibility(View.GONE);
            lblModel.setVisibility(View.GONE);
            txtModel.setVisibility(View.GONE);
            lblLatitude.setVisibility(View.GONE);
            txtLatitude.setVisibility(View.GONE);
            lblLongitude.setVisibility(View.GONE);
            txtLongitude.setVisibility(View.GONE);
            lblBatteryLevel.setVisibility(View.GONE);
            txtBatteryLevel.setVisibility(View.GONE);
            lblBatteryHealth.setVisibility(View.GONE);
            txtBatteryHealth.setVisibility(View.GONE);
            lblOSVersion.setVisibility(View.GONE);
            txtOSVersion.setVisibility(View.GONE);
            lblPatchLevel.setVisibility(View.GONE);
            txtPatchLevel.setVisibility(View.GONE);
            lblReleaseVersion.setVisibility(View.GONE);
            txtReleaseVersion.setVisibility(View.GONE);
            btnSendDummyData.setVisibility(View.GONE);
        }
        else if (menuId == R.id.navigation_dummy_data)
        {
            lblDeviceId.setVisibility(View.GONE);
            txtDeviceId.setVisibility(View.GONE);
            lblTestConnection.setVisibility(View.GONE);
            btnConnect.setVisibility(View.GONE);
            btnDisconnect.setVisibility(View.GONE);
            btnTestConnection.setVisibility(View.GONE);
            radioGroup.setVisibility(View.GONE);
            lblProjectId.setVisibility(View.GONE);
            txtProjectId.setVisibility(View.GONE);
            lblRegistryId.setVisibility(View.GONE);
            txtRegistryId.setVisibility(View.GONE);
            lblPrivateKeyName.setVisibility(View.GONE);
            txtPrivateKeyName.setVisibility(View.GONE);
            lblAlgorithm.setVisibility(View.GONE);
            txtAlgorithm.setVisibility(View.GONE);
            lblCloudRegion.setVisibility(View.GONE);
            txtCloudRegion.setVisibility(View.GONE);
            switchSendDummyData.setVisibility(View.VISIBLE);
            switchSendRealData.setVisibility(View.VISIBLE);
            lblModel.setVisibility(View.VISIBLE);
            txtModel.setVisibility(View.VISIBLE);
            lblLatitude.setVisibility(View.VISIBLE);
            txtLatitude.setVisibility(View.VISIBLE);
            lblLongitude.setVisibility(View.VISIBLE);
            txtLongitude.setVisibility(View.VISIBLE);
            lblBatteryLevel.setVisibility(View.VISIBLE);
            txtBatteryLevel.setVisibility(View.VISIBLE);
            lblBatteryHealth.setVisibility(View.VISIBLE);
            txtBatteryHealth.setVisibility(View.VISIBLE);
            lblOSVersion.setVisibility(View.VISIBLE);
            txtOSVersion.setVisibility(View.VISIBLE);
            lblPatchLevel.setVisibility(View.VISIBLE);
            txtPatchLevel.setVisibility(View.VISIBLE);
            lblReleaseVersion.setVisibility(View.VISIBLE);
            txtReleaseVersion.setVisibility(View.VISIBLE);
            btnSendDummyData.setVisibility(View.VISIBLE);
        }
    }

    public void showMessage(String message)
    {
        statusText.setText(message);
        Log.i(TAG, message);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if (intent.hasExtra("message"))
            {
                String message = intent.getStringExtra("message");
                showMessage(message);
            }
        }
    };
}
