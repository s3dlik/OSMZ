package com.vsb.kru13.osmzhttpserver;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.gms.location.FusedLocationProviderClient;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketServer extends Thread {

    ServerSocket serverSocket;
    public final int port = 12345;
    boolean bRunning;
    int connected_clients = 0;
    Handler handler;
    Location location;
    Context context;
    public SocketServer(Context context, Handler handler){
        this.context = context;
        this.handler = handler;
    }
    private final Semaphore available = new Semaphore(3);
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    public void run() {
        try {

            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            Log.d("SERVER", "Socket Waiting for connection");
            Log.d("CLIENTTHREAD", "Semaphore remaining permits: " + this.available.availablePermits());
            bRunning = true;

            File access_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/access.log");
            File errors_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/errors.log");
            if(!access_file.exists()){
                access_file.createNewFile();
            }
            if(!errors_file.exists()){
                errors_file.createNewFile();
            }

            FileWriter errors = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath()+"/errors.log", true);
            FileWriter access = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath()+"/access.log", true);
            PrintWriter printWriterAccess = new PrintWriter(access);
            PrintWriter printWriterErrors = new PrintWriter(errors);
            while (bRunning) {

                Socket s = serverSocket.accept();
                Log.d("SERVER", "Socket Accepted");
                printWriterAccess.println("Socket was accepted at time: " + Calendar.getInstance().getTime());
                OutputStream o = s.getOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                if(this.available.tryAcquire()){

                    ClientThread th = new ClientThread(context, s, this.available, this.handler, printWriterAccess, printWriterAccess, this.location);
                    printWriterAccess.println("Client with Thread ID: " + Thread.currentThread().getId() + " has been started at time: " + Calendar.getInstance().getTime());
                    printWriterAccess.flush();
                    th.start();
                }else{
                    out.write("HTTP/1.0 503 HTTP 503 Server too busy\n");
                    out.write("Content-Type: text/html\n");
                    out.write("\n");
                    out.write("<html><h1>HTTP 503 Server too busy</h1></html>");
                    out.flush();
                    s.close();
                    Log.d("SERVER", "Socket Closed");
                    printWriterErrors.println("Client with Thread ID: " + Thread.currentThread().getId() + " has been closed with error 503 due to full server at time: " +  Calendar.getInstance().getTime());
                    printWriterErrors.flush();
                }
            }
        }
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        }
        finally {
            serverSocket = null;
            bRunning = false;
        }
    }
}

