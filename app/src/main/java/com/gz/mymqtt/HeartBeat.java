package com.gz.mymqtt;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * auto maintain the long priod of connect
 * detect the health of the connection
 * <p/>
 * Created by host on 2016/1/24.
 */
public class HeartBeat {
    public static final int HEARTBEAT_RATE = 90;

    public static final int CHECK_HEART_RATE = 15;

    public static final double BARE_RATE = 1.5;

    private MqttService mqttService;

    private Timer sendTimer;

    private Timer receiveTimer;

    private TimerTask heartBeatSender;

    private TimerTask heartBeatChecker;

    private ScheduledExecutorService executorService;

    private long lastHeatBeatTime;

    public HeartBeat(MqttService mqttService) {
        this.mqttService = mqttService;

        lastHeatBeatTime = System.currentTimeMillis();

        executorService = new ScheduledThreadPoolExecutor(2);
        heartBeatSender = new TimerTask() {
            @Override
            public void run() {
                String sendTime = "" + System.currentTimeMillis();
                HeartBeat.this.mqttService.sendMessage(
                        HeartBeat.this.mqttService.getHeartbeatTitle(), sendTime, 2);
                Log.i("heartbeatsend", "" + sendTime);
            }
        };
//        sendTimer = new Timer();
//        sendTimer.schedule(heartBeatSender, HEARTBEAT_RATE * 1000, HEARTBEAT_RATE * 1000);
        executorService.scheduleAtFixedRate(heartBeatSender, HEARTBEAT_RATE, HEARTBEAT_RATE, TimeUnit.SECONDS);

        heartBeatChecker = new TimerTask() {
            @Override
            public void run() {
                long gap = System.currentTimeMillis() - lastHeatBeatTime;
                Log.i("check", "" + gap);
                if (gap > BARE_RATE * HEARTBEAT_RATE * 1000) {
                    Log.i("heartbeat", "time over");
                    HeartBeat.this.mqttService.onStartCommand(null, 0, 0);
                    lastHeatBeatTime = System.currentTimeMillis();
                }
            }
        };
//        receiveTimer = new Timer();
//        receiveTimer.schedule(heartBeatChecker, 0, CHECK_HEART_RATE * 1000);
        executorService.scheduleAtFixedRate(heartBeatChecker, 0, CHECK_HEART_RATE, TimeUnit.SECONDS);
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

    public void initHeartBeatRecord() {
        lastHeatBeatTime = System.currentTimeMillis();
        Log.i("heartbeat", "init");
    }

    public void setOverTimeFlag() {
        lastHeatBeatTime = System.currentTimeMillis()
                - (long) BARE_RATE * HEARTBEAT_RATE * 2000;
    }

    public void stop() {
//        sendTimer.cancel();
//        sendTimer = null;
//        receiveTimer.cancel();
//        receiveTimer = null;
        executorService.shutdownNow();
    }
}
