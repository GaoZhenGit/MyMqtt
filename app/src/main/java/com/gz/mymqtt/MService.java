package com.gz.mymqtt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.widget.Toast;

import com.ibm.mqtt.MqttSimpleCallback;

/**
 * Created by host on 2016/1/26.
 */
public class MService extends Service implements MqttSimpleCallback {

    public static final int CHECK_RATE = 15;

    public static final int SEND_CHECK_TIME = 6;

    private static final double BARE_RATE = 1.5;

    private ReceiveServiceBinder receiveServiceBinder;

    private MqttHelper mqttHelper;

    private ToastHandler toastHandler;

    //the interface of receive
    private MCallback mCallback;

    private long lastHeatBeatTime;

    private int checkTime = SEND_CHECK_TIME;

    private int failTime = 0;


    /**
     * ***********override method****************
     */
    @Override
    public void onCreate() {
        receiveServiceBinder = new ReceiveServiceBinder();
        mqttHelper = new MqttHelper(this);
        toastHandler = new ToastHandler();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("mq2", "start2");
        int code;
        if (intent == null) {
            code = -1;
        } else {
            code = intent.getIntExtra("service action", -1);
        }
        switch (code) {
            case 1:
                if (checkTime >= SEND_CHECK_TIME) {
                    sendHeartBeat();
                    checkTime = 0;
                } else {
                    ++checkTime;
                }
                checkOverTime();
                break;
            case -1:
            default:
                if (mqttHelper == null)
                    mqttHelper = new MqttHelper(this);
                mqttHelper.connect(new MqttHelper.ActionListener() {
                    @Override
                    public void success() {
                        lastHeatBeatTime = System.currentTimeMillis();
                        mqttHelper.setMqttSimpleCallback(MService.this);
                    }

                    @Override
                    public void fail(Exception e) {
                        setOverTime();
                    }
                });
                Intent mIntent = new Intent(this, MReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, mIntent, 0);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + CHECK_RATE * 1000,
                        CHECK_RATE * 1000, pi);
                checkTime = SEND_CHECK_TIME;
        }
        return START_STICKY;
    }





    /*
    ***********oridinary method****************
     */

    private void sendHeartBeat() {
        String heartBeatTitle = mqttHelper.getHeartbeatTitle();
        String sendTime = System.currentTimeMillis() + "";
        mqttHelper.sendMessage(heartBeatTitle, sendTime, 2);
        Log.i("heartbeatsend", "" + sendTime);
    }

    private void checkOverTime() {
        long gap = System.currentTimeMillis() - lastHeatBeatTime;
        Log.i("check", "" + gap);
        if (gap > BARE_RATE * CHECK_RATE * SEND_CHECK_TIME * 1000) {
            Log.i("heartbeat", "time over");
            onStartCommand(null, 0, 0);
            lastHeatBeatTime = System.currentTimeMillis();
        }
    }

    private void receiveHeartBeat(String message) {
        try {
            long receive = Long.decode(message);
            Log.i("heartbeatreceive", message);
            if (receive - lastHeatBeatTime > 0) {
                lastHeatBeatTime = receive;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void setOverTime() {
        long gap = (long) (BARE_RATE * CHECK_RATE * SEND_CHECK_TIME * 1000);
        lastHeatBeatTime = System.currentTimeMillis() - gap;
    }

    public void setCallback(MCallback mCallback) {
        this.mCallback = mCallback;
    }

    public void subscribe(String topic, int qos) {
        mqttHelper.subScribe(topic, qos);
    }

    public void unScubscribe(String topic) {
        mqttHelper.unScubscribe(topic);
    }

    public void sendMessage(String topic, String msg, int qos) {
        mqttHelper.sendMessage(topic, msg, qos);
    }

    /*
    ***********implement method****************
     */
    @Override
    public IBinder onBind(Intent intent) {
        return receiveServiceBinder;
    }

    @Override
    public void connectionLost() throws Exception {
        Log.i("mqtt","connetion lost");
        setOverTime();
    }

    @Override
    public void publishArrived(String topicName, byte[] payload, int qos, boolean retained) throws Exception {
        String s = new String(payload);
        if (mqttHelper.getHeartbeatTitle().equals(topicName)) {
            receiveHeartBeat(s);
            return;
        }
        Log.i("message", topicName + ":" + s);
        //show message in toast
        toastHandler.show(s);
        //call the implement of interface
        if (mCallback != null) {
            mCallback.arrived(topicName, s);
        }

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(500);
    }


    /*
    ***********inner class*********************
     */
    public interface MCallback {
        void arrived(String topic, String msg);
    }


    public class ReceiveServiceBinder extends Binder {
        public MService getService() {
            return MService.this;
        }
    }

    private class ToastHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String s = msg.getData().getString("message");
            Toast.makeText(MService.this, s, Toast.LENGTH_LONG).show();
        }

        public void show(String s) {
            Bundle bundle = new Bundle();
            bundle.putString("message", s);
            Message msg = new Message();
            msg.setData(bundle);
            sendMessage(msg);
        }
    }
}
