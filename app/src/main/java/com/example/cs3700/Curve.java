package com.example.cs3700;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class Curve extends AppCompatActivity {
    private CurvedPathView curvedPathView;
    private static final String TAG = "Curve";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curve);
        curvedPathView = findViewById(R.id.curvedPathView);

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

                    // Calculate the number of weeks since the start date
                    int currentWeek = calculateCurrentWeek(startDate);
                    Log.d(TAG, "Current Week: " + currentWeek);

                    // Update the progress in the curved view
                    curvedPathView.setWeekProgress(currentWeek);
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching data from Firestore", e));
    }

    private int calculateCurrentWeek(Date startDate) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);

        Calendar today = Calendar.getInstance();

        long diffInMillis = today.getTimeInMillis() - startCalendar.getTimeInMillis();
        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

        return (int) Math.ceil(diffInDays / 7.0); // Convert days to weeks
    }
}
