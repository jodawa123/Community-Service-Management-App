package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
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
import android.content.Intent;
import io.github.muddz.styleabletoast.StyleableToast;


public class Profile extends AppCompatActivity {

    private TextView pickedSiteName, availableSlots, hoursRemainingtx, weeksRemainingText, pick;
    private FirebaseFirestore firestore;
    private String selectedCategory, selectedSite;
    private int currentAvailableSlots;
    private View siteSection, hoursSection, documents_section;
    private CalendarView calendarView;
    private static final int TOTAL_REQUIRED_HOURS = 90;
    private static final int HOURS_PER_WEEK = 9;
    private Date startDate, endDate;
    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private ImageView imageView4,down;
    private Button resetDateButton,track;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        Animation ani = AnimationUtils.loadAnimation(this, R.anim.pulsating_animation);
        pick = findViewById(R.id.pick);
        pick.setAnimation(ani);
        // Initialize Views

        pickedSiteName = findViewById(R.id.picked_site_name);
        availableSlots = findViewById(R.id.available_slots);
        RadioButton dropSiteButton = findViewById(R.id.drop_site_button);
        calendarView = findViewById(R.id.calendarView);
        hoursRemainingtx = findViewById(R.id.hoursRemaining);
        weeksRemainingText = findViewById(R.id.weeksRemaining);
        imageView4 = findViewById(R.id.imageView4);
        down = findViewById(R.id.doc1_download);
        resetDateButton = findViewById(R.id.bt);
        track=findViewById(R.id.bt1);

        siteSection = findViewById(R.id.site_section);
        hoursSection = findViewById(R.id.hours_section);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        selectedCategory = getIntent().getStringExtra("CATEGORY_NAME");
        selectedSite = getIntent().getStringExtra("SELECTED_SITE");

        imageView4.setOnClickListener(view -> {
            Intent intent = new Intent(Profile.this, Home.class);
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
        resetDateButton.setOnClickListener(v -> resetStartDate());

        down.setOnClickListener(view -> {
            String fileUrl = "https://drive.google.com/uc?id=1t6YyMTxAI2IE14wcye737s2vIsde0r0H&export=download";
            String fileName = "COMMUNITY.docx";
            downloadFile(fileUrl, fileName);
        });
        track.setOnClickListener(v -> {
            Intent targetIntent = new Intent(Profile.this, Curve.class);
            startActivity(targetIntent);
        });

    }

    // Download a file directly from a URL
    private void downloadFile(String fileUrl, String fileName) {
        try {
            // Get the DownloadManager system service
            android.app.DownloadManager downloadManager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            // Create a request for the file download
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(fileUrl));
            request.setTitle(fileName); // Title shown in notification
            request.setDescription("Downloading file..."); // Description in notification
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // Show notification when done
            request.setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_DOWNLOADS, fileName); // Save file to downloads directory

            // Enqueue the download
            downloadManager.enqueue(request);

            // Notify the user that the download has started
            Toast.makeText(this, "Downloading file...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Download failed.", Toast.LENGTH_SHORT).show();
        }
    }
    private void resetStartDate() {
        startDate = null;
        endDate = null;
        calendarView.setEnabled(true);
        StyleableToast.makeText(this, "Start date reset. Select a new date.", R.style.mytoast).show();
        saveStateToFirebase();
    }
    private void loadStateFromFirebase() {
        if (currentUser != null) {
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    selectedSite = documentSnapshot.getString("selectedSite");
                    selectedCategory = documentSnapshot.getString("selectedCategory");
                    currentAvailableSlots = documentSnapshot.getLong("currentAvailableSlots").intValue();
                    long startDateMillis = documentSnapshot.getLong("startDate");
                    long endDateMillis = documentSnapshot.getLong("endDate");

                    // Update UI with loaded state
                    pickedSiteName.setText("Selected Site: " + selectedSite);
                    availableSlots.setText("Slots: " + currentAvailableSlots);
                    if (startDateMillis != 0) {
                        startDate = new Date(startDateMillis);
                    }
                    if (endDateMillis != 0) {
                        endDate = new Date(endDateMillis);
                    }
                    // Highlight dates
                    if (startDate != null) {
                        calendarView.setDate(startDate.getTime(), true, true);
                    }
                    if (endDate != null) {
                        calendarView.setDate(endDate.getTime(), true, true);
                    }
                    initializeCountdown(startDate);
                    updateCountdownUI();
                    showSiteDetails();
                } else {
                    hideSiteDetails();
                    Toast.makeText(this, "No saved site.", Toast.LENGTH_SHORT).show();

                }
            }).addOnFailureListener(e -> Toast.makeText(this, "Error loading state.", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveStateToFirebase() {
        if (userRef == null || selectedSite == null) {
            Toast.makeText(this, "Incomplete data. Cannot save state.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> state = new HashMap<>();
        state.put("selectedSite", selectedSite);
        state.put("currentAvailableSlots", currentAvailableSlots);
        state.put("startDate", startDate.getTime());
        state.put("endDate", endDate.getTime());
        state.put("selectedCategory", selectedCategory);

        userRef.set(state)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "State saved successfully.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save state: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

                                // Locate the `members` subcollection
                                DocumentReference memberRef = siteRef
                                        .collection("members")
                                        .document(currentUser.getUid());

                                // Start a Firestore batch
                                firestore.runBatch(batch -> {
                                    // Update site slot count
                                    batch.update(siteRef, "availableSlots", currentAvailableSlots + 1);

                                    // Delete from UserStates
                                    batch.delete(userRef);

                                    // Delete from UserSelections
                                    DocumentReference userSelectionRef = firestore.collection("UserSelections")
                                            .document(currentUser.getUid());
                                    batch.delete(userSelectionRef);

                                    // Delete user from the `members` subcollection
                                    batch.delete(memberRef);
                                }).addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Deleted successfully.", Toast.LENGTH_SHORT).show();
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
        } else {
            // If user deletes using loadFirebase
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
                                                    // Locate the `members` subcollection
                                                    DocumentReference memberRef = siteRef
                                                            .collection("members")
                                                            .document(currentUser.getUid());

                                                    // Perform batch operations
                                                    firestore.runBatch(batch -> {
                                                        // Increment available slots
                                                        batch.update(siteRef, "availableSlots", availableSlots + 1);

                                                        // Delete from UserSelections
                                                        DocumentReference userSelectionsRef = firestore.collection("UserSelections")
                                                                .document(currentUser.getUid());
                                                        batch.delete(userSelectionsRef);

                                                        // Delete from UserStates
                                                        DocumentReference userStatesRef = firestore.collection("UserStates")
                                                                .document(currentUser.getUid());
                                                        batch.delete(userStatesRef);

                                                        // Delete user from the `members` subcollection
                                                        batch.delete(memberRef);
                                                    }).addOnSuccessListener(batchSuccess -> {
                                                        Toast.makeText(this, "You successfully dropped your selection.", Toast.LENGTH_SHORT).show();
                                                        Log.d("Debug", "Deleted successfully.");
                                                        hideSiteDetails();
                                                    }).addOnFailureListener(batchFailure -> {
                                                        Toast.makeText(this, "Failed to drop your selection: " + batchFailure.getMessage(), Toast.LENGTH_SHORT).show();
                                                        Log.e("Error", "Batch deletion failed: " + batchFailure.getMessage());
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
        if (start == null) {
            Toast.makeText(this, "Start date is not set.", Toast.LENGTH_SHORT).show();
            return;
        }

        this.startDate = start;

        // Calculate end date (10 weeks = 70 days)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        calendar.add(Calendar.DAY_OF_MONTH, 70);
        this.endDate = calendar.getTime();

        // Highlight start date and end date
        calendarView.setDate(start.getTime(), true, true);

        // Set the countdown values
        updateCountdownUI();

        // Save to Firestore
        saveStateToFirebase();
    }

    private void updateCountdownUI() {
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Please select a start date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar currentCalendar = Calendar.getInstance();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);

        // Calculate weeks and hours remaining
        long timeUntilEnd = endCalendar.getTimeInMillis() - currentCalendar.getTimeInMillis();
        int weeksRemaining = Math.max(0, (int) (timeUntilEnd / (1000 * 60 * 60 * 24 * 7)));
        int hoursRemaining = Math.max(0, weeksRemaining * HOURS_PER_WEEK);

        // Update UI
        weeksRemainingText.setText("Weeks Remaining: " + weeksRemaining);
        hoursRemainingtx.setText("Hours Remaining: " + hoursRemaining);

        if (weeksRemaining <= 0) {
            Toast.makeText(this, "Community service completed!", Toast.LENGTH_SHORT).show();
        }
    }

}
