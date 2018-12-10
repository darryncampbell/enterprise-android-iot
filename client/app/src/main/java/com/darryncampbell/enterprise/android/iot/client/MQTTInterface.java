package com.darryncampbell.enterprise.android.iot.client;

import android.content.Context;
import android.os.Bundle;

public interface MQTTInterface {

    //  Keys associated with MQTT connection
    String MQTT_SERVER_ENDPOINT = "MQTT_SERVER_ENDPOINT";
    String MQTT_DEVICE_ID = "MQTT_DEVICE_ID";
    String MQTT_PROJECT_ID = "MQTT_PROJECT_ID";
    String MQTT_REGISTRY_ID = "MQTT_REGISTRY_ID";
    String MQTT_PRIVATE_KEY_NAME = "MQTT_PRIVATE_KEY_NAME";
    String MQTT_ALGORITHM = "MQTT_ALGORITHM";
    String MQTT_CLOUD_REGION = "MQTT_CLOUD_REGION";
    String MQTT_AWS_ENDPOINT = "MQTT_AWS_ENDPOINT";
    String MQTT_COGNITO_POOL_ID = "MQTT_COGNITO_POOL_ID";
    String MQTT_POLICY_NAME = "MQTT_POLICY_NAME";
    String MQTT_CONNECTION_STRING = "MQTT_CONNECTION_STRING";

    boolean initialise(Bundle configuration, Context context);
    boolean connect();
    boolean disconnect();
    boolean isConnected();
    boolean publish(String deviceId, String model, String lat, String lng, int battLevel,
                             int battHealth, String osVersion, String patchLevel,
                             String releaseVersion);
    String getLastConnectionError();
    String getLastPublishError();
    String getEndpointDescription();
    boolean getPublishInProgress();
}
