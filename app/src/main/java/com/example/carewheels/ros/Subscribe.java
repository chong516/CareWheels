package com.example.carewheels.ros;

public class Subscribe {
    public String topic;
    public String type;

    public Subscribe() {}

    public Subscribe(String topic, String type) {
        this.topic = topic;
        this.type = type;
    }
}
