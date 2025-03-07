package com.example.cs3700;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;

public class Curve extends AppCompatActivity {
    private static final String CHANNEL_ID = "progress_notifications";
    private static final int NOTIFICATION_ID = 1;
    private ProgressBar countdownProgressBar;
    private TextView daysRemainingText;
    private TextView hoursRemainingText;
    private TextView weeksRemainingText;
    private static final String TAG = "Curve";
    private static final String PREFS_NAME = "ProgressPrefs";
    private static final String KEY_LAST_NOTIFIED_WEEK = "last_notified_week";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curve);
        countdownProgressBar = findViewById(R.id.countdownProgressBar);
        daysRemainingText = findViewById(R.id.daysText);
        hoursRemainingText = findViewById(R.id.hoursText);
        weeksRemainingText = findViewById(R.id.weeksText);

        // Create a notification channel for Android O and above
        createNotificationChannel();

        // Fetch the start date from Firestore
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentRef = db.collection("UserStates").document(userId);

        documentRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long startDateMillis = documentSnapshot.getLong("startDate");

                if (startDateMillis != null) {
                    Date startDate = new Date(startDateMillis);

                    // Calculate remaining weeks, days, and hours
                    int weeksRemaining = calculateRemainingWeeks(startDate);
                    int daysRemaining = weeksRemaining * 3;
                    int hoursRemaining = daysRemaining * 3;
                    Log.d(TAG, "Weeks Remaining: " + weeksRemaining);
                    Log.d(TAG, "Days Remaining: " + daysRemaining);
                    Log.d(TAG, "Hours Remaining: " + hoursRemaining);

                    // Update the TextViews
                    weeksRemainingText.setText(String.valueOf(weeksRemaining));
                    daysRemainingText.setText(String.valueOf(daysRemaining));
                    hoursRemainingText.setText(String.valueOf(hoursRemaining));

                    // Update the progress bar
                    updateProgressBar(daysRemaining);

                    // Send a notification only if the week changes
                    sendNotificationIfNewWeek(weeksRemaining);
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching data from Firestore", e));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Progress Notifications";
            String description = "Notifications for weekly progress updates";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotificationIfNewWeek(int weeksRemaining) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int lastNotifiedWeek = prefs.getInt(KEY_LAST_NOTIFIED_WEEK, 10);

        if (weeksRemaining < lastNotifiedWeek) {
            sendNotification(10 - weeksRemaining);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_LAST_NOTIFIED_WEEK, weeksRemaining);
            editor.apply();
        }
    }

    private void sendNotification(int currentWeek) {
        String[] encouragingMessages = {
                "You're doing great! Keep it up! 🌟",
                "One step at a time! You've got this! 💪",
                "Believe in yourself! You're amazing! ✨",
                "Every week counts! Keep pushing! 🚀",
                "You're making progress! Stay strong! 💯",
                "You're unstoppable! Keep going! 🔥",
                "You're on the right track! Keep shining! 🌈",
                "You're a star! Keep reaching for the sky! ⭐",
                "You're crushing it! Keep up the good work! 🎉",
                "You're almost there! Finish strong! 🏁"
        };

        if (currentWeek >= 1 && currentWeek <= 10) {
            String message = encouragingMessages[currentWeek - 1];
            Log.d(TAG, "Sending notification for week " + currentWeek + ": " + message);

            // Create an intent to open the app when the notification is clicked
            Intent intent = new Intent(this, Curve.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Week " + currentWeek)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent) // Set the intent to launch the app
                    .setAutoCancel(true); // Automatically dismiss the notification when clicked

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Notification sent successfully");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } else {
            Log.e(TAG, "Invalid currentWeek: " + currentWeek);
        }
    }

    private int calculateRemainingWeeks(Date startDate) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        Calendar today = Calendar.getInstance();
        long diffInMillis = today.getTimeInMillis() - startCalendar.getTimeInMillis();
        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
        int elapsedWeeks = (int) Math.ceil(diffInDays / 7.0);
        return Math.max(10 - elapsedWeeks, 0);
    }

    private void updateProgressBar(int daysRemaining) {
        int progress = 30 - daysRemaining;
        countdownProgressBar.setProgress(progress);
    }
}