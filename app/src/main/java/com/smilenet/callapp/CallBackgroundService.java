package com.smilenet.callapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CallBackgroundService extends Service {

    private static final String CHANNEL_ID = "SmileNet_Channel";
    private DatabaseReference callRef;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1001, buildNotification());
        listenToIncomingCalls();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SmileNet Wi-Fi Call Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SmileNet ဖုန်းစနစ်")
                .setContentText("Wi-Fi ဖုန်းခေါ်ဆိုမှုအတွက် အသင့်ရှိနေပါသည်")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void listenToIncomingCalls() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        int hash = Math.abs(androidId.hashCode() % 900000) + 100000;
        String myFixed6DigitId = String.valueOf(hash);

        callRef = FirebaseDatabase.getInstance().getReference("calls").child(myFixed6DigitId);
        callRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && "ringing".equals(snapshot.child("status").getValue(String.class))) {
                    String callerId = snapshot.child("caller").getValue(String.class);

                    Intent intent = new Intent(CallBackgroundService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("incoming_caller", callerId);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
