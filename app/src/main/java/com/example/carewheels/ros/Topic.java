package com.example.carewheels.ros;

import android.util.Log;

import com.example.carewheels.rosbridge.ROSBridgeClient;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class Topic {

    private static final String TAG = "TP";

    private ROSBridgeClient client;
    public Message msg;
    private String json;
    public boolean is_subscribed;

    public Topic(ROSBridgeClient client, String topic, String type) {
        this.client = client;
        this.is_subscribed = false;
        msg = new Message();
        msg.topic = topic;
        msg.type = type;
    }

    public void subscribe() {
        this.msg.op = "subscribe";
        Gson gson = new Gson();
        json = gson.toJson(msg);
        client.send(json);
    }

    public void unsubscribe() {
        this.msg.op = "unsubscribe";
        Gson gson = new Gson();
        json = gson.toJson(msg);
        client.send(json);
    }

    public void advertise() {
        this.msg.op = "advertise";
        Gson gson = new Gson();
        json = gson.toJson(msg);
        client.send(json);
    }

    public void unadvertise() {
        this.msg.op = "unadvertise";
        Gson gson = new Gson();
        json = gson.toJson(msg);
        client.send(json);
    }

    public void publish(Object object) {

        Gson gson = new Gson();

        String jsonMsg = gson.toJson(object);
        String fullMsg = "{\"op\": \"publish\", \"topic\": \"" + msg.topic + "\", \"type\": \"" + msg.type + "\", " + "\"msg\": " + jsonMsg + "}";

        client.send(fullMsg);
    }

    public String getJson() {
        return json;
    }

    class Message {
        public String op, topic, type;
    }

    public String getTopic() {
        return msg.topic;
    }

    public String getType() {
        return msg.type;
    }
}
