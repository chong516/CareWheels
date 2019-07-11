package com.example.carewheels.dsp;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverage {

    private Queue<Float> window = new LinkedList<Float>();
    private float sum = 0;
    private int period;

    public MovingAverage(int period) {
        this.period = period;
    }

    public void addData(float data) {
        sum += data;
        window.add(data);
        if (window.size() > period) {
            sum -= window.remove();
        }
    }

    public float getFiltered() {
        return sum / period;
    }

}
