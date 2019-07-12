package com.example.carewheels;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.carewheels.ros.Message.Joy;
import com.example.carewheels.ros.Topic;
import com.example.carewheels.rosbridge.ROSBridgeClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.net.URI;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class RemoteActivity extends AppCompatActivity implements View.OnClickListener, JoystickView.OnMoveListener {

    private Button btn_back_remote, btn_subscribe_lidar, btn_subscribe_camera;
    private TextView tv_camera, tv_lidar;
    private ImageView iv_front_camera, iv_lidar;
    private JoystickView jsv_remote;

    private ROSBridgeClient client;
    private Topic topic_compressed, topic_lidar, topic_joy;
    private Joy joy;

    private static final String TAG = "RA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_remote);

        btn_back_remote = findViewById(R.id.btn_back_remote);
        btn_subscribe_lidar = findViewById(R.id.btn_subscribe_lidar);
        btn_subscribe_camera = findViewById(R.id.btn_subscribe_camera);

        btn_back_remote.setOnClickListener(this);
        btn_subscribe_lidar.setOnClickListener(this);
        btn_subscribe_camera.setOnClickListener(this);

        tv_camera = findViewById(R.id.tv_camera);
        tv_lidar = findViewById(R.id.tv_lidar);
        iv_front_camera = findViewById(R.id.iv_front_camera);
        iv_lidar = findViewById(R.id.iv_lidar);
        jsv_remote = findViewById(R.id.jsv_remote);
        jsv_remote.setOnMoveListener(this);

        init();
    }

    private void init() {
        client = new ROSBridgeClient(URI.create("ws://13.125.210.133:9090"), this);
        client.connect();

        topic_compressed = new Topic(client, "/compressed_repub", "sensor_msgs/CompressedImage");
        topic_lidar = new Topic(client, "/laserScan_repub", "sensor_msgs/LaserScan");
        topic_joy = new Topic(client, "/joy_unity", "sensor_msgs/Joy");

        joy = new Joy();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back_remote:
                client.close();
                finish();
                break;

            case R.id.btn_subscribe_lidar:
                if (!topic_lidar.is_subscribed) {
                    topic_lidar.subscribe();
                    topic_joy.advertise();
                    topic_lidar.is_subscribed = true;
                    iv_lidar.setVisibility(View.VISIBLE);
                    tv_lidar.setVisibility(View.INVISIBLE);
                    btn_subscribe_lidar.setText("LIDAR\n구독취소");
                } else {
                    topic_lidar.unsubscribe();
                    topic_joy.unadvertise();
                    topic_lidar.is_subscribed = false;
                    iv_lidar.setVisibility(View.INVISIBLE);
                    tv_lidar.setVisibility(View.VISIBLE);
                    btn_subscribe_lidar.setText("LIDAR\n구독하기");
                }
                break;

            case R.id.btn_subscribe_camera:
                if (!topic_compressed.is_subscribed) {
                    topic_compressed.subscribe();
                    topic_compressed.is_subscribed = true;
                    iv_front_camera.setVisibility(View.VISIBLE);
                    tv_camera.setVisibility(View.INVISIBLE);
                    btn_subscribe_camera.setText("CAMERA\n구독취소");
                } else {
                    topic_compressed.unsubscribe();
                    topic_compressed.is_subscribed = false;
                    iv_front_camera.setVisibility(View.INVISIBLE);
                    tv_camera.setVisibility(View.VISIBLE);
                    btn_subscribe_camera.setText("CAMERA\n구독하기");
                }
                break;
        }
    }

    public void setImageView(String message) {
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(message);
        String imageString = element.getAsJsonObject().get("msg").getAsJsonObject().get("data").getAsString();
        byte[] imageByteArray = Base64.decode(imageString, Base64.DEFAULT);

        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(imageByteArray);

        byte[] data = buffer.array();
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, buffer.arrayOffset(), buffer.readableBytes());

//        Bitmap bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);

        iv_front_camera.setImageBitmap(bitmap);
    }

    public void drawLidar(String message) {
        JsonParser parser = new JsonParser();
        JsonElement msg = parser.parse(message).getAsJsonObject().get("msg");
        JsonArray ranges = msg.getAsJsonObject().get("ranges").getAsJsonArray();
        float angle_min = msg.getAsJsonObject().get("angle_min").getAsFloat();
        float angle_max = msg.getAsJsonObject().get("angle_max").getAsFloat();
        float angle_increment = msg.getAsJsonObject().get("angle_increment").getAsFloat();

//        Log.d(TAG, "drawLidar: " + angle_min + " / " + angle_max + " / " + angle_increment + " / " + ranges.size());

        Bitmap bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5f);

        Paint paint_ = new Paint();
        paint_.setColor(Color.BLUE);
        paint_.setStrokeWidth(10f);

        canvas.drawPoint(400, 400, paint_);

        for (int i = 0; i < ranges.size(); i++) {
            JsonElement range = ranges.get(i);
            if (range.isJsonNull())
                continue;

            if (i == 0) {
                canvas.drawPoint((int) (400 + 50 * range.getAsFloat() * Math.cos(Math.toRadians(i))), (int) (400 - 50 * range.getAsFloat() * Math.sin(Math.toRadians(i))), paint_);
            } else {
                canvas.drawPoint((int) (400 + 50 * range.getAsFloat() * Math.cos(Math.toRadians(i))), (int) (400 - 50 * range.getAsFloat() * Math.sin(Math.toRadians(i))), paint);
            }
        }

        iv_lidar.setImageBitmap(bitmap);
    }

    @Override
    public void onMove(int angle, int strength) {
        Log.d(TAG, "onMove: " + angle + " / " + strength);
        joy.axes = new float[2];
        joy.axes[0] = (float) (strength * Math.cos(Math.toRadians(angle))) / 100;
        joy.axes[1] = (float) (strength * Math.sin(Math.toRadians(angle))) / 100;

        try {
            topic_joy.publish(joy);
        } catch (WebsocketNotConnectedException e) {
        }
    }
}
