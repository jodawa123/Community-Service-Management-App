package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.github.muddz.styleabletoast.StyleableToast;


public class Profile extends AppCompatActivity {

    private TextView pickedSiteName, availableSlots, hoursRemaining, weeksRemainingText,pick;
    private FirebaseFirestore firestore;
    private String selectedCategory, selectedSite;
    private int currentAvailableSlots;
    private View siteSection,hoursSection,documents_section;
    private CalendarView calendarView;
    private static final int TOTAL_REQUIRED_HOURS = 90;
    private static final int HOURS_PER_WEEK = 9;
    private Date startDate;
    private int totalWeeksRemaining;
    private int totalHoursRemaining;
    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private ImageView imageView4;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);


        Animation ani = AnimationUtils.loadAnimation(this, R.anim.pulsating_animation);
        pick=findViewById(R.id.pick);
        pick.setAnimation(ani);
        // Initialize Views

        pickedSiteName = findViewById(R.id.picked_site_name);
        availableSlots = findViewById(R.id.available_slots);
        RadioButton dropSiteButton = findViewById(R.id.drop_site_button);
        calendarView = findViewById(R.id.calendarView);
        hoursRemaining = findViewById(R.id.hoursRemaining);
        weeksRemainingText = findViewById(R.id.weeksRemaining);
        imageView4=findViewById(R.id.imageView4);

        siteSection = findViewById(R.id.site_section);
        hoursSection = findViewById(R.id.hours_section);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        selectedCategory = getIntent().getStringExtra("CATEGORY_NAME");
        selectedSite = getIntent().getStringExtra("SELECTED_SITE");

        imageView4.setOnClickListener(view -> {
            Intent intent = new Intent(Profile.this,Home.class);
            startActivity(intent);
        });

        // Initialize User Reference
        if (currentUser != null) {
            userRef = firestore.collection("UserStates").document(currentUser.getUid());
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Handle Scenario: Intent Data or Firebase Data
        if (selectedCategory != null && selectedSite != null) {
            // Data passed via Intent
            fetchSiteDetails();
        } else {
            // Load saved state from Firestore
            loadStateFromFirebase();
        }

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            if (startDate == null) { // Allow selection only if no date has been set
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                startDate = selectedDate.getTime();
                // Save the selected date
                initializeCountdown(startDate);
                saveStateToFirebase();
                // Highlight the selected date
                calendarView.setDate(startDate.getTime(), true, true);
                // Disable further interactions
                calendarView.setEnabled(false);
                StyleableToast.makeText(this, "Start date set to: "+ startDate, R.style.mytoast).show();
            } else {
                StyleableToast.makeText(this, "You already have a start date.", R.style.mytoast).show();
            }
        });
        dropSiteButton.setOnClickListener(v -> dropSelectedSite());
    }

    private void loadStateFromFirebase() {
        if (currentUser != null) {
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Load saved state
                    selectedSite = documentSnapshot.getString("selectedSite");
                    selectedCategory = documentSnapshot.getString("selectedCategory"); // Add this
                    currentAvailableSlots = documentSnapshot.getLong("currentAvailableSlots").intValue();
                    long startDateMillis = documentSnapshot.getLong("startDate");

                    // Update UI with loaded state
                    pickedSiteName.setText("Selected Site: " + selectedSite);
                    availableSlots.setText("Slots: " + currentAvailableSlots);
                    if (startDateMillis != 0) {
                        startDate = new Date(startDateMillis);
                        initializeCountdown(startDate);
                        updateCountdownUI();
                    }
                    // Fetch details if category is available
                    if (selectedCategory != null) {
                        fetchSiteDetails();
                    } else {
                        showSiteDetails();
                    }
                } else {
                    Toast.makeText(this, "No saved site.", Toast.LENGTH_SHORT).show();
                    hideSiteDetails();
                }
            }).addOnFailureListener(e -> Toast.makeText(this, "Error loading state.", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveStateToFirebase() {
        if (userRef == null) {
           StyleableToast.makeText(this, "User reference is not initialized.", R.style.mytoast).show();
            return;
        }

        if (selectedSite == null) {
            StyleableToast.makeText(this, "No site selected to save.", R.style.mytoast).show();
            return;
        }

        if (startDate == null) {
           StyleableToast.makeText(this, "Start date is not set.", R.style.mytoast).show();
            return;
        }
        Map<String, Object> state = new HashMap<>();
        state.put("selectedSite", selectedSite);
        state.put("currentAvailableSlots", currentAvailableSlots);
        state.put("startDate", startDate.getTime());
        state.put("selectedCategory", selectedCategory);

        userRef.set(state)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "State saved successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save state: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                            //saveStateToFirebase();
                        });
                    } else {
                        Toast.makeText(this, "Site details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching site details.", Toast.LENGTH_SHORT).show());
    }
    private void dropSelectedSite() {
        if (selectedCategory != null) { // if user chooses to directly delete from the intent
            firestore.collection(selectedCategory)
                    .whereEqualTo("head", selectedSite)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            querySnapshot.getDocuments().forEach(document -> {
                                DocumentReference siteRef = document.getReference();

                                // Start a Firestore batch
                                firestore.runBatch(batch -> {
                                    // Update site slot count
                                    batch.update(siteRef, "availableSlots", currentAvailableSlots + 1);

                                    // Delete from UserStates
                                    batch.delete(userRef);

                                    // Delete from UserSelection (assuming UserSelection is a separate collection)
                                    DocumentReference userSelectionRef = firestore.collection("UserSelections")
                                            .document(currentUser.getUid());
                                    batch.delete(userSelectionRef);
                                }).addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "User and site selection deleted successfully.", Toast.LENGTH_SHORT).show();
                                    hideSiteDetails();
                                    selectedSite = null; // Clear the current site
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to complete drop operation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            });
                        } else {
                            Toast.makeText(this, "Site not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error fetching site details.", Toast.LENGTH_SHORT).show());
        }
        else {
            // If user deletes using the loadFirebase
            firestore.collection("UserSelections")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String selectedSite = documentSnapshot.getString("selectedSite");
                            String selectedCategory = documentSnapshot.getString("selectedCategory");

                            if (selectedSite != null && selectedCategory != null) {
                                // Search for the site in the selected category
                                firestore.collection(selectedCategory)
                                        .whereEqualTo("head", selectedSite)
                                        .get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            if (!querySnapshot.isEmpty()) {
                                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                                DocumentReference siteRef = document.getReference();
                                                Long availableSlots = document.getLong("availableSlots");

                                                if (availableSlots != null) {
                                                    // Increment the available slots
                                                    siteRef.update("availableSlots", availableSlots + 1)
                                                            .addOnSuccessListener(aVoid -> {
                                                                // Perform batch deletion for UserSelections and UserStates
                                                                firestore.runBatch(batch -> {
                                                                    DocumentReference userSelectionsRef = firestore.collection("UserSelections").document(currentUser.getUid());
                                                                    batch.delete(userSelectionsRef);

                                                                    DocumentReference userStatesRef = firestore.collection("UserStates").document(currentUser.getUid());
                                                                    batch.delete(userStatesRef);
                                                                }).addOnSuccessListener(batchSuccess -> {
                                                                    Toast.makeText(this, "You successfully dropped your selection.", Toast.LENGTH_SHORT).show();
                                                                    Log.d("Debug", "Deleted successfully.");
                                                                    hideSiteDetails();
                                                                }).addOnFailureListener(batchFailure -> {
                                                                    Toast.makeText(this, "Failed to drop your selection: " + batchFailure.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    Log.e("Error", "Batch deletion failed: " + batchFailure.getMessage());
                                                                });
                                                            })
                                                            .addOnFailureListener(updateFailure -> {
                                                                Toast.makeText(this, "Failed to update site: " + updateFailure.getMessage(), Toast.LENGTH_SHORT).show();
                                                                Log.e("Error", "Failed to update available slots: " + updateFailure.getMessage());
                                                            });
                                                } else {
                                                    Toast.makeText(this, "Invalid available slots data for the site.", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(this, "No matching site found in the selected category.", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(queryFailure -> {
                                            Toast.makeText(this, "Error querying site: " + queryFailure.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e("Error", "Error querying site: " + queryFailure.getMessage());
                                        });
                            } else {
                                Toast.makeText(this, "No site or category found in your selection.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No selection found for the user.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(fetchFailure -> {
                        Toast.makeText(this, "Failed to fetch user selection: " + fetchFailure.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Error", "Failed to fetch user selection: " + fetchFailure.getMessage());
                    });
        }
    }
    private void showSiteDetails() {
        siteSection.setVisibility(View.VISIBLE);
        calendarView.setVisibility(View.VISIBLE);
        hoursSection.setVisibility(View.VISIBLE);
        pick.setVisibility(View.VISIBLE);


    }
    private void hideSiteDetails() {
        siteSection.setVisibility(View.GONE);
        calendarView.setVisibility(View.GONE);
        hoursSection.setVisibility(View.GONE);
        pick.setVisibility(View.GONE);

    }
    private void initializeCountdown(Date start) {
        this.startDate = start;
        totalHoursRemaining = TOTAL_REQUIRED_HOURS;
        totalWeeksRemaining = 10;
        updateCountdownUI();
    }
    private void updateCountdownUI() {
        if (startDate == null) {
            Toast.makeText(this, "Please select a start date first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar currentCalendar = Calendar.getInstance();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);

        // Calculate the difference in milliseconds
        long diffInMillis = currentCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();

        // If the start date is in the future, set weeksPassed to 0
        int weeksPassed = diffInMillis > 0 ? (int) (diffInMillis / (1000 * 60 * 60 * 24 * 7)) : 0;

        totalWeeksRemaining = Math.max(0, 10 - weeksPassed);
        totalHoursRemaining = Math.max(0, totalWeeksRemaining * HOURS_PER_WEEK);

        weeksRemainingText.setText("Weeks Remaining: " + totalWeeksRemaining);
        hoursRemaining.setText("Hours Remaining: " + totalHoursRemaining);

        if (totalWeeksRemaining <= 0) {
            Toast.makeText(this, "Community service completed!", Toast.LENGTH_SHORT).show();
        }
    }
}
