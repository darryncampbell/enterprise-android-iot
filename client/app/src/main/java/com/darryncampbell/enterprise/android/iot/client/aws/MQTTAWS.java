//  Based on https://github.com/awslabs/aws-sdk-android-samples/tree/master/AndroidPubSub/src/com/amazonaws/demo/androidpubsub

package com.darryncampbell.enterprise.android.iot.client.aws;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.darryncampbell.enterprise.android.iot.client.MQTTInterface;
import com.darryncampbell.enterprise.android.iot.client.MainActivity;
import com.darryncampbell.enterprise.android.iot.client.UserConfig;

import java.security.KeyStore;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class MQTTAWS implements MQTTInterface {

    CognitoCachingCredentialsProvider credentialsProvider;
    String keystorePath;
    String keystoreName;
    String keystorePassword;
    KeyStore clientKeyStore = null;
    String certificateId;
    private boolean canConnect = false;
    private boolean connected = false;
    Context context = null;

    private AWSIotClient mIotAndroidClient;
    private AWSIotMqttManager mqttManager;
    private String lastConnectionError = "no error";
    private String lastPublishError = "no error";
    private String deviceId;
    private String awsEndpoint;
    private String cognitoPoolId;
    private String policyName;
    private Regions MY_REGION;
    public static String TAG = "ent-iot-client";

    @Override
    public boolean initialise(Intent configuration, Context context) {
        this.context = context;
        deviceId = configuration.getStringExtra(MQTTInterface.MQTT_DEVICE_ID);
        awsEndpoint = configuration.getStringExtra(MQTTInterface.MQTT_AWS_ENDPOINT);
        cognitoPoolId = configuration.getStringExtra(MQTTInterface.MQTT_COGNITO_POOL_ID);
        policyName = configuration.getStringExtra(MQTTInterface.MQTT_POLICY_NAME);
        String cloudRegionString = configuration.getStringExtra(MQTTInterface.MQTT_CLOUD_REGION);
        try
        {
            MY_REGION = Regions.fromName(cloudRegionString);
        }
        catch (IllegalArgumentException e)
        {
            lastConnectionError = "Invalid cloud region";
            return false;
        }
        if (deviceId == null || awsEndpoint == null || cognitoPoolId == null || policyName == null ||
                cloudRegionString == null)
        {
            lastConnectionError = "Invalid connection configuration";
            return false;
        }

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context, // context
                cognitoPoolId, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(deviceId, awsEndpoint);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = context.getFilesDir().getPath();
        keystoreName = UserConfig.AWS_KEYSTORE_NAME;
        keystorePassword = UserConfig.AWS_KEYSTORE_PASSWORD;
        certificateId = UserConfig.AWS_CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                    canConnect = true;
                } else {
                    Log.i(TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(policyName);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        canConnect = true;
                    } catch (Exception e) {
                        Log.e(TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                        lastConnectionError = e.getMessage();
                    }
                }
            }).start();
        }
        return true;
    }

    @Override
    public boolean connect() {
        if (!canConnect)
        {
            //  This could be a lot smarter but for now, just get the user to try to connect again.
            lastConnectionError = "Certificates have not been created, please try again";
            return false;
        }

        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(TAG, "Status = " + String.valueOf(status));

                    if (status == AWSIotMqttClientStatus.Connecting) {
                        Log.d(TAG, "Connecting...");
                        updateStatusOnMainActivity("Connecting to AWS Server");
                    } else if (status == AWSIotMqttClientStatus.Connected) {
                        connected = true;
                        Log.d(TAG, "Connected");
                        updateStatusOnMainActivity("Connected to AWS Server");
                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        if (throwable != null) {
                            Log.e(TAG, "Connection error.", throwable);
                            updateStatusOnMainActivity("Connection error with AWS Server");
                        }
                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        if (throwable != null) {
                            Log.e(TAG, "Connection error.", throwable);
                            updateStatusOnMainActivity("Connection error with  AWS Server");
                        }
                        Log.d(TAG, "Disconnected");
                        //updateStatusOnMainActivity("Disconnected from AWS Server");
                    } else {
                        Log.d(TAG, "Disconnected");
                        //updateStatusOnMainActivity("Disconnected from AWS Server");
                    }
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Connection error.", e);
        }
        return true;
    }

    @Override
    public boolean disconnect() {
        try {
            mqttManager.disconnect();
            connected = false;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Disconnect error.", e);
            lastConnectionError = e.getMessage();
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean publish(String deviceId, String model, String lat, String lng, int battLevel, int battHealth, String osVersion, String patchLevel, String releaseVersion) {
        final String topic = UserConfig.AWS_TOPIC;
        long dt = System.currentTimeMillis();
        DateFormat formatter = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss");
        String dateTime = formatter.format(dt);
        final String message = "{\n" +
                "\"deviceId\": \"" + deviceId + "\",\n" +
                "\"dateTime\": \"" + dateTime + "\",\n" +
                "\"model\": \"" + model + "\",\n" +
                "\"lat\": \"" + lat + "\",\n" +
                "\"lng\": \"" + lng + "\",\n" +
                "\"battLevel\": \"" + battLevel + "\",\n" +
                "\"battHealth\": \"" + battHealth + "\",\n" +
                "\"osVersion\": \"" + osVersion + "\",\n" +
                "\"patchLevel\": \"" + patchLevel + "\",\n" +
                "\"releaseVersion\": \"" + releaseVersion + "\"\n" +
                "}\n";
        try
        {
            mqttManager.publishString(message, topic, AWSIotMqttQos.QOS0);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Publish error." + e.getMessage());
            updateStatusOnMainActivity("Publish error." + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public String getLastConnectionError() {
        return lastConnectionError;
    }

    @Override
    public String getLastPublishError() {
        return lastPublishError;
    }

    @Override
    public String getEndpointDescription() {
        return "AWS";
    }

    @Override
    public boolean getPublishInProgress()
    {
        //  AWS publish is synchronous so just return false;
        return false;
    }

    private void updateStatusOnMainActivity(String message)
    {
        //  If the UI is available and listening, update it with the latest status
        if (context != null)
        {
            Intent intent = new Intent(MainActivity.LOCAL_BROADCAST_MESSAGE);
            intent.putExtra("message", message);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

}
