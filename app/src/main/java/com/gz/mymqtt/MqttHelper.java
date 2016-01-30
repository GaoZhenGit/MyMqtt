package com.gz.mymqtt;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttSimpleCallback;

import java.util.List;

/**
 * Created by host on 2016/1/26.
 */
public class MqttHelper {

    private final static boolean MQTT_CLEAN_START = false;
    // heartbeat, pre second, now i dont use it because i make heartbeat myself
    private final static short MQTT_KEEP_ALIVE = 60 * 60 * 2;
    //mqtt host url
    private final static String MQTT_HOST = "139.129.18.117";
    //mqtt host port, default 1883
    private final static int MQTT_PORT = 1883;
    //emergency title, always subscribe
    private final static String EMERGENCY_TITLE = "emergency";
    //the title of only self sub, for heartbeat, will add the device id before
    private final static String HEARTBEAT_TITLE = "heartbeat";

    private IMqttClient mqttClient;

    private LocalTitles localTitles;

    private String deviceID;

    public MqttHelper(Context context) {
        localTitles = new LocalTitles(context);
        deviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public void connect(final ActionListener actionListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mqttConnSpec = "tcp://" + MQTT_HOST + ":" + MQTT_PORT;
                    if (mqttClient == null) {
//                        mqttClient = MqttClient.createMqttClient(mqttConnSpec, null);
                    } else {
                        Log.i("mqtt", "reconnected");
                    }
                    mqttClient = MqttClient.createMqttClient(mqttConnSpec, null);
                    mqttClient.connect(deviceID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);
                    Log.i("mqtt", "connected!");
                    reSub();
                    if (actionListener != null)
                        actionListener.success();
                } catch (MqttException e) {
                    Log.e("mqtt", "connect fail");
                    if (actionListener != null)
                        actionListener.fail(e);
                }
            }
        }).start();
    }

    public void connect() {
        connect(null);
    }

    private void reSub() throws MqttException {
        List<String> ls = localTitles.getTitles();
        String[] s = ls.toArray(new String[ls.size()]);
        mqttClient.subscribe(new String[]{EMERGENCY_TITLE, getHeartbeatTitle()}, new int[]{2, 2});
        int[] qos = new int[s.length];
        for (int i = 0; i < qos.length; i++) {
            qos[i] = 2;
        }
        mqttClient.subscribe(s, qos);
        for (String st : s) {
            Log.i("resub", st);
        }
    }

    public void subScribe(String topic, int qos) {
        subScribe(topic, qos, null);
    }

    public void subScribe(String topic, int qos, final ActionListener actionListener) {
        if (localTitles.getTitles().contains(topic))
            return;
        final String[] t = new String[]{topic};
        final int[] q = new int[]{qos};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.subscribe(t, q);
                    Log.i("mqtt", "sub " + t[0]);
                    localTitles.add(t[0]);
                    if (actionListener != null)
                        actionListener.success();
                } catch (MqttException e) {
                    e.printStackTrace();
                    if (actionListener != null)
                        actionListener.fail(e);
                }
            }
        }).start();
    }

    public void unSubscribe(String topic) {
        unSubscribe(topic, null);
    }

    public void unSubscribe(String topic, final ActionListener actionListener) {
        if (!localTitles.getTitles().contains(topic))
            return;
        final String[] t = new String[]{topic};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.unsubscribe(t);
                    Log.i("mqtt", "unsub");
                    localTitles.remove(t[0]);
                    if (actionListener != null)
                        actionListener.success();
                } catch (MqttException e) {
                    e.printStackTrace();
                    if (actionListener != null)
                        actionListener.fail(e);
                }
            }
        }).start();
    }

    public void sendMessage(String topic, String message, final int qos) {
        sendMessage(topic, message, qos, null);
    }

    public void sendMessage(String topic, String message, final int qos, final ActionListener actionListener) {
        if (TextUtils.isEmpty(topic) || TextUtils.isEmpty(message)) {
            return;
        }
        final String t = topic;
        final String m = message;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.publish(t, m.getBytes(), qos, false);
                    if (actionListener != null)
                        actionListener.success();
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.getStackTraceString(e);
                    if (actionListener != null)
                        actionListener.fail(e);
                }
            }
        }).start();
    }

    public void setMqttSimpleCallback(MqttSimpleCallback mqttSimpleCallback) {
        mqttClient.registerSimpleHandler(mqttSimpleCallback);
    }

    public String getHeartbeatTitle() {
        return HEARTBEAT_TITLE + deviceID;
    }

    public interface ActionListener {
        void success();

        void fail(Exception e);
    }
}
