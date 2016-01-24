package com.gz.mymqtt;

import java.util.Timer;
import java.util.TimerTask;

/**
 * auto maintain the long priod of connect
 * detect the health of the connection
 * <p/>
 * Created by host on 2016/1/24.
 */
public class HeartBeat {
    public static final int HEARTBEAT_RATE = 30;

    public static final int CHECK_HEART_RATE = 5;

    public static final double BARE_RATE = 1.5;

    private MqttService mqttService;

    private Timer sendTimer;

    private Timer receiveTimer;

    private TimerTask heartBeatSender;

    private TimerTask heartBeatReceiver;

    private long lastHeatBeatTime;

    public HeartBeat(MqttService mqttService) {
        this.mqttService = mqttService;
        heartBeatSender = new TimerTask() {
            @Override
            public void run() {
                String sendTime = "" + System.currentTimeMillis();
                HeartBeat.this.mqttService.sendMessage(
                        HeartBeat.this.mqttService.getHeartbeatTitle(), sendTime, 2);
                Log.i("heartbeatsend", "" + sendTime);
            }
        };
        sendTimer = new Timer();
        sendTimer.schedule(heartBeatSender, HEARTBEAT_RATE * 1000, HEARTBEAT_RATE * 1000);

        heartBeatReceiver = new TimerTask() {
            @Override
            public void run() {
                double gap = System.currentTimeMillis() - lastHeatBeatTime;
                Log.i("check", "" + gap);
                if (gap > BARE_RATE * HEARTBEAT_RATE * 1000) {
                    Log.i("heartbeat","time over");
                    HeartBeat.this.mqttService.onStartCommand(null, 0, 0);
                    lastHeatBeatTime = System.currentTimeMillis();
                }
            }
        };
        receiveTimer = new Timer();
        receiveTimer.schedule(heartBeatReceiver, 0, CHECK_HEART_RATE * 1000);

        lastHeatBeatTime = System.currentTimeMillis();
    }

    //renew the last record time from received heart beat, if it is
    public void receiveHeartBeat(String message) {
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

    public void stop() {
        sendTimer.cancel();
        sendTimer = null;
        receiveTimer.cancel();
        receiveTimer = null;
    }
}
