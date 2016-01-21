package com.gz.mymqtt;

import android.app.Activity;
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


public class MainActivity extends Activity implements ReceiveService.MCallback{
    Button start;
    Button stop;
    Button subscribe;
    Button unSubscribe;
    EditText title;
    TextView display;

    ReceiveService receiveService;
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            receiveService = (ReceiveService) ((ReceiveService.ReceiveServiceBinder)service).getService();
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
        unSubscribe=(Button) findViewById(R.id.unSubscribe);
        title = (EditText) findViewById(R.id.title);
        display = (TextView) findViewById(R.id.display);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),ReceiveService.class);
                getApplicationContext().startService(intent);
                getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),ReceiveService.class);
                getApplicationContext().unbindService(serviceConnection);
                getApplicationContext().stopService(intent);
            }
        });

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveService.subscribe(title.getText().toString(), 2);
            }
        });

        unSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveService.unScubscribe(title.getText().toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        if(receiveService!=null) {
            getApplicationContext().unbindService(serviceConnection);
        }
        super.onDestroy();
    }

    @Override
    public void arrived(final String topic,final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuffer s = new StringBuffer(display.getText().toString());
                s.append("\n"+topic+":"+msg);
                display.setText(s.toString());
            }
        });
    }
}
