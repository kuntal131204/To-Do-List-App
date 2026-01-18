package com.example.todolist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String taskTitle = intent.getStringExtra("task");
        String soundUri = intent.getStringExtra("sound");

        Uri uri = soundUri != null
                ? Uri.parse(soundUri)
                : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "task_channel")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Task Reminder")
                        .setContentText(taskTitle)
                        .setSound(uri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), builder.build());
    }
}
