package com.vsb.kru13.osmzhttpserver;

import android.content.Context;
import android.hardware.Sensor;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.gms.location.FusedLocationProviderClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientThread extends Thread{

//    final BufferedReader in;
//    final BufferedWriter out;
//    final OutputStream o;
    Socket s;
    Semaphore available;
    Handler handler;
    PrintWriter access;
    PrintWriter errors;
    Location location;
    Context context;
    public ClientThread(Context context, Socket s, Semaphore available, Handler handler, PrintWriter access, PrintWriter errors, Location location){
//        this.o = o;
//        this.out = out;
//        this.in = in;
        this.context = context;
        this.s = s;
        this.available = available;
        this.handler = handler;
        this.access = access;
        this.errors = errors;
        this.location = location;
    }

    public ClientThread(Context context, Socket s, Semaphore available, Handler handler, PrintWriter access, PrintWriter errors){
//        this.o = o;
//        this.out = out;
//        this.in = in;
        this.context = context;
        this.s = s;
        this.available = available;
        this.handler = handler;
        this.access = access;
        this.errors = errors;
    }

    public void run(){
        Pattern pattern_file = Pattern.compile("[a-zA-Z]+.html|png|jpe?g");
        boolean lineBool = true;
        String path = "";
        String tmp = null;
        boolean telemetry = false;
        boolean isCmd = false;
        try{

            Log.d("CLIENTTHREAD", "Semaphore remaining permits: " + this.available.availablePermits());
            //this.access.write("Semaphore remaining permits: "+ this.available.availablePermits());

            this.access.println("Semaphore remaining permits: "+ this.available.availablePermits());
            this.access.flush();

            OutputStream o = this.s.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(this.s.getInputStream()));
            String firstline = "";
            String detailedInfo = "";
            while((tmp = in.readLine()) != null){
                if(lineBool){
                    String[] requests = tmp.split(" ");
                    firstline = tmp;
                    path = requests[1];
                    Log.d("SERVER_REQUEST", requests[1]);
                    lineBool = false;
                }
                if(tmp.startsWith("User-Agent")){
                    detailedInfo = tmp;
                }
                Log.d("SERVER", tmp);
                if(tmp.isEmpty())
                    break;
            }
            Bundle bundle = new Bundle();
            Message msg = this.handler.obtainMessage();

            bundle.putString("message", messageCreation(firstline, detailedInfo));
            msg.setData(bundle);
            msg.sendToTarget();

            if(path.equals("/"))
                path = "/index.html";
            if(path.equals("/favicon.ico"))
                path = "/index.html";
            if(path.equals("/streams/telemetry")){
                telemetry = true;
            }
            if(path.equals("/sensors.json"))
                handleJSON();

            if(path.equals("/cmd"))
                isCmd = true;

            if(telemetry){
                    Telemetry tel = new Telemetry(this.context);
                    tel.handleLocation();
                    tel.handleSensors();
                    JSONObject sensorData = tel.getSensorData();

                    File json = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/sensors.json");
                    if(!json.exists()){
                        json.createNewFile();
                    }

                    try {
                        FileWriter writer = new FileWriter(json);
                        writer.write(sensorData.toString());
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File streams = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/streams.html");
                    out.write("HTTP/1.0 200 OK\n");
                    out.write("Content-Type: " + getMimeType(streams.getAbsolutePath()) + "\n");
                    out.write("Content-Length: " + streams.length() + "\n");
                    out.write("\n");
                    out.flush();

                    FileInputStream fis = new FileInputStream(streams.getAbsolutePath());
                    byte[] buffer = new byte[4 * 1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        o.write(buffer, 0, bytesRead);
                    }
                    fis.close();
                    s.close();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


            }
            else{
                File fl = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), path);

                if(fl.exists()){
                    this.access.println("Client on ThreadID: " + Thread.currentThread().getId() + " is accessing file: " + fl.getAbsolutePath());
                    out.write("HTTP/1.0 200 OK\n");
                    out.write("Content-Type: " + getMimeType(fl.getAbsolutePath()) + "\n");
                    out.write("Content-Length: " + fl.length() + "\n");
                    out.write("\n");
                    out.flush();

                    FileInputStream fis = new FileInputStream(fl.getAbsolutePath());
                    byte[] buffer = new byte[4 * 1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        o.write(buffer, 0, bytesRead);
                    }

                }
                else{
                    out.write(buildResponse(0, null, 404, null));
                    out.flush();
                    Log.d("CLIENTTHREAD", "Error 404 - page not found");
                    this.errors.println("Client with ThreadID: " +Thread.currentThread().getId()+ " requested file that does not exist, returning 404");
                }
                s.close();
                Log.d("SERVER", "Socket Closed");
                this.access.println("Client on ThreadID: " + Thread.currentThread().getId() + " is closing");
            }

            }
            catch (IOException e){
                e.printStackTrace();
                Log.d("CLIENTTHREAD","There was a problem with client service - processing data and writing failed");
                this.errors.println("Client with ThreadID: " + Thread.currentThread().getId() + " - There was a problem with client service - processing data and writing failed");
        }
        finally {
            this.available.release();
            Log.d("CLIENTTHREAD", "Semaphore remaining permits: " + this.available.availablePermits());
            this.access.println("Client on ThreadID: " + Thread.currentThread().getId() + " was released, remaining permits: " + this.available.availablePermits());
        }
    }


    private void handleJSON(){
        Telemetry tel = new Telemetry(this.context);
        tel.handleLocation();
        tel.handleSensors();
        JSONObject sensorData = tel.getSensorData();

        File json = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/sensors.json");
        if(!json.exists()){
            try {
                json.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            FileWriter writer = new FileWriter(json);
            writer.write(sensorData.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String buildResponse(long len, String resp, int code, String type){
        String status = null;

        String body = null;
        long length = 0;
        switch (code){
            case 200:
                status = "200 OK";
                body = resp;
                break;
            case 404:
                status = "404 Not Found";
                body = "<html><body><h1 style='color: red'> Error 404 - Page not found</h1></body></html>";
                break;
            default:
                status = "500 Server Error";
                body = "<html><body><h1 style='color: red'> Error 500 - Server Error</h1></body></html>";
                break;
        }

        String response = "HTTP/1.0 " + status + ""+ "\n" +
                "Date: " + getServerTime() + "\n" +
                "Content-Type: " + type + "\n" +
                "Content-Length:" +
                "\n" + body;
        length =  len == 0 ? response.length() : len;
        response = response.replace("Content-Length:", "Content-Length: " + Long.toString(length) + "\n");
        return response;
    }
    private String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.GERMAN);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }


    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private String messageCreation(String request, String additional){
        StringBuilder str = new StringBuilder();
        String socketAddr = this.s.getRemoteSocketAddress().toString();

        Date actDate = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss z");
        String time = format.format(actDate);


        str.append(socketAddr + " - - ");
        str.append("[" +time + "] ");
        str.append(request + " - ");
        str.append(additional.replace("User-Agent: ", ""));

        return str.toString();
    }
}
