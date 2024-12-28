package com.example.cs3700;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private TextView user_name, pickedSiteName, availableSlots, hoursRemaining, weeksRemainingText;
    private RadioButton dropSiteButton;
    private FirebaseFirestore firestore;
    private String selectedCategory, selectedSite, name;
    private int currentAvailableSlots;

    private View siteSection;
    private View hoursSection;
    private CalendarView calendarView;

    private static final int TOTAL_REQUIRED_HOURS = 90;
    private static final int HOURS_PER_WEEK = 9;
    private Date startDate;
    private int totalWeeksRemaining;
    private int totalHoursRemaining;

    private FirebaseUser currentUser;
    private DocumentReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Views
        user_name = findViewById(R.id.user_name);
        pickedSiteName = findViewById(R.id.picked_site_name);
        availableSlots = findViewById(R.id.available_slots);
        dropSiteButton = findViewById(R.id.drop_site_button);
        calendarView = findViewById(R.id.calendarView);
        hoursRemaining = findViewById(R.id.hoursRemaining);
        weeksRemainingText = findViewById(R.id.weeksRemaining);

        siteSection = findViewById(R.id.site_section);
        hoursSection = findViewById(R.id.hours_section);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            userRef = firestore.collection("UserStates").document(currentUser.getUid());
            loadStateFromFirebase();
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
        }

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            startDate = selectedDate.getTime();

            initializeCountdown(startDate);
            updateCountdownUI();

            saveStateToFirebase();
        });

        dropSiteButton.setOnClickListener(v -> dropSelectedSite());
    }

    private void loadStateFromFirebase() {
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                selectedCategory = documentSnapshot.getString("selectedCategory");
                selectedSite = documentSnapshot.getString("selectedSite");
                currentAvailableSlots = documentSnapshot.getLong("currentAvailableSlots").intValue();
                long startDateMillis = documentSnapshot.getLong("startDate");
                startDate = new Date(startDateMillis);

                initializeCountdown(startDate);
                updateCountdownUI();

                fetchSiteDetails();
            } else {
                Toast.makeText(this, "state found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error loading state.", Toast.LENGTH_SHORT).show());
    }

    private void saveStateToFirebase() {
        if (currentUser != null && selectedCategory != null && selectedSite != null && startDate != null) {
            Map<String, Object> state = new HashMap<>();
            state.put("selectedCategory", selectedCategory);
            state.put("selectedSite", selectedSite);
            state.put("currentAvailableSlots", currentAvailableSlots);
            state.put("startDate", startDate.getTime());

            userRef.set(state).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "State saved successfully.", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to save state.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void fetchSiteDetails() {
        firestore.collection(selectedCategory)
                .whereEqualTo("head", selectedSite)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().forEach(document -> {
                            pickedSiteName.setText("Selected Site: " + selectedSite);

                            currentAvailableSlots = document.getLong("availableSlots").intValue();
                            availableSlots.setText("Slots: " + currentAvailableSlots);

                            showSiteDetails();
                        });
                    } else {
                        Toast.makeText(this, "Site details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching site details.", Toast.LENGTH_SHORT).show());
    }

    private void dropSelectedSite() {
        if (currentUser != null && selectedSite != null) {
            firestore.collection(selectedCategory)
                    .whereEqualTo("head", selectedSite)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            querySnapshot.getDocuments().forEach(document -> {
                                DocumentReference siteRef = document.getReference();

                                siteRef.update("availableSlots", currentAvailableSlots + 1)
                                        .addOnSuccessListener(aVoid -> {
                                            userRef.delete()
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        Toast.makeText(this, "User and site selection deleted successfully.", Toast.LENGTH_SHORT).show();
                                                        hideSiteDetails();
                                                        selectedSite = null;
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(this, "Error deleting user document.", Toast.LENGTH_SHORT).show());
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update site slots.", Toast.LENGTH_SHORT).show());
                            });
                        } else {
                            Toast.makeText(this, "Site not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error fetching site details.", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No site selected or user not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSiteDetails() {
        siteSection.setVisibility(View.VISIBLE);
        calendarView.setVisibility(View.VISIBLE);
        hoursSection.setVisibility(View.VISIBLE);
    }

    private void hideSiteDetails() {
        siteSection.setVisibility(View.GONE);
        calendarView.setVisibility(View.GONE);
        hoursSection.setVisibility(View.GONE);
    }

    private void initializeCountdown(Date start) {
        this.startDate = start;
        totalHoursRemaining = TOTAL_REQUIRED_HOURS;
        totalWeeksRemaining = 10;
    }

    private void updateCountdownUI() {
        if (startDate == null) {
            Toast.makeText(this, "Please select a start date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar currentCalendar = Calendar.getInstance();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);

        long diffInMillis = currentCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
        int weeksPassed = (int) (diffInMillis / (1000 * 60 * 60 * 24 * 7));

        totalWeeksRemaining = Math.max(0, 10 - weeksPassed);
        totalHoursRemaining = Math.max(0, totalWeeksRemaining * HOURS_PER_WEEK);

        weeksRemainingText.setText("Weeks Remaining: " + totalWeeksRemaining);
        hoursRemaining.setText("Hours Remaining: " + totalHoursRemaining);

        if (totalWeeksRemaining <= 0) {
            Toast.makeText(this, "Community service completed!", Toast.LENGTH_SHORT).show();
        }
    }
}
