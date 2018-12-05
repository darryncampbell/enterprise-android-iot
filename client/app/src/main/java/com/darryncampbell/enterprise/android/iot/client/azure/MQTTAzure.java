package com.darryncampbell.enterprise.android.iot.client.azure;

//  Based on https://github.com/Azure-Samples/azure-iot-samples-java.  Full credit to the original
//  author

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.darryncampbell.enterprise.android.iot.client.MQTTInterface;
import com.darryncampbell.enterprise.android.iot.client.MainActivity;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static com.darryncampbell.enterprise.android.iot.client.UserConfig.AZURE_CRITICAL_BATTERY_LEVEL;

public class MQTTAzure implements MQTTInterface {
    private String connectionString = null;
    private String lastConnectionError = "no error";
    private String lastPublishError = "no error";
    private DeviceClient client;
    private boolean connected = false;
    private Context context;

    @Override
    public boolean initialise(Intent configuration, Context context) {
        this.context = context;
        connectionString = configuration.getStringExtra(MQTTInterface.MQTT_CONNECTION_STRING);
        return true;
    }

    @Override
    public boolean connect() {
        try {
            client = new DeviceClient(connectionString, IotHubClientProtocol.MQTT);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            lastConnectionError = e.getInput();
            return false;
        }
        try
        {
            client.open();
            connected = true;
            //  Sample project on which this class is based registers for callbacks here.  We don't need
            //  these in this project but for reference see https://github.com/Azure-Samples/azure-iot-samples-java/blob/master/iot-hub/Samples/device/AndroidSample/app/src/main/java/com/microsoft/azure/sdk/iot/samples/androidsample/MainActivity.java
            //  for how to use.
            // MessageCallback callback = new MessageCallback();
            //client.setMessageCallback(callback, null);
            //client.subscribeToDeviceMethod(new SampleDeviceMethodCallback(), getApplicationContext(), new DeviceMethodStatusCallBack(), null);
        }
        catch (IOException e)
        {
            lastConnectionError = e.getMessage();
            connected = false;
            try {
                client.closeNow();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean disconnect() {
        connected = false;
        try {
            client.closeNow();
        } catch (IOException e) {
            e.printStackTrace();
            lastConnectionError = e.getMessage();
            return false;
        }
        return true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean publish(String deviceId, String model, String lat, String lng, int battLevel, int battHealth, String osVersion, String patchLevel, String releaseVersion) {
        long dt = System.currentTimeMillis();
        DateFormat formatter = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss");
        String dateTime = formatter.format(dt);
        JSONObject msgPayload = new JSONObject();
        try {
            msgPayload.put("deviceId", deviceId);
            msgPayload.put("dateTime", dateTime);
            msgPayload.put("model", model);
            msgPayload.put("lat", lat);
            msgPayload.put("lng", lng);
            msgPayload.put("battLevel", Integer.toString(battLevel));
            msgPayload.put("battHealth", Integer.toString(battHealth));
            msgPayload.put("osVersion", osVersion);
            msgPayload.put("patchLevel", patchLevel);
            msgPayload.put("releaseVersion", releaseVersion);
        } catch (JSONException e) {
            e.printStackTrace();
            lastPublishError = e.getMessage();
            return false;
        }
        Message message = new Message(msgPayload.toString().getBytes());
        if (battLevel <= AZURE_CRITICAL_BATTERY_LEVEL)
            message.setProperty("batteryLevel", "critical");
        else
            message.setProperty("batteryLevel", "normal");
        message.setMessageId(java.util.UUID.randomUUID().toString());
        EventCallback eventCallback = new EventCallback();
        client.sendEventAsync(message, eventCallback, 1);
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
        return "Azure";
    }

    class EventCallback implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            if((status == IotHubStatusCode.OK) || (status == IotHubStatusCode.OK_EMPTY))
            {
                //  Publish success
                updateStatusOnMainActivity("Data successfully published to Azure");
            }
            else
            {
                //  Publish fail
                updateStatusOnMainActivity("Data publish to Azure has FAILED");
            }
        }
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
