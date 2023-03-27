package com.vsb.kru13.osmzhttpserver;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;


import com.google.android.gms.location.FusedLocationProviderClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Telemetry implements SensorEventListener {
    JSONObject sensorData;
    private static Location location;

    public static void setLocation(Location loc){
        location = loc;
    }

    public static Location getLocation() {
        return location;
    }
    double altitude;
    double longitude;
    Context context;

    public Telemetry(Context context){
        this.context = context;
        this.sensorData = new JSONObject();
    }

    public void handleLocation(){
        if(location != null){
            altitude = location.getAltitude();
            longitude = location.getLongitude();
            Log.d("GPS", "Longtitude: " + longitude + " altitude: " + altitude);
        }
    }

    public JSONObject getSensorData(){
        return this.sensorData;
    }

    public void handleSensors(){
        final SensorManager sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        for(Sensor sensor : sensors){
            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    String senName = sensorEvent.sensor.getName();
                    float[] values = sensorEvent.values;

                    JSONArray arr = new JSONArray();
                    for(float iter : values){
                        try {
                            arr.put(iter);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        sensorData.put(senName, arr);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    sensorManager.unregisterListener(this);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            sensorData.put("Longitude", longitude);
            sensorData.put("Altitude", altitude);
            sensorData.put("Time", Calendar.getInstance().getTime());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
