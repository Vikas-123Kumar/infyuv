package com.infyuv_app.infyuv.bluetoothConnection;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.infyuv_app.infyuv.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothProfile.GATT;

public class InfyuvConnection extends AppCompatActivity implements View.OnClickListener {
    private ListView listViewOFpairdevice, timerList;
    BluetoothSocket socket;
    Context context;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    private final static String default_notification_channel_id = "default";
    Button refrigerator, lavatory, general_item, open_area, alarm_button;
    private BluetoothDevice device;
    private ArrayAdapter arrayAdapterForList;
    ArrayAdapter<String> spinnerAdapter;
    Set<BluetoothDevice> bondedDevices;
    ArrayList list;
    byte buffer[];
    int i;
    AlarmManager alarmManager;
    PendingIntent pendingIntent;
    private long mTimeLeftInMili;
    private boolean mTimerRunning;
    int counter;
    boolean isDeviceConnected;
    SharedPreferences sharedPreferences;
    CountDownTimer countDownTimer;
    ConnectionBean connectionBean;
    ArrayList alarmTime;
    String timerForNotification = "", myTimePicker;
    ProgressBar progressBar;
    Handler handler = new Handler();
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    Button deviceOFF, bluetoothConnection, setting_action;
    private BluetoothAdapter BA;
    boolean deviceConnected = false;
    SharedPreferences.Editor myEdit;
    TextView countDown;
    private OutputStream outputStream;
    private InputStream inputStream;
    final static int RQS_1 = 1;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infyuv_connection);
        init();
        context = this;
        BA = BluetoothAdapter.getDefaultAdapter();
        onBluetooth();
        setupToolbar();
        sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        myEdit = sharedPreferences.edit();

        alarmTime = new ArrayList();
        String currentDateandTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        Log.e("current time ", currentDateandTime);
//        int i = Integer.parseInt(currentDateandTime);
//        Log.e("time",i+"");
        connectionBean = new ConnectionBean();
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        List<BluetoothDevice> connected = manager.getConnectedDevices(GATT);
        Log.i("Connected Devices: ", connected.size() + "");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        BA.startDiscovery();
        this.registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("data ", "in broadcast");
            String action = intent.getAction();
//            BluetoothDevice mdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            Log.e("device", mdevice + "");

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e("device in found", device.getName() + "");
                Log.e("connected", "connected");
                //Device found
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.e("connected in ", "connected");
                //Device is now connected
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e("connected in discover", "connected");

                //Done searching
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Device Disconnected", Toast.LENGTH_LONG).show();
                bluetoothConnection.setText("CONNECTION");
                bluetoothConnection.setEnabled(true);
                if (mTimerRunning == true) {
                    countDownTimer.cancel();
                    alarmManager.cancel(pendingIntent);
                }
                countDown.setText("00:00 mins");
                alarm_button.setText("START TIMER");
                refrigerator.setTag("ON");
                refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                lavatory.setTag("ON");
                lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                open_area.setTag("ON");
                open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                general_item.setTag("ON");
                general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                deviceOFF.setEnabled(false);
                if (socket.isConnected()) {
                    try {
                        socket.close();
                        isDeviceConnected=false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //Device has disconnected
            }
        }
    };

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        ActionBar action_Bar = getSupportActionBar();
        if (action_Bar != null) {
            action_Bar.setDisplayHomeAsUpEnabled(true);
            action_Bar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public void init() {

        bluetoothConnection = findViewById(R.id.connection);
        listViewOFpairdevice = findViewById(R.id.pair_devices);
        timerList = findViewById(R.id.timer);
        progressBar = findViewById(R.id.calProgress);
        setting_action = findViewById(R.id.setting_action);
        refrigerator = findViewById(R.id.refrigerator);
        lavatory = findViewById(R.id.lavatory);
        open_area = findViewById(R.id.open_area);
        general_item = findViewById(R.id.general_item);
        alarm_button = findViewById(R.id.alarm);
        deviceOFF = findViewById(R.id.turnOff);
        countDown = findViewById(R.id.countDown);
        refrigerator.setOnClickListener(this);
        lavatory.setOnClickListener(this);
        open_area.setOnClickListener(this);
        general_item.setOnClickListener(this);
        bluetoothConnection.setOnClickListener(this);
        setting_action.setOnClickListener(this);
        deviceOFF.setOnClickListener(this);
        deviceOFF.setEnabled(false);
        alarm_button.setOnClickListener(this);
    }

    public void onBluetooth() {
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        }
    }

    public void addAlarmTime() {
        alarmTime = new ArrayList();
        i = 0;
        alarmTime.add("1 Minute");
        alarmTime.add("2 Minutes");
        alarmTime.add("5 Minutes");
        alarmTime.add("10 Minutes");
        alarmTime.add("15 Minutes");
        alarmTime.add("20 Minutes");
        alarmTime.add("25 Minutes");
        final AlertDialog alertdailog = new AlertDialog.Builder(InfyuvConnection.this).create();
        spinnerAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, alarmTime);
        timerList.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
        alertdailog.setMessage("");
        if (timerList.getParent() != null) {
            ((ViewGroup) timerList.getParent()).removeView(timerList);
        }
        alertdailog.setView(timerList);
        alertdailog.setTitle("Time Duration:");
        alertdailog.show();
        timerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                timerForNotification = timerList.getItemAtPosition(position).toString();
                refrigerator.setTag("ON");
                refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                lavatory.setTag("ON");
                lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                open_area.setTag("ON");
                open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                general_item.setTag("ON");
                general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                alarm_button.setText(timerForNotification);
                mTimeLeftInMili = 60000;
                deviceOFF.setEnabled(true);
                sendtimeToUV(timerForNotification);

                if (mTimerRunning == true) {
                    countDownTimer.cancel();
                    alarmManager.cancel(pendingIntent);
                }
                if (timerForNotification.contains("Minutes")) {
                    timerForNotification.replace("Minutes", "");
                    myTimePicker = timerForNotification.replace("Minutes", "").trim();
                    i = Integer.parseInt(myTimePicker);
                    setAlarm(i);
                    mTimeLeftInMili = mTimeLeftInMili * Long.valueOf(i);
                    startTimer(mTimeLeftInMili);
                    Log.e("time", mTimeLeftInMili + "");
                } else if (timerForNotification.contains("Minute")) {
                    timerForNotification.replace("Minute", "");
                    myTimePicker = timerForNotification.replace("Minute", "").trim();
                    i = Integer.parseInt(myTimePicker);
                    setAlarm(i);
                    mTimeLeftInMili = mTimeLeftInMili * i;
                    startTimer(mTimeLeftInMili);
                    Log.e("time", mTimeLeftInMili + "");
                }

                alertdailog.dismiss();
            }
        });

    }

    public void sendtimeToUV(String timeToUV) {
        String onString = "";
        String offString = "0";
        if (socket != null) {
            if (socket.isConnected()) {
                try {
                    outputStream = socket.getOutputStream();
                    Log.e("device  after connected", "" + socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    inputStream = socket.getInputStream();
                    Log.e("output", inputStream + "");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (timeToUV.equals("1 Minute")) {
                        Log.e("timeToUV ", timeToUV);
                        onString = "1";
                        outputStream.write(offString.getBytes());
                        outputStream.write(onString.getBytes());

                    } else if (timeToUV.equals("2 Minutes")) {
                        onString = "2";
                        outputStream.write(offString.getBytes());
                        outputStream.write(onString.getBytes());
                    } else if (timeToUV.equals("5 Minutes")) {
                        onString = "3";
                        outputStream.write(offString.getBytes());
                        outputStream.write(onString.getBytes());
                    } else if (timeToUV.equals("10 Minutes")) {
                        onString = "4";
                        outputStream.write(offString.getBytes());
                        outputStream.write(onString.getBytes());
                    } else if (timeToUV.equals("15 Minutes")) {
                        onString = "5";
                        outputStream.write(offString.getBytes());
                        outputStream.write(onString.getBytes());

                    } else if (timeToUV.equals("20 Minutes")) {
                        onString = "6";
                        outputStream.write(offString.getBytes());
                        outputStream.write(onString.getBytes());

                    } else if (timeToUV.equals("25 Minutes")) {
                        onString = "7";
                        outputStream.write(offString.getBytes());
                        outputStream.write(onString.getBytes());
                    }
                    Log.e("socket connected", socket.isConnected() + "");
                } catch (IOException e) {
                    Log.e("timeToUV e", timeToUV);
                }
            }
        }
    }

    public void startTimer(long time) {
        countDownTimer = new CountDownTimer(time, 1000) {

            @Override
            public void onTick(long l) {
                mTimeLeftInMili = l;
                coundDownStart();
                mTimerRunning = true;
//                alarm_button.setEnabled(false);
            }

            @Override
            public void onFinish() {
                alarm_button.setEnabled(true);
            }
        }.start();
    }

    public void coundDownStart() {
        int minute = (int) (mTimeLeftInMili / 1000) / 60;
        int seconds = (int) (mTimeLeftInMili / 1000) % 60;
        String mLeftTimeFormate = String.format(Locale.getDefault(), "%02d:%02d", minute, seconds);
        countDown.setText(mLeftTimeFormate + " mins");
    }

    private void setAlarm(int minute) {
        Calendar now = Calendar.getInstance();
        if (socket != null) {
            connectionBean.setSocket(socket);
        }
        Log.e("alarm", minute + "");
        now.add(Calendar.MINUTE, minute);
        Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(getBaseContext(), RQS_1, intent, 0);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pendingIntent);

    }

    public boolean BTinit() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
        }
        bondedDevices = bluetoothAdapter.getBondedDevices();
        list = new ArrayList();

        if (bondedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
        } else {
            for (BluetoothDevice iterator : bondedDevices) {
                if (iterator != null) {
                    Log.e("Iterator Devices ", " " + iterator);
                }
                if (iterator.getName() != null) {
                    list.add(iterator.getName() + "\n" + iterator.getAddress());
                }
            }
            alertDialog();
        }
        return true;
    }

    //this method is to show the paired device in your mobile on dialog box.
    private void alertDialog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                final AlertDialog alertdailog = new AlertDialog.Builder(InfyuvConnection.this).create();
                arrayAdapterForList = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                listViewOFpairdevice.setAdapter(arrayAdapterForList);
                arrayAdapterForList.notifyDataSetChanged();
                alertdailog.setMessage("");
                if (listViewOFpairdevice.getParent() != null) {
                    ((ViewGroup) listViewOFpairdevice.getParent()).removeView(listViewOFpairdevice);
                }
                alertdailog.setView(listViewOFpairdevice);
                alertdailog.setTitle("List Of Bluetooth Pair Devices");
                alertdailog.show();
                listViewOFpairdevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        for (BluetoothDevice iterator : bondedDevices) {
                            Log.e("position", list.get(0) + "");
                            if (iterator.getAddress().equals(list.get(position).toString().split("\n")[1])) {
                                device = iterator;
                                myEdit.putString("name", device.getAddress());
                                Log.e("click device", device.getName() + "");
                                break;
                            }
                        }
                        alertdailog.dismiss();
                        BTconnect();
                        if (socket != null) {
                            if (socket.isConnected()) {
                                try {
                                    boolean status = false;
                                    status = true;
                                    if (status) {
                                        progressBar.setVisibility(View.GONE);
                                        bluetoothConnection.setText("Connected");
                                        bluetoothConnection.setEnabled(false);
                                        Toast.makeText(getApplicationContext(), "Device Connected", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                progressBar.setVisibility(View.GONE);
                                Toast toast1 = Toast.makeText(getApplicationContext(), "Device not connected", Toast.LENGTH_LONG);
                                toast1.show();
                                bluetoothConnection.setEnabled(true);
                            }
                        }
                    }
                });
            }
        });
    }
    //    private void alertDialog() {
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                progressBar.setVisibility(View.VISIBLE);
//                for (BluetoothDevice iterator : bondedDevices) {
//                    Log.e("position", list.get(0) + "");
//                    if (iterator.getName().equals("HC-05")) {
//                        device = iterator;
//                        myEdit.putString("name", device.getAddress());
//                        Log.e("click device", device.getName() + "");
//                        break;
//                    }
//                }
//                BTconnect();
//                if (socket != null) {
//                    if (socket.isConnected()) {
//                        try {
//                            boolean status = false;
//                            status = true;
//                            if (status) {
//                                progressBar.setVisibility(View.GONE);
//                                bluetoothConnection.setText("Connected");
//                                bluetoothConnection.setEnabled(false);
//                                Toast.makeText(getApplicationContext(), "Device Connected", Toast.LENGTH_SHORT).show();
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    } else {
//                        progressBar.setVisibility(View.GONE);
//                        Toast toast1 = Toast.makeText(getApplicationContext(), "Device not connected", Toast.LENGTH_LONG);
//                        toast1.show();
//                        bluetoothConnection.setEnabled(true);
//                    }
//                }
//            }
//        });
//    }
    public boolean BTconnect() {
        // making the connection of device to your mobile
        if (device != null) {
            boolean connected = false;
            try {
                Log.e("device name e", device + "");
                connectionBean.setDevice(device);
                socket = device.createInsecureRfcommSocketToServiceRecord(PORT_UUID);
                if (!socket.isConnected()) {
                    socket.connect();
                    isDeviceConnected = true;
//                    connectionBean.setSocket(socket);
                    Log.e("device name", socket.isConnected() + "");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("error", e.getMessage() + "");

                connected = true;
            }
            return connected;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.refrigerator) {
            String string = "r";
            string.concat("\n");
            String backgroundImageName = String.valueOf(refrigerator.getTag());
            Log.e("color", backgroundImageName + "");
            if (backgroundImageName.equals("ON")) {
                turnOnDevice(string);
            } else {
                turnOffDevice(string);
            }
        }
        if (view.getId() == R.id.lavatory) {
            String string = "l";
            string.concat("\n");
            String backgroundImageName = String.valueOf(lavatory.getTag());
            if (backgroundImageName.equals("ON")) {
                turnOnDevice(string);
            } else {
                turnOffDevice(string);
            }
        }
        if (view.getId() == R.id.open_area) {
            String string = "o";
            string.concat("\n");
            String backgroundImageName = String.valueOf(open_area.getTag());
            if (backgroundImageName.equals("ON")) {
                turnOnDevice(string);
            } else {
                turnOffDevice(string);
            }
        }
        if (view.getId() == R.id.general_item) {
            String string = "g";
            string.concat("\n");
            String backgroundImageName = String.valueOf(general_item.getTag());
            if (backgroundImageName.equals("ON")) {
                turnOnDevice(string);
            } else {
                turnOffDevice(string);
            }
        }
        if (view.getId() == R.id.connection) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (BTinit()) {
                        if (deviceConnected = true) {
                            Log.e("device connection", "connected");
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Device not connected", Toast.LENGTH_SHORT);
                    }
                }
            });
        }
        if (view.getId() == R.id.setting_action) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
        if (view.getId() == R.id.alarm) {
            if (isDeviceConnected == true) {
                addAlarmTime();
            } else {
                Toast.makeText(getApplicationContext(), "Please Connect To InfyUV", Toast.LENGTH_LONG).show();
            }
        }
        if (view.getId() == R.id.turnOff) {
            turnOffTimer();
        }

    }

    public void turnOffTimer() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure, You want to turn off device");
        alertDialogBuilder.setPositiveButton("yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (mTimerRunning == true) {
                            countDownTimer.cancel();
                        }
                        String offString = "0";
                        offString.concat("\n");
                        if (socket != null) {
                            if (socket.isConnected()) {
                                try {
                                    outputStream = socket.getOutputStream();
                                    Log.e("device  after connected", "" + socket);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    inputStream = socket.getInputStream();
                                    Log.e("output", inputStream + "");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    outputStream.write(offString.getBytes());
                                    Log.e("socket connected", socket.isConnected() + "");
                                } catch (IOException e) {
                                    Log.e("string e", offString);
                                }
                            }
                        }
                        countDown.setText("00:00 mins");
                        alarm_button.setText("START TIMER");
                        alarmManager.cancel(pendingIntent);
                        refrigerator.setTag("ON");
                        refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        lavatory.setTag("ON");
                        lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        open_area.setTag("ON");
                        open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        general_item.setTag("ON");
                        general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        deviceOFF.setEnabled(false);

                    }
                });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null && socket.isConnected()) {
            try {
                Log.e("socket connected", socket.isConnected() + "");
//                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void turnOnDevice(String string) {
        String offString = "0";
        mTimeLeftInMili = 60000;
        if (socket != null) {
            Log.e("isconnected", socket.isConnected() + "");
            if (socket.isConnected()) {
                connectionBean.setSocket(socket);
                connectionBean.setSwitch(true);
                try {
                    outputStream = socket.getOutputStream();
                    Log.e("device  after connected", "" + socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    inputStream = socket.getInputStream();
                    Log.e("output", inputStream + "");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (mTimerRunning == true) {
                        countDownTimer.cancel();
                        alarmManager.cancel(pendingIntent);
                    }
                    Log.e("string", string);
                    deviceOFF.setEnabled(true);
                    if (string.equals("r")) {
                        Log.e("string ", string);
                        refrigerator.setTag("OFF");
                        refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_off));
                        lavatory.setTag("ON");
                        lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        open_area.setTag("ON");
                        open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        general_item.setTag("ON");
                        general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        setAlarm(10);
                        mTimeLeftInMili = mTimeLeftInMili * 10;
                        startTimer(mTimeLeftInMili);
                    } else if (string.equals("l")) {
                        lavatory.setTag("OFF");
                        lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_off));
                        open_area.setTag("ON");
                        open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        general_item.setTag("ON");
                        general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        refrigerator.setTag("ON");
                        refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        setAlarm(15);
                        mTimeLeftInMili = mTimeLeftInMili * 15;
                        startTimer(mTimeLeftInMili);
                    } else if (string.equals("o")) {
                        open_area.setTag("OFF");
                        open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_off));
                        general_item.setTag("ON");
                        general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        refrigerator.setTag("ON");
                        refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        lavatory.setTag("ON");
                        lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        setAlarm(5);
                        mTimeLeftInMili = mTimeLeftInMili * 5;
                        startTimer(mTimeLeftInMili);
                    } else if (string.equals("g")) {
                        general_item.setTag("OFF");
                        general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_off));
                        refrigerator.setTag("ON");
                        refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        lavatory.setTag("ON");
                        lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        open_area.setTag("ON");
                        open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                        setAlarm(2);
                        mTimeLeftInMili = mTimeLeftInMili * 2;
                        startTimer(mTimeLeftInMili);
                    }
                    countDown.setText("00:00 mins");
                    alarm_button.setText("Start Timer");
                    outputStream.write(offString.getBytes());
                    outputStream.write(string.getBytes());
                    bluetoothConnection.setEnabled(false);
                } catch (IOException e) {
                    Log.e("string ", string);
                }

            } else {
                Toast.makeText(getApplicationContext(), "Please Connect To InfyUV ", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please Connect To InfyUV ", Toast.LENGTH_SHORT).show();
        }
    }

    public void turnOffDevice(String string) {
        String offString = "0";
        offString.concat("\n");
        if (mTimerRunning == true) {
            countDownTimer.cancel();
            countDown.setText("00:00 mins");
            alarm_button.setText("Start Timer");
        }
        if (socket != null) {
            if (socket.isConnected()) {
                try {
                    outputStream = socket.getOutputStream();
                    Log.e("device  after connected", "" + socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    inputStream = socket.getInputStream();
                    Log.e("output", inputStream + "");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (string.equals("r")) {
                        Log.e("string ", string);
                        refrigerator.setTag("ON");
                        refrigerator.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                    } else if (string.equals("l")) {
                        lavatory.setTag("ON");
                        lavatory.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                    } else if (string.equals("o")) {
                        open_area.setTag("ON");
                        open_area.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                    } else if (string.equals("g")) {
                        general_item.setTag("ON");
                        general_item.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_bacground));
                    }
                    alarmManager.cancel(pendingIntent);
                    deviceOFF.setEnabled(false);
                    outputStream.write(offString.getBytes());
                    Log.e("socket connected", socket.isConnected() + "");
                } catch (IOException e) {
                    Log.e("string e", offString);
                }
            }
        }
    }
}
