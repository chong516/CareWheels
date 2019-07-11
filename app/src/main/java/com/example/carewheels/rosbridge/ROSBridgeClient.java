package com.example.carewheels.rosbridge;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.carewheels.AutomaticActivity;
import com.example.carewheels.RemoteActivity;
import com.example.carewheels.ros.Topic;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class ROSBridgeClient extends WebSocketClient {

    private static final String TAG = "RBC";
    private static int id_counter;

    private String connected_ac;

    private RemoteActivity remoteActivity;
    private AutomaticActivity automaticActivity;
//    private AutomaticActivity automaticActivity;

    public ROSBridgeClient(URI serverUri, Context context) {
        super(serverUri);
        id_counter = 0;
        connected_ac = context.getClass().getSimpleName();

        switch (connected_ac) {
            case "RemoteActivity":
                this.remoteActivity = (RemoteActivity) context;
                break;
            case "AutomaticActivity":
                this.automaticActivity = (AutomaticActivity) context;
                break;
        }

    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "onOpen: Client Opened!");
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onMessage(String message) {
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(message);
        String topic = element.getAsJsonObject().get("topic").getAsString();

        switch (connected_ac) {
            case "RemoteActivity":
                switch (topic) {
                    case "/compressed_repub":
                        remoteActivity.setImageView(message);
                        break;
                    case "/laserScan_repub":
                        remoteActivity.drawLidar(message);
                        break;
                }
                break;
            case "AutomaticActivity":
                switch (topic) {
                    case "/laserScan_repub":
                        automaticActivity.drawLidar(message);
                        break;
                    case "/heading_repub":
                        automaticActivity.updatePosition(message);
                        break;
                }
                break;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "onClose: Client Closed!");
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG, "onError: " + ex.getMessage());
    }

    public int idUpCounter() {
        id_counter += 1;
        return id_counter;
    }
}
