package com.example.cs3700;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
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
    private CurvedPathView curvedPathView;
    private ProgressBar countdownProgressBar;
    private TextView daysRemainingText;
    private TextView hoursRemainingText;
    private static final String TAG = "Curve";
    private static final String PREFS_NAME = "ProgressPrefs";
    private static final String KEY_LAST_NOTIFIED_WEEK = "last_notified_week";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curve);
        curvedPathView = findViewById(R.id.curvedPathView);
        countdownProgressBar = findViewById(R.id.countdownProgressBar);
        daysRemainingText = findViewById(R.id.daysText);
        hoursRemainingText = findViewById(R.id.hoursText);

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

                    // Calculate the number of days and hours remaining
                    int daysRemaining = calculateDaysRemaining(startDate);
                    int hoursRemaining = calculateHoursRemaining(startDate);
                    Log.d(TAG, "Days Remaining: " + daysRemaining);
                    Log.d(TAG, "Hours Remaining: " + hoursRemaining);

                    // Update the TextViews
                    daysRemainingText.setText(String.valueOf(daysRemaining));
                    hoursRemainingText.setText(String.valueOf(hoursRemaining));

                    // Update the progress bar
                    updateProgressBar(daysRemaining);

                    // Calculate the number of weeks since the start date
                    int currentWeek = calculateCurrentWeek(startDate);
                    Log.d(TAG, "Current Week: " + currentWeek);

                    // Update the progress in the curved view
                    curvedPathView.setWeekProgress(currentWeek);

                    // Send a notification only if the current week is new
                    sendNotificationIfNewWeek(currentWeek);
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching data from Firestore", e));
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not available in older versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Progress Notifications";
            String description = "Notifications for weekly progress updates";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotificationIfNewWeek(int currentWeek) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int lastNotifiedWeek = prefs.getInt(KEY_LAST_NOTIFIED_WEEK, 0);

        if (currentWeek > lastNotifiedWeek) {
            // Send the notification
            sendNotification(currentWeek);

            // Update the last notified week in SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_LAST_NOTIFIED_WEEK, currentWeek);
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

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification) // Add a notification icon
                    .setContentTitle("Week " + currentWeek)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private int calculateCurrentWeek(Date startDate) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);

        Calendar today = Calendar.getInstance();

        long diffInMillis = today.getTimeInMillis() - startCalendar.getTimeInMillis();
        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

        return (int) Math.ceil(diffInDays / 7.0); // Convert days to weeks
    }

    private int calculateDaysRemaining(Date startDate) {
        int ten= 10-calculateCurrentWeek(startDate);
        return ten *3;
    }

    private int calculateHoursRemaining(Date startDate) {
        int hrs=calculateDaysRemaining(startDate);
        return hrs *3;
    }

    private void updateProgressBar(int daysRemaining) {
        int progress = 30-daysRemaining;
        countdownProgressBar.setProgress(progress);
    }
}