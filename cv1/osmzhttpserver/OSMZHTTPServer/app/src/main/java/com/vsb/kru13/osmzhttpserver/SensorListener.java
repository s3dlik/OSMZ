package com.vsb.kru13.osmzhttpserver;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.android.gms.common.api.Api;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorListener implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;
    JSONObject jsonObject;
    public SensorListener(SensorManager sensorManager, Sensor sensor) {
        this.sensorManager = sensorManager;
        this.sensor = sensor;
        this.jsonObject = new JSONObject();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if(this.jsonObject.has(sensor.getName())){
            Log.d("SensorListener", "Accuracy changed!");
        }
    }

    public void registerListener() {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }
}