package com.darryncampbell.enterprise.android.iot.client;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    public static String TAG = "ent-iot-client";
    private static String GCP_DEVICE_ID = "test-device";
    private static String GCP_PROJECT_ID = "ent-android-iot-server-gcp";
    private static String GCP_REGISTRY_ID = "ent-android-iot-registry-gcp";
    private static String GCP_PRIVATE_KEY_NAME = "rsa_private_pkcs8";
    private static String GCP_ALGORITHM = "RS256";
    private static String GCP_CLOUD_REGION = "europe-west1";
    private MQTTInterface mqtt = new MQTTInterface();
    private UUID sendRealDataWorkId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        configureUi(R.id.navigation_connect);
        prePopulateConnectionInfo(R.id.radioGCP);
        prePopulateDummyData();

        Switch switchDummyData = findViewById(R.id.switchSendDummyData);
        switchDummyData.setOnCheckedChangeListener(this);
        Switch switchSendRealData = findViewById(R.id.switchSendRealData);
        switchSendRealData.setOnCheckedChangeListener(this);

        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(this);
        Button btnTestConnection = findViewById(R.id.btnTestConnection);
        btnTestConnection.setOnClickListener(this);
        Button btnSendDummyData = findViewById(R.id.btnSendDummyData);
        btnSendDummyData.setOnClickListener(this);

        //  Request permission to read external storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            }
        }
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
            //resume tasks needing this permission
        }
        else if (requestCode == 0)
        {
            String errorMsg = "Read Storage permission is required for MQTTInterface Connection.  Location permission is required for Real location data";
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
                sendRealData(true);
            else
                sendRealData(false);
        }
    }

    @Override
    public void onClick(View view) {
        //  Button handlers
        if (view.getId() == R.id.btnConnect)
        {
            //  Connect to MQTTInterface server
            TextView txtDeviceId = findViewById(R.id.txtDeviceId);
            TextView txtProjectId = findViewById(R.id.txtProjectId);
            TextView txtRegistryId = findViewById(R.id.txtRegistryId);
            TextView txtPrivateKeyName = findViewById(R.id.txtPrivateKeyName);
            TextView txtAlgorithm = findViewById(R.id.txtAlgorithm);
            TextView txtCloudRegion = findViewById(R.id.txtCloudRegion);
            String deviceId = txtDeviceId.getText().toString();
            String projectId = txtProjectId.getText().toString();
            String cloudRegion = txtCloudRegion.getText().toString();
            String registryId = txtRegistryId.getText().toString();
            String algorithm = txtAlgorithm.getText().toString();
            String privateKeyFile = txtPrivateKeyName.getText().toString();
            Boolean connectSuccess =
                    mqtt.connectToGoogleCloudIot(deviceId, projectId, cloudRegion, registryId, algorithm, privateKeyFile);
            if (connectSuccess)
                showMessage("Connected to " + projectId);
            else
                showMessage("Connect Failed: " + mqtt.getLastConnectionError());
        }
        else if (view.getId() == R.id.btnDisconnect)
        {
            mqtt.disconnect();
            showMessage("MQTT client is disconnected");
        }
        else if (view.getId() == R.id.btnTestConnection)
        {
            boolean isConnected = mqtt.isConnected();
            if (isConnected)
                showMessage("MQTT client is connected");
            else
                showMessage("MQTT client is NOT connected");
        }
        else if (view.getId() == R.id.btnSendDummyData)
        {
            if (mqtt.isConnected())
            {
                TextView txtDeviceId = findViewById(R.id.txtDeviceId);
                TextView txtModel = findViewById(R.id.txtModel);
                TextView txtLatitude = findViewById(R.id.txtLatitude);
                TextView txtLongitude = findViewById(R.id.txtLongitude);
                TextView txtBatteryLevel = findViewById(R.id.txtBatteryLevel);
                TextView txtBatteryHealth = findViewById(R.id.txtBatteryHealth);
                TextView txtOSVersion = findViewById(R.id.txtOSVersion);
                TextView txtPatchLevel = findViewById(R.id.txtPatchLevel);
                TextView txtReleaseVersion = findViewById(R.id.txtReleaseVersion);
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
            if (!mqtt.isConnected())
                showMessage("Data will not be sent until MQTT is manually connected.");
            PeriodicWorkRequest.Builder sendRealDataBuilder =
                    new PeriodicWorkRequest.Builder(SendRealDataWorker.class,
                            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
            PeriodicWorkRequest sendRealDataWork = sendRealDataBuilder.build();
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
        TextView txtDeviceId = findViewById(R.id.txtDeviceId);
        TextView txtProjectId = findViewById(R.id.txtProjectId);
        TextView txtRegistryId = findViewById(R.id.txtRegistryId);
        TextView txtPrivateKeyName = findViewById(R.id.txtPrivateKeyName);
        TextView txtAlgorithm = findViewById(R.id.txtAlgorithm);
        TextView txtCloudRegion = findViewById(R.id.txtCloudRegion);
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
        TextView txtModel = findViewById(R.id.txtModel);
        TextView txtLatitude = findViewById(R.id.txtLatitude);
        TextView txtLongitude = findViewById(R.id.txtLongitude);
        TextView txtBatteryLevel = findViewById(R.id.txtBatteryLevel);
        TextView txtBatteryHealth = findViewById(R.id.txtBatteryHealth);
        TextView txtOSVersion = findViewById(R.id.txtOSVersion);
        TextView txtPatchLevel = findViewById(R.id.txtPatchLevel);
        TextView txtReleaseVersion = findViewById(R.id.txtReleaseVersion);
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
        TextView lblModel = findViewById(R.id.lblModel);
        TextView txtModel = findViewById(R.id.txtModel);
        TextView lblLatitude = findViewById(R.id.lblLatitude);
        TextView txtLatitude = findViewById(R.id.txtLatitude);
        TextView lblLongitude = findViewById(R.id.lblLongitude);
        TextView txtLongitude = findViewById(R.id.txtLongitude);
        TextView lblBatteryLevel = findViewById(R.id.lblBatteryLevel);
        TextView txtBatteryLevel = findViewById(R.id.txtBatteryLevel);
        TextView lblBatteryHealth = findViewById(R.id.lblBatteryHealth);
        TextView txtBatteryHealth = findViewById(R.id.txtBatteryHealth);
        TextView lblOSVersion = findViewById(R.id.lblOSVersion);
        TextView txtOSVersion = findViewById(R.id.txtOSVersion);
        TextView lblPatchLevel = findViewById(R.id.lblPatchLevel);
        TextView txtPatchLevel = findViewById(R.id.txtPatchLevel);
        TextView lblReleaseVersion = findViewById(R.id.lblReleaseVersion);
        TextView txtReleaseVersion = findViewById(R.id.txtReleaseVersion);
        Button btnSendDummyData = findViewById(R.id.btnSendDummyData);
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
        TextView lblDeviceId = findViewById(R.id.lblDeviceId);
        TextView txtDeviceId = findViewById(R.id.txtDeviceId);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);
        Button btnTestConnection = findViewById(R.id.btnTestConnection);
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        TextView lblProjectId = findViewById(R.id.lblProjectId);
        TextView txtProjectId = findViewById(R.id.txtProjectId);
        TextView lblRegistryId = findViewById(R.id.lblRegistryId);
        TextView txtRegistryId = findViewById(R.id.txtRegistryId);
        TextView lblPrivateKeyName = findViewById(R.id.lblPrivateKeyName);
        TextView txtPrivateKeyName = findViewById(R.id.txtPrivateKeyName);
        TextView lblAlgorithm = findViewById(R.id.lblAlgorithm);
        TextView txtAlgorithm = findViewById(R.id.txtAlgorithm);
        TextView lblCloudRegion = findViewById(R.id.lblCloudRegion);
        TextView txtCloudRegion = findViewById(R.id.txtCloudRegion);
        Switch switchSendDummyData = findViewById(R.id.switchSendDummyData);
        Switch switchSendRealData = findViewById(R.id.switchSendRealData);
        TextView lblModel = findViewById(R.id.lblModel);
        TextView txtModel = findViewById(R.id.txtModel);
        TextView lblLatitude = findViewById(R.id.lblLatitude);
        TextView txtLatitude = findViewById(R.id.txtLatitude);
        TextView lblLongitude = findViewById(R.id.lblLongitude);
        TextView txtLongitude = findViewById(R.id.txtLongitude);
        TextView lblBatteryLevel = findViewById(R.id.lblBatteryLevel);
        TextView txtBatteryLevel = findViewById(R.id.txtBatteryLevel);
        TextView lblBatteryHealth = findViewById(R.id.lblBatteryHealth);
        TextView txtBatteryHealth = findViewById(R.id.txtBatteryHealth);
        TextView lblOSVersion = findViewById(R.id.lblOSVersion);
        TextView txtOSVersion = findViewById(R.id.txtOSVersion);
        TextView lblPatchLevel = findViewById(R.id.lblPatchLevel);
        TextView txtPatchLevel = findViewById(R.id.txtPatchLevel);
        TextView lblReleaseVersion = findViewById(R.id.lblReleaseVersion);
        TextView txtReleaseVersion = findViewById(R.id.txtReleaseVersion);
        Button btnSendDummyData = findViewById(R.id.btnSendDummyData);

        if (menuId == R.id.navigation_connect)
        {
            lblDeviceId.setVisibility(View.VISIBLE);
            txtDeviceId.setVisibility(View.VISIBLE);
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
        TextView statusText = findViewById(R.id.txtStatus);
        statusText.setText(message);
        Log.i(TAG, message);
    }

}
