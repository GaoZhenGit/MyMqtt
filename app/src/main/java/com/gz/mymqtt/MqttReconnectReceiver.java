package com.gz.mymqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;



public class MqttReconnectReceiver extends BroadcastReceiver {
    public MqttReconnectReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MqttService.class);
        context.startService(i);
    }
}
