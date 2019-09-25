package com.test.rxjava;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        String deviceId = "";
//
//        try {
//            deviceId = getLocalMac(this).replace(":", "");
//            deviceId = getAndroidId(this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//
//        Toast.makeText(this,deviceId+"",Toast.LENGTH_SHORT).show();
//        Log.e(TAG,deviceId+"");

        Button btnCreate = findViewById(R.id.btn_create);
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,CreateActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_schedulers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SchedulerActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_transform).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,TransformActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_back_pressure).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,BackPressureActivity.class);
                startActivity(intent);
            }
        });
    }

    private static String getLocalMac(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiInfo info = wifi.getConnectionInfo();
            return info.getMacAddress();
        }
        return "";
    }


    private static String getAndroidId(Context context) {
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }
}
