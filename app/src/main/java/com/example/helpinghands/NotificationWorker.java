package com.example.helpinghands;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class NotificationWorker extends Worker {
    private static final String CHANNEL_ID = "helpinghands_channel";
    private static final String[] weeklyMessages = {
            "Week 1: You're just getting started! Keep up the great work!🌟",
            "Week 2: Consistency is key. You're doing amazing💪",
            "Week 3: Almost there 🚀 Keep pushing forward!",
            "Week 4: You're making a difference 💯. Don't stop now!",
            "Week 5: You're crushing it! Only a few weeks left🔥",
            "Week 6: So close to the finish line. Keep going ⭐!",
            "Week 7: You're almost there! One last push 🏁",
            "Week 8: You're doing fantastic! Keep it up! 🎯",
            "Week 9: Just one more week to go! You've got this! 🚀",
            "Week 10: Congratulations 🎉 You've completed your community service!"
    };

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int weeksRemaining = getInputData().getInt("WEEKS_REMAINING", 10);

        if (weeksRemaining >= 0 && weeksRemaining < weeklyMessages.length) {
            // Send the notification
            sendNotification("Weekly Update", weeklyMessages[10 - weeksRemaining]);

            // Decrement the weeks remaining and reschedule
            Data newData = new Data.Builder()
                    .putInt("WEEKS_REMAINING", weeksRemaining - 1)
                    .build();
            PeriodicWorkRequest newWork = new PeriodicWorkRequest.Builder(
                    NotificationWorker.class,
                    7, // Repeat interval (7 days)
                    TimeUnit.DAYS,
                    1, // Flex interval (1 day)
                    TimeUnit.DAYS
            )
                    .setInputData(newData)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(newWork);
        }
        return Result.success();
    }

    private void sendNotification(String title, String message) {
        // Create a notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "HelpingHands Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        // Create an intent to open the app when the notification is clicked
        Intent intent = new Intent(getApplicationContext(), Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, flags);

        // Build the notification
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Use your app's notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        // Display the notification
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }
}