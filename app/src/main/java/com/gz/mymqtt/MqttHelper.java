package com.gz.mymqtt;

import android.content.Context;
import android.provider.Settings;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.List;

/**
 * Created by host on 2016/1/31.
 */
public class MqttHelper implements MqttCallback {

    public static final String HOST = "139.129.18.117";

    public static final int PORT = 1883;

    private static final String URL = "tcp://" + HOST + ":" + PORT;

    public static String DEVICE_ID;

    //emergency title, always subscribe
    private final static String EMERGENCY_TITLE = "emergency";
    //the title of only self sub, for heartbeat, will add the device id before
    private final static String HEARTBEAT_TITLE = "heartbeat";

    private MqttClient mqttClient;

    private LocalTitles localTitles;

    public MqttHelper(Context context) {
        localTitles = new LocalTitles(context);
        DEVICE_ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public void connect(final ActionListener actionListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MemoryPersistence memoryPersistence = new MemoryPersistence();
                    if (mqttClient == null) {
                        mqttClient = new MqttClient(URL, DEVICE_ID, memoryPersistence);
                    }
                    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                    mqttConnectOptions.setCleanSession(false);
                    mqttConnectOptions.setKeepAliveInterval(60 * 60);
                    mqttClient.connect(mqttConnectOptions);
                    android.util.Log.i("paho", "connect");
                    reSub();
                    if (actionListener != null)
                        actionListener.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("paho", "connect fail");
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
        mqttClient.subscribe(EMERGENCY_TITLE, 2);
        mqttClient.subscribe(getHeartbeatTitle(), 2);
//        int[] qos = new int[s.length];
//        for (int i = 0; i < qos.length; i++) {
//            qos[i] = 2;
//        }
//        mqttClient.subscribe(s, qos);
        for (String st : s) {
            mqttClient.subscribe(st, 2);
            Log.i("resub", st);
        }
    }

    public void subscribe(final String topic, final int qos, final ActionListener actionListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.subscribe(topic, qos);
                    Log.i("mqtt", "sub " + topic);
                    localTitles.add(topic);
                    if (actionListener != null)
                        actionListener.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (actionListener != null)
                        actionListener.fail(e);
                }
            }
        }).start();
    }

    public void subscribe(String topic, int qos) {
        subscribe(topic, qos, null);
    }

    public void unSubscribe(final String topic, final ActionListener actionListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.unsubscribe(topic);
                    Log.i("mqtt", "unsub " + topic);
                    if (actionListener != null)
                        actionListener.success();
                } catch (Exception e) {
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

    public void sendMessage(final String topic, final String message,
                            final int qos, final ActionListener actionListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                    mqttMessage.setQos(qos);

                    mqttClient.publish(topic, mqttMessage);
                    if (actionListener != null)
                        actionListener.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (actionListener != null)
                        actionListener.fail(e);
                }

            }
        }).start();
    }

    public void sendMessage(String topic, String message, int qos) {
        sendMessage(topic, message, qos, null);
    }

    public void setMqttCallback(MqttCallback mqttCallback) {
        mqttClient.setCallback(mqttCallback);
    }

    public String getHeartbeatTitle() {
        return HEARTBEAT_TITLE + DEVICE_ID;
    }


    @Override
    public void connectionLost(Throwable throwable) {
        Log.e("paho", "connection lost");
        throwable.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.i("message", topic + ":" + message.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public interface ActionListener {
        void success();

        void fail(Exception e);
    }
}
