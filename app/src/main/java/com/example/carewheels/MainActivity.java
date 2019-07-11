package com.example.carewheels;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_remote, btn_auto_driving, btn_seat_log;
    private TextView tv_status;

    private static final String TAG = "MA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        btn_remote = findViewById(R.id.btn_remote);
        btn_auto_driving = findViewById(R.id.btn_auto_driving);
        btn_seat_log = findViewById(R.id.btn_seat_log);

        btn_remote.setOnClickListener(this);
        btn_auto_driving.setOnClickListener(this);
        btn_seat_log.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_remote:
                Intent intent_remote = new Intent(this, RemoteActivity.class);
                startActivity(intent_remote);
                break;
            case R.id.btn_auto_driving:
                Intent intent_auto = new Intent(this, AutomaticActivity.class);
                startActivity(intent_auto);
                break;
            case R.id.btn_seat_log:
                break;
        }
    }
}
