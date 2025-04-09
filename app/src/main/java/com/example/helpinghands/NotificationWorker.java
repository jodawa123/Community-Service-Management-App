package com.example.helpinghands;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {
    private static final String CHANNEL_ID = "helpinghands_channel";
    public static final String[] weeklyMessages = {
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
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("HelpingHandsPrefs", Context.MODE_PRIVATE);

        // Get both the calculated week and last notified week
        int calculatedWeek = prefs.getInt("current_week", 1);
        int lastNotifiedWeek = prefs.getInt("last_notified_week", 0);

        // Don't proceed if we've completed all weeks
        if (calculatedWeek > weeklyMessages.length) {
            cancelNotifications(context);
            return Result.success();
        }

        // Only send notification if we haven't already notified for this week
        if (calculatedWeek > lastNotifiedWeek) {
            sendNotification(weeklyMessages[calculatedWeek - 1]);
            prefs.edit()
                    .putInt("last_notified_week", calculatedWeek)
                    .apply();

            // Special handling for final week
            if (calculatedWeek == weeklyMessages.length) {
                cancelNotifications(context);
            }
        }

        return Result.success();
    }

    private void cancelNotifications(Context context) {
        WorkManager.getInstance(context)
                .cancelUniqueWork("weekly_encouragement");

        // Clear notification badges if needed
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }

    private void sendNotification(String message) {
        createNotificationChannel();

        Intent intent = new Intent(getApplicationContext(), Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Community Service Update")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(createNotificationId(), notification);
    }

    private int createNotificationId() {
        return (int) System.currentTimeMillis();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Helping Hands Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Weekly encouragement messages");
            NotificationManager manager =
                    (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}