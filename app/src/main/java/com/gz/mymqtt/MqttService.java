package com.gz.mymqtt;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.widget.Toast;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

import java.util.List;


public class MqttService extends Service implements MqttSimpleCallback {
    /**
     * the tech of connect and reconnect
     * 1.when the service is called start,it will try the first connect in onStartCommand()
     * 2.if the start fail because of MqttException, then wil call reConnectDelay()
     * 3.when the connection break detected and connectionLost() is call back, then call reConnectDelay()
     */


    private static MqttPersistence MQTT_PERSISTENCE = null;
    // We don't need to remember any state between the connections, so we use a
    // clean start.
    private final static boolean MQTT_CLEAN_START = true;
    // heartbeat, pre second
    private final static short MQTT_KEEP_ALIVE = 15;
    //mqtt host url
    private final static String MQTT_HOST = "139.129.18.117";
    //mqtt host port, default 1883
    private final static int MQTT_PORT = 1883;
    //emergency title, always subscribe
    private final static String EMERGENCY_TITLE = "emergency";
    //the post of reconnect pre second
    private final static int RECONNECT_DELAY = 15;

    //store and read subscribed title list in sharepreference
    private LocalTitles localTitles;
    //the binder for activity subscribe or unsubscribe
    private ReceiveServiceBinder mBinder;
    //the mqtt client, core of this service
    private IMqttClient mqttClient;
    //use for show toast in ui thread
    private ToastHandler toastHandler;
    //the interface of receive
    private MCallback mCallback;

//    private MqttReconnectReceiver mqttReconnectReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new ReceiveServiceBinder();
        toastHandler = new ToastHandler();
        localTitles = new LocalTitles(getApplication());
//        mqttReconnectReceiver = new MqttReconnectReceiver();
//
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("android.intent.action.TIME_TICK");
//        mqttReconnectReceiver = new MqttReconnectReceiver();
//        registerReceiver(mqttReconnectReceiver, intentFilter);
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

        //start a new thread of network connection,
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connect(MQTT_HOST, MQTT_PORT, id);
                    recoverSub();
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.e("mqtt","connect fail");
                    //renconnect if fail
                    reConnectDelay();
                }
            }
        }).start();
        return START_STICKY;
    }


    /**
     * this method should not run in ui thread
     *
     * @param brokerHostName
     * @param port
     * @param clientID       the divice id
     * @throws MqttException
     */
    public void connect(String brokerHostName, int port, String clientID)
            throws MqttException {
        // Create connection spec
        String mqttConnSpec = "tcp://" + brokerHostName + ":" + port;
        if (mqttClient == null) {
            mqttClient = MqttClient.createMqttClient(mqttConnSpec, MQTT_PERSISTENCE);
        } else {
            Log.i("mqtt", "reconnected");
        }
        mqttClient.connect(clientID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);
        mqttClient.registerSimpleHandler(this);
        Log.i("mqtt", "conneced!");
    }

    //in case of losing connection, this method help to reconnect of a delay
    private void reConnectDelay() {
        Log.i("mqtt", "delay");
        Looper.prepare();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                onStartCommand(null, 0, 0);
            }
        }, RECONNECT_DELAY * 1000);
        Looper.loop();
    }

    //after reconnecting, all the title will lost, call this to subscribe the title store
    private void recoverSub() {
        List<String> ls = localTitles.getTitles();
        String[] s = ls.toArray(new String[ls.size()]);
        for (String st : s) {
            Log.i("resub", st);
        }
        try {
            mqttClient.subscribe(new String[]{EMERGENCY_TITLE}, new int[]{2});
            int[] qos = new int[s.length];
            for (int i = 0; i < qos.length; i++) {
                qos[i] = 2;
            }
            mqttClient.subscribe(s, qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void subscribe(String title, int qos) {
        if (localTitles.getTitles().contains(title))
            return;
        final String[] t = new String[]{title};
        final int[] q = new int[]{qos};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.subscribe(t, q);
                    Log.i("mqtt", "sub " + t[0]);
                    localTitles.add(t[0]);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void unScubscribe(String title) {
        if (!localTitles.getTitles().contains(title))
            return;
        final String[] t = new String[]{title};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttClient.unsubscribe(t);
                    Log.i("mqtt", "unsub");
                    localTitles.remove(t[0]);
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
//        unregisterReceiver(mqttReconnectReceiver);
        super.onDestroy();
    }

    /**
     * this is the method of implements MqttSimpleCallback
     * deteted part of connect lost, so call reConnectDelay()
     *
     * @throws Exception
     */
    @Override
    public void connectionLost() throws Exception {
        Log.e("mqtt", "connectloss" + mqttClient.isConnected());
        reConnectDelay();
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
        Log.i("message", topicName+":"+s);
        //show message in toast
        toastHandler.show(s);
        //call the implement of interface
        if (mCallback != null) {
            mCallback.arrived(topicName, s);
        }
    }


    //to show toast in ui thread
    private class ToastHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String s = msg.getData().getString("message");
            Toast.makeText(MqttService.this, s, Toast.LENGTH_LONG).show();
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
        public MqttService getService() {
            return MqttService.this;
        }
    }


}
