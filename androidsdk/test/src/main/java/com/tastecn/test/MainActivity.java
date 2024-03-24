package com.tastecn.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.netty.client.api.ConnectionManager;
import com.netty.client.api.NettyConnectionClient;
import com.netty.client.api.listener.DefaultNotificationListener;

public class MainActivity extends AppCompatActivity {
    NettyConnectionClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectionManager.addConnectionListener(new DefaultNotificationListener(getApplicationContext(), true));
        Button online = findViewById(R.id.btn_online);
        Button offline = findViewById(R.id.btn_offline);
        Button bound = findViewById(R.id.btn_bound);
        online.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client = new NettyConnectionClient(getApplicationContext(), "11111111111");
                client.setHeartbeatPeriod(60000);
                client.online();
            }
        });
        offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.offline();
            }
        });
        bound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.bindingAliasToDevice("TestAlias");
            }
        });

    }
}
