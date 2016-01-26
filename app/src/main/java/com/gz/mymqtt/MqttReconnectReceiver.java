package com.gz.mymqtt;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class MqttReconnectReceiver extends BroadcastReceiver {
    public MqttReconnectReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("receive","onReceive");
        if (!isServiceRunning(context)) {
            Intent i = new Intent(context, MqttService.class);
            context.startService(i);
            Log.i("receive", "not run");
        }
//        setAlarm(context);
    }

    private boolean isServiceRunning(Context context) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MqttService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent localIntent = new Intent(context,MqttReconnectReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,localIntent,0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,10000,10000,pendingIntent);
    }
}
