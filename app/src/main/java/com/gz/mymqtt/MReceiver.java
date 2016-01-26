package com.gz.mymqtt;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MReceiver extends BroadcastReceiver {
    public MReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("receive","onReceive");
        Intent localIntent = new Intent(context,MService.class);
        localIntent.putExtra("service action", 1);
        context.startService(localIntent);
    }

    private boolean isServiceRunning(Context context) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
