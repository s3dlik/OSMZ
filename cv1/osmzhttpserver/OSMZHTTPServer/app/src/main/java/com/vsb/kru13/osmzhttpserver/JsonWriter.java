package com.vsb.kru13.osmzhttpserver;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonWriter {
    JSONObject json;
    public JsonWriter(JSONObject json){
        this.json = json;
    }

    public JSONObject returnJson(){
        return json;
    }
    public JSONObject write(String name, float valX){
        try{
            if(this.json.has(name)){
                this.json.put(name, name + " x: " + valX);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        return this.json;
    }

    public JSONObject write(String name, float valX, float valY, float valZ){
        try{
            if(this.json.has(name)){
                this.json.put(name, name + " x: " + valX + " y: " + valY + " z: " + valZ);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        return this.json;
    }
}
