package com.gz.mymqtt;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

import java.util.ArrayList;
import java.util.List;


public class ReceiveService extends Service implements MqttSimpleCallback {


    private static MqttPersistence MQTT_PERSISTENCE = null;
    // We don't need to remember any state between the connections, so we use a
    // clean start.
    private static boolean MQTT_CLEAN_START = true;
    // heartbeat, pre second
    private static short MQTT_KEEP_ALIVE = 15;

    private static List<String> titles = new ArrayList<>();
    private ReceiveServiceBinder mBinder;
    private IMqttClient mqttClient;
    private ToastHandler toastHandler = new ToastHandler();
    private MCallback mCallback;


    public ReceiveService() {
        super();
        mBinder = new ReceiveServiceBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "start", Toast.LENGTH_SHORT).show();
        final String id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connect("139.129.18.117", 1883, id);
                    recoverSub();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return START_STICKY;
    }

    //this method should not run in ui thread
    public void connect(String brokerHostName, int port, String clientID)
            throws MqttException {
        // Create connection spec
        String mqttConnSpec = "tcp://" + brokerHostName + ":"
                + port;
        if (mqttClient == null) {
            mqttClient = MqttClient.createMqttClient(mqttConnSpec, MQTT_PERSISTENCE);
        } else {
            Log.i("mqtt", "reconnected");
        }
        mqttClient.connect(clientID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);
        mqttClient.registerSimpleHandler(this);
        Log.i("mqtt", "conneced!");
    }

    private void recoverSub() {
        String[] s = (String[]) titles.toArray(new String[titles.size()]);
        for (String st : s) {
            Log.i("resub", st);
        }
        try {
            mqttClient.subscribe(new String[]{"emergency"}, new int[]{2});
            mqttClient.subscribe(s, new int[titles.size()]);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void subscribe(String title, int qos) {
        if (titles.contains(title))
            return;
        final String[] t = new String[]{title};
        final int[] q = new int[]{qos};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.subscribe(t, q);
                    Log.i("mqtt", "sub " + t[0]);
                    titles.add(t[0]);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void unScubscribe(String title) {
        if (!titles.contains(title))
            return;
        final String[] t = new String[]{title};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.unsubscribe(t);
                    Log.i("mqtt", "unsub");
                    titles.remove(t[0]);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setCallback(MCallback mCallback) {
        this.mCallback = mCallback;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "destory", Toast.LENGTH_SHORT).show();
        try {
            mqttClient.disconnect();
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /**
     * this is the method of implements MqttSimpleCallback
     *
     * @throws Exception
     */
    @Override
    public void connectionLost() throws Exception {
        Log.e("mqtt", "connectloss" + mqttClient.isConnected());
        onStartCommand(null, 0, 0);
    }

    /**
     * the callback when message arrived
     *
     * @param topicName
     * @param payload
     * @param qos
     * @param retained
     * @throws Exception
     */
    @Override
    public void publishArrived(String topicName, byte[] payload, int qos, boolean retained) throws Exception {
        String s = new String(payload);
        Log.i("message", s);
        Log.i("thread", "" + (Looper.myLooper() == Looper.getMainLooper()));
        toastHandler.show(s);
        if (mCallback != null) {
            mCallback.arrived(topicName,s);
        }
    }


    //to show toast in ui thread
    private class ToastHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String s = msg.getData().getString("message");
            Toast.makeText(ReceiveService.this, s, Toast.LENGTH_LONG).show();
        }

        public void show(String s) {
            Bundle bundle = new Bundle();
            bundle.putString("message", s);
            Message msg = new Message();
            msg.setData(bundle);
            sendMessage(msg);
        }
    }

    public interface MCallback {
        void arrived(String topic, String msg);
    }


    public class ReceiveServiceBinder extends Binder {
        public ReceiveService getService() {
            return ReceiveService.this;
        }
    }


}
