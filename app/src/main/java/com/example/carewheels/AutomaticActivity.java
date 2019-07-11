package com.example.carewheels;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.carewheels.dsp.MovingAverage;
import com.example.carewheels.ros.Message.Joy;
import com.example.carewheels.ros.Topic;
import com.example.carewheels.rosbridge.ROSBridgeClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;


public class AutomaticActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "AA";
    private ImageView iv_lidar_automatic, iv_default_map, iv_position, iv_path;
    private Button btn_back_automatic, btn_subscribe_automatic, btn_start;
    private RadioGroup rdg_select_map;
    private RadioButton rdb_13_office, rdb_15_office;

    private ROSBridgeClient client;
    private Topic topic_lidar, topic_heading, topic_joy;
    private Joy joy;

    private static int count = 0;
    private String where = "13_office";
    private final int scale = 40;
    private final float ratio = 40 / 0.3f;                     // point per meter
    private final int[] geo_angles = {20, 110, 200, 290};
    private int[] rel_angles;
    private float[] cross_ranges;

    private MovingAverage maf_heading;
    private MovingAverage[] maf_ranges;

    private Bitmap map_default, map_lidar, map_position, map_path;
    private Canvas canvas_default, canvas_lidar, canvas_position, canvas_path;
    private Paint paint_red, paint_blue, paint_clear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_automatic);

        client = new ROSBridgeClient(URI.create("ws://13.125.210.133:9090"), this);
        client.connect();

        iv_lidar_automatic = findViewById(R.id.iv_lidar_automatic);
        iv_default_map = findViewById(R.id.iv_default_map);
        iv_position = findViewById(R.id.iv_position);
        iv_path = findViewById(R.id.iv_path);
        btn_subscribe_automatic = findViewById(R.id.btn_subscribe_automatic);
        btn_back_automatic = findViewById(R.id.btn_back_automatic);
        btn_start = findViewById(R.id.btn_start);

        rdg_select_map = findViewById(R.id.rdg_select_map);
        rdb_13_office = findViewById(R.id.rdb_13_office);
        rdb_15_office = findViewById(R.id.rdb_15_office);

        rdg_select_map.setOnCheckedChangeListener(this);
        btn_subscribe_automatic.setOnClickListener(this);
        btn_back_automatic.setOnClickListener(this);
        btn_start.setOnClickListener(this);

        topic_lidar = new Topic(client, "/laserScan_repub", "sensor_msgs/LaserScan");
        topic_heading = new Topic(client, "/heading_repub", "std_msgs/String");
        topic_joy = new Topic(client, "/joy_unity", "sensor_msgs/Joy");

        joy = new Joy();

        rel_angles = new int[4];
        cross_ranges = new float[4];

        maf_heading = new MovingAverage(15);
        maf_ranges = new MovingAverage[4];

        for (int i = 0; i < 4; i++) {
            maf_ranges[i] = new MovingAverage(10);
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initDraw();
            }
        }, 1000);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_subscribe_automatic:
                if (!(topic_lidar.is_subscribed && topic_heading.is_subscribed)) {
                    topic_lidar.subscribe();
                    topic_heading.subscribe();
                    topic_lidar.is_subscribed = true;
                    topic_heading.is_subscribed = true;
                    iv_lidar_automatic.setVisibility(View.VISIBLE);
                    iv_position.setVisibility(View.VISIBLE);
                    btn_subscribe_automatic.setText("구독취소");
                    initDraw();
                } else {
                    topic_lidar.unsubscribe();
                    topic_heading.unsubscribe();
                    topic_lidar.is_subscribed = false;
                    topic_heading.is_subscribed = false;
                    iv_lidar_automatic.setVisibility(View.INVISIBLE);
                    iv_position.setVisibility(View.INVISIBLE);
                    btn_subscribe_automatic.setText("구독하기");
                }
                break;

            case R.id.btn_start:
                joy.axes = new float[2];
                joy.axes[0] = (float) 0.5;
                joy.axes[1] = (float) 0;
                final Timer timer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (count < 10) {
                            topic_joy.publish(joy);
                            count++;
                        } else {
                            count = 0;
                            timer.cancel();
                        }
                    }
                };
                timer.schedule(timerTask, 0, 500);
                break;

            case R.id.btn_back_automatic:
                client.close();
                finish();
                break;
        }
    }

    public void initDraw() {
        map_default = Bitmap.createBitmap(iv_default_map.getWidth(), iv_default_map.getHeight(), Bitmap.Config.ARGB_8888);
        canvas_default = new Canvas(map_default);

        map_position = Bitmap.createBitmap(iv_position.getWidth(), iv_position.getHeight(), Bitmap.Config.ARGB_8888);
        canvas_position = new Canvas(map_position);

        map_path = Bitmap.createBitmap(iv_path.getWidth(), iv_path.getHeight(), Bitmap.Config.ARGB_8888);
        canvas_path = new Canvas(map_path);

        map_lidar = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
        canvas_lidar = new Canvas(map_lidar);

        paint_red = new Paint();
        paint_red.setColor(Color.RED);
        paint_red.setStrokeWidth(5f);

        paint_blue = new Paint();
        paint_blue.setColor(Color.BLUE);
        paint_blue.setStrokeWidth(10f);

        paint_clear = new Paint();
        Xfermode xmode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        paint_clear.setXfermode(xmode);

    }

    public void drawLidar(String message) {

        canvas_lidar.drawBitmap(map_lidar, 0, 0, paint_clear);

        JsonParser parser = new JsonParser();
        JsonElement msg = parser.parse(message).getAsJsonObject().get("msg");
        JsonArray ranges = msg.getAsJsonObject().get("ranges").getAsJsonArray();

        canvas_lidar.drawPoint(400, 400, paint_blue);

        for (int i = 0; i < 4; i++) {
            if (ranges.get(rel_angles[i]).isJsonNull()) {
            } else {
                maf_ranges[i].addData(ranges.get(rel_angles[i]).getAsFloat());
            }
        }

        for (int i = 0; i < ranges.size(); i++) {
            JsonElement range = ranges.get(i);
            if (range.isJsonNull())
                continue;

            if (i == rel_angles[0] || i == rel_angles[1] || i == rel_angles[2] || i == rel_angles[3]) {
                canvas_lidar.drawPoint((int) (400 + 50 * range.getAsFloat() * Math.cos(Math.toRadians(i))), (int) (400 - 50 * range.getAsFloat() * Math.sin(Math.toRadians(i))), paint_blue);
            } else {
                canvas_lidar.drawPoint((int) (400 + 50 * range.getAsFloat() * Math.cos(Math.toRadians(i))), (int) (400 - 50 * range.getAsFloat() * Math.sin(Math.toRadians(i))), paint_red);
            }
        }

        iv_lidar_automatic.setImageBitmap(map_lidar);
    }

    public void drawMap(String where) {
        canvas_default.drawBitmap(map_default, 0, 0, paint_clear);
        switch (where) {
            case "13_office":
                float[] pts_13 = {1, 2, 3, 2, 3, 2, 3, 1, 3, 1, 26, 1, 26, 1, 26, 2, 26, 2, 28, 2, 28, 2, 28, 25, 28, 25, 1, 25, 1, 25, 1, 2};
                float[] pts_scaled_13 = new float[pts_13.length];
                for (int i = 0; i < pts_13.length; i++) {
                    pts_scaled_13[i] = pts_13[i] * scale;
                }
                canvas_default.drawLines(pts_scaled_13, paint_red);
                break;

            case "15_office":
                float[] pts_15 = {1, 22, 2, 22, 2, 22, 2, 1, 2, 1, 13, 1, 13, 1, 13, 2, 13, 2, 14, 2, 14, 2, 14, 25, 14, 25, 1, 25, 1, 25, 1, 22};
                float[] pts_scaled_15 = new float[pts_15.length];
                for (int i = 0; i < pts_15.length; i++) {
                    pts_scaled_15[i] = pts_15[i] * scale;
                }
                canvas_default.drawLines(pts_scaled_15, paint_red);
                break;
        }
        iv_default_map.setImageBitmap(map_default);
    }

    public void updatePosition(String message) {

        JsonParser parser = new JsonParser();
        JsonElement msg = parser.parse(message).getAsJsonObject().get("msg");
        String heading = msg.getAsJsonObject().get("data").getAsString();

        maf_heading.addData(Float.valueOf(heading));

        for (int i = 0; i < 4; i++) {
            rel_angles[i] = (int) maf_heading.getFiltered() - geo_angles[i];
            if (rel_angles[i] < 0)
                rel_angles[i] += 360;
        }

        canvas_position.drawBitmap(map_position, 0, 0, paint_clear);

        canvas_position.drawCircle(getPosition(where)[0], getPosition(where)[1], 0.3f * ratio, paint_blue);
        canvas_position.drawText("raw : " + heading, 1200, 100, paint_red);
        canvas_position.drawText("filter : " + maf_heading.getFiltered(), 1200, 120, paint_blue);

        iv_position.setImageBitmap(map_position);
    }

    public float[] getPosition(String where) {

        float[] point = new float[2];

        switch (where) {
            case "13_office":
                point[0] = (maf_ranges[2].getFiltered() * 1120 + maf_ranges[0].getFiltered() * 40) / (maf_ranges[0].getFiltered() + maf_ranges[2].getFiltered());
                point[1] = 1000 - ratio * maf_ranges[1].getFiltered();
                break;
            case "15_office":
                point[0] = (maf_ranges[2].getFiltered() * 560 + maf_ranges[0].getFiltered() * 40) / (maf_ranges[0].getFiltered() + maf_ranges[2].getFiltered());
                point[1] = 1000 - ratio * maf_ranges[1].getFiltered();
                break;
        }
        return point;
    }

    public void clearPath() {
        canvas_path.drawBitmap(map_path, 0, 0, paint_clear);
        iv_path.setImageBitmap(map_path);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        clearPath();
        canvas_path.drawCircle(motionEvent.getX(), motionEvent.getY(), 10, paint_blue);
        iv_path.setImageBitmap(map_path);
        return false;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.rdb_13_office:
                where = "13_office";
                break;
            case R.id.rdb_15_office:
                where = "15_office";
                break;
        }
        clearPath();
        drawMap(where);
        iv_position.setOnTouchListener(this);
    }
}
