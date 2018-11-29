package com.darryncampbell.enterprise.android.iot.client;

import android.content.Intent;

public interface MQTTInterface {

    //  Keys associated with MQTT connection
    public String MQTT_SERVER_ENDPOINT = "MQTT_SERVER_ENDPOINT";
    public String MQTT_DEVICE_ID = "MQTT_DEVICE_ID";
    public String MQTT_PROJECT_ID = "MQTT_PROJECT_ID";
    public String MQTT_REGISTRY_ID = "MQTT_REGISTRY_ID";
    public String MQTT_PRIVATE_KEY_NAME = "MQTT_PRIVATE_KEY_NAME";
    public String MQTT_ALGORITHM = "MQTT_ALGORITHM";
    public String MQTT_CLOUD_REGION = "MQTT_CLOUD_REGION";

    abstract boolean initialise(Intent configuration);
    abstract boolean connect();
    abstract void disconnect();
    abstract boolean isConnected();
    abstract boolean publish(String deviceId, String model, String lat, String lng, int battLevel,
                             int battHealth, String osVersion, String patchLevel,
                             String releaseVersion);
    abstract String getLastConnectionError();
    abstract String getLastPublishError();
    abstract String getEndpointDescription();
}
