package com.example.carewheels.ros.Message;

public class Joy {
    public Header header;
    public float[] axes;
    public int[] buttons;

    public Joy() {
    }

    public Joy(Header header, float[] axes, int[] buttons) {
        this.header = header;
        this.axes = new float[axes.length];
        System.arraycopy(axes,0, this.axes, 0, axes.length);
        this.buttons = new int[buttons.length];
        System.arraycopy(buttons,0, this.buttons, 0, buttons.length);
    }
}
