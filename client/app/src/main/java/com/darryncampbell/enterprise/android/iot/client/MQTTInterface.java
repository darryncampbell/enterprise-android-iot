package com.darryncampbell.enterprise.android.iot.client;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import static com.darryncampbell.enterprise.android.iot.client.MainActivity.TAG;


public class MQTTInterface {
    private String mqttBridgeHostname = "mqtt.googleapis.com";
    private short mqttBridgePort = 8883;
    private String lastConnectionError = "no error";
    private String lastPublishError = "no error";
    private MqttClient client;

    //  Keys associated with MQTT connection
    public static String MQTT_SERVER_ENDPOINT = "MQTT_SERVER_ENDPOINT";
    public static String MQTT_DEVICE_ID = "MQTT_DEVICE_ID";
    public static String MQTT_PROJECT_ID = "MQTT_PROJECT_ID";
    public static String MQTT_REGISTRY_ID = "MQTT_REGISTRY_ID";
    public static String MQTT_PRIVATE_KEY_NAME = "MQTT_PRIVATE_KEY_NAME";
    public static String MQTT_ALGORITHM = "MQTT_ALGORITHM";
    public static String MQTT_CLOUD_REGION = "MQTT_CLOUD_REGION";

    // [START iot_mqtt_jwt]
    /** Create a Cloud IoT Core JWT for the given project id, signed with the given RSA key. */
    private static String createJwtRsa(String projectId, String privateKeyFile) throws Exception {
        DateTime now = new DateTime();
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(20).toDate())
                        .setAudience(projectId);

        String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        //  Note: On a TC57 this will read /storage/emulated/0/rsa_private_pkcs8
        byte[] keyBytes = readAllBytes(Environment.getExternalStorageDirectory() + "/" + privateKeyFile);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return jwtBuilder.signWith(SignatureAlgorithm.RS256, kf.generatePrivate(spec)).compact();
    }

    /** Create a Cloud IoT Core JWT for the given project id, signed with the given ES key. */
    private static String createJwtEs(String projectId, String privateKeyFile) throws Exception {
        DateTime now = new DateTime();
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(20).toDate())
                        .setAudience(projectId);

        String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        byte[] keyBytes = readAllBytes(Environment.getExternalStorageDirectory() + "/" + privateKeyFile);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");

        return jwtBuilder.signWith(SignatureAlgorithm.ES256, kf.generatePrivate(spec)).compact();
    }
    // [END iot_mqtt_jwt]

    // [START iot_mqtt_configcallback]
    static MqttCallback mCallback;

    /** Attaches the callback used when configuration changes occur. */
    public static void attachCallback(MqttClient client, String deviceId) throws MqttException {
        mCallback = new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Do nothing...
                //  todo
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                System.out.println("Payload : " + payload);
                // TODO: Insert your parsing / handling of the configuration message here.
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Do nothing;
            }
        };

        String configTopic = String.format("/devices/%s/config", deviceId);
        client.subscribe(configTopic, 1);

        client.setCallback(mCallback);
    }
    // [END iot_mqtt_configcallback]

    public String getLastConnectionError()
    {
        return lastConnectionError;
    }
    public String getLastPublishError() {return lastPublishError; }

    public boolean connectToGoogleCloudIot(String deviceId, String projectId, String cloudRegion,
                                         String registryId, String algorithm, String privateKeyFile)
    {
        try {
            if (isConnected())
                return true;
            // Build the connection string for Google's Cloud IoT Core MQTT server. Only SSL
            // connections are accepted. For server authentication, the JVM's root certificates
            // are used.
            final String mqttServerAddress =
                    String.format("ssl://%s:%s", mqttBridgeHostname, mqttBridgePort);

            // Create our MQTT client. The mqttClientId is a unique string that identifies this device. For
            // Google Cloud IoT Core, it must be in the format below.
            final String mqttClientId =
                    String.format(
                            "projects/%s/locations/%s/registries/%s/devices/%s",
                            projectId, cloudRegion, registryId, deviceId);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            // Note that the Google Cloud IoT Core only supports MQTT 3.1.1, and Paho requires that we
            // explictly set this. If you don't set MQTT version, the server will immediately close its
            // connection to your device.
            connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

            Properties sslProps = new Properties();
            sslProps.setProperty("com.ibm.ssl.protocol", "TLSv1.2");
            connectOptions.setSSLProperties(sslProps);

            // With Google Cloud IoT Core, the username field is ignored, however it must be set for the
            // Paho client library to send the password field. The password field is used to transmit a JWT
            // to authorize the device.
            connectOptions.setUserName("unused");

            DateTime iat = new DateTime();
            if (algorithm.equals("RS256")) {
                connectOptions.setPassword(
                        createJwtRsa(projectId, privateKeyFile).toCharArray());
            } else if (algorithm.equals("ES256")) {
                connectOptions.setPassword(
                        createJwtEs(projectId, privateKeyFile).toCharArray());
            } else {
                lastConnectionError = "Invalid algorithm " + algorithm + ". Should be one of 'RS256' or 'ES256'.";
                return false;
            }
            // [END iot_mqtt_configuremqtt]

            // [START iot_mqtt_publish]
            // Create a client, and connect to the Google MQTT bridge.
            client = new MqttClient(mqttServerAddress, mqttClientId, new MemoryPersistence());

            // Both connect and publish operations may fail. If they do, allow retries but with an
            // exponential backoff time period.
            long initialConnectIntervalMillis = 500L;
            long maxConnectIntervalMillis = 6000L;
            long maxConnectRetryTimeElapsedMillis = 2000L;
            float intervalMultiplier = 1.5f;

            long retryIntervalMs = initialConnectIntervalMillis;
            long totalRetryTimeMs = 0;

            while (!client.isConnected() && totalRetryTimeMs < maxConnectRetryTimeElapsedMillis) {
                try {
                    client.connect(connectOptions);
                } catch (MqttException e) {
                    int reason = e.getReasonCode();

                    // If the connection is lost or if the server cannot be connected, allow retries, but with
                    // exponential backoff.
                    System.out.println("An error occurred: " + e.toString());
                    lastConnectionError = e.getLocalizedMessage();
                    if (reason == MqttException.REASON_CODE_CONNECTION_LOST
                            || reason == MqttException.REASON_CODE_SERVER_CONNECT_ERROR) {
                        System.out.println("Retrying in " + retryIntervalMs / 1000.0 + " seconds.");
                        Thread.sleep(retryIntervalMs);
                        totalRetryTimeMs += retryIntervalMs;
                        retryIntervalMs *= intervalMultiplier;
                        if (retryIntervalMs > maxConnectIntervalMillis) {
                            retryIntervalMs = maxConnectIntervalMillis;
                        }
                    } else {
                        lastConnectionError = e.toString();
                        return false;
                    }
                }
            }
            attachCallback(client, deviceId);
        }
        catch (Exception e) {
            Log.e("IOT_Test", e.toString());
            lastConnectionError = e.toString();
            return false;
        }
        lastConnectionError = "no error";
        return true;
    }

    public boolean isConnected()
    {
        if (client == null)
            return false;
        else
            return client.isConnected();
    }

    public void disconnect()
    {
        if (client != null && isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                Log.e(TAG, "MQTT Exception during disconnect: " + e.toString());
            }
        }
    }

    public boolean publish(String deviceId, String model, String lat, String lng, int battLevel,
                        int battHealth, String osVersion, String patchLevel,
                        String releaseVersion) {
        long dt = System.currentTimeMillis();
        //  todo remove the 100 from the payload
        String payload =
                String.format(
                        "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", deviceId, Long.toString(dt), model,
                        lat, lng, Integer.toString(battLevel), Integer.toString(battHealth),
                        osVersion, patchLevel, releaseVersion, "100");
        String topic = String.format("/devices/%s/events", deviceId);

        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);

        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            lastPublishError = e.toString();
            return false;
        }
        return true;
    }

    private static byte[] readAllBytes(String filePath) throws Exception
    {
        File file = new File(filePath);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
        buf.read(bytes, 0, bytes.length);
        buf.close();
        return bytes;
    }
}
