package com.example.carewheels.ros.Message;

import org.ros.rosjava_geometry.Vector3;

public class MagneticField {
    public Header header;
    public Vector3 magnetic_field;
    public float[] magnetic_field_covariance;

    public MagneticField() {
    }

    public MagneticField(Header header, Vector3 magnetic_field, float[] magnetic_field_covariance) {
        this.header = header;
        this.magnetic_field = magnetic_field;
        this.magnetic_field_covariance = new float[magnetic_field_covariance.length];
        System.arraycopy(magnetic_field_covariance, 0, this.magnetic_field_covariance, 0, magnetic_field_covariance.length);
    }

}
