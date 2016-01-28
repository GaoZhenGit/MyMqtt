package com.gz.mymqtt;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity implements MService.MCallback {
    Button start;
    Button stop;
    Button subscribe;
    Button unSubscribe;
    Button send;
    EditText title;
    TextView display;

    MService receiveService;
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            receiveService = ((MService.ReceiveServiceBinder) service).getService();
            receiveService.setCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            receiveService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        subscribe = (Button) findViewById(R.id.subscribe);
        unSubscribe = (Button) findViewById(R.id.unSubscribe);
        send = (Button) findViewById(R.id.send);
        title = (EditText) findViewById(R.id.title);
        display = (TextView) findViewById(R.id.display);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MService.class);
                getApplicationContext().startService(intent);
                getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MService.class);
                try {
                    getApplicationContext().unbindService(serviceConnection);
                    getApplicationContext().stopService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (receiveService != null)
                    receiveService.subscribe(title.getText().toString(), 2);
            }
        });

        unSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (receiveService != null)
                    receiveService.unSubscribe(title.getText().toString());
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (receiveService != null)
                    receiveService.sendMessage("emergency", title.getText().toString(), 2);
            }
        });
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        if (receiveService != null) {
            try {
                getApplicationContext().unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public void arrived(final String topic, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder s = new StringBuilder(display.getText().toString());
                s.append("\n").append(topic).append(":").append(msg);
                display.setText(s.toString());
            }
        });
    }
}
