package com.infyuv_app.infyuv.bluetoothConnection;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.infyuv_app.infyuv.R;


public class AlarmReceiver extends BroadcastReceiver {
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    private final static String default_notification_channel_id = "default";
    BluetoothSocket socket;
    boolean Switch;
    BluetoothDevice device;
    ConnectionBean connectionBean = new ConnectionBean();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        socket = connectionBean.getSocket();
        Switch = connectionBean.isSwitch();
        device = connectionBean.getDevice();
        Log.e("log in ", device + "");

        Log.e("log in reciever", socket + "");
        if (socket != null) {
            Toast.makeText(arg0, "Alarm received!" + socket.isConnected(), Toast.LENGTH_LONG).show();
        }
        Log.e("alarm start", "start");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            show_Notification(arg0);
        }
        Vibrator vibrator = (Vibrator) arg0.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(3000);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void show_Notification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, default_notification_channel_id);
        mBuilder.setContentTitle("Time complete.");
        mBuilder.setContentText("InfyUV ");
        mBuilder.setTicker("Notification Listener Service Example");
        Log.e("notification", "Notification Listener Service Example");
        mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
        mBuilder.setAutoCancel(true);
        mBuilder.setVibrate(new long[]{1000, 1000, 1000});
        Intent notifyIntent = new Intent(context, InfyuvConnection.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //to be able to launch your activity from the notification
        mBuilder.setContentIntent(pendingIntent);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        assert mNotificationManager != null;
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
