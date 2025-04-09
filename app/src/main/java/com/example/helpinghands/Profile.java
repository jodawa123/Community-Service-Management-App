package com.example.helpinghands;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.SharedPreferences;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.joery.animatedbottombar.AnimatedBottomBar;

public class Profile extends AppCompatActivity {

    private TextView pickedSiteName, availableSlots, documents_header, doc1_title;
    private FirebaseFirestore firestore;
    private String selectedCategory, selectedSite, userId;
    private int currentAvailableSlots;
    private View siteSection;
    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private ImageView imageView4, down;
    private Button track, rate;
    private AnimatedBottomBar bottomBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Disable page number announcements by hiding the action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Ensure the title is not spoken by TalkBack
        setTitle(" ");

        // Initialize Views
        pickedSiteName = findViewById(R.id.picked_site_name);
        availableSlots = findViewById(R.id.available_slots);
        RadioButton dropSiteButton = findViewById(R.id.drop_site_button);
        imageView4 = findViewById(R.id.imageView4);
        down = findViewById(R.id.doc1_download);
        track = findViewById(R.id.bt1);
        siteSection = findViewById(R.id.site_section);
        documents_header = findViewById(R.id.documents_header);
        doc1_title = findViewById(R.id.doc1_title);
        bottomBar = findViewById(R.id.bottom);
        rate=findViewById(R.id.bt2);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = (currentUser != null) ? currentUser.getUid() : null;

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
            showToastAndAnnounce("User not logged in");
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

        dropSiteButton.setOnClickListener(v -> dropSelectedSite());

        down.setOnClickListener(view -> {
            String fileUrl = "https://drive.google.com/uc?id=1t6YyMTxAI2IE14wcye737s2vIsde0r0H&export=download";
            String fileName = "COMMUNITY.docx";
            downloadFile(fileUrl, fileName);
        });

        track.setOnClickListener(v -> {
            Intent targetIntent = new Intent(Profile.this, daily_Diary.class);
            startActivity(targetIntent);
        });
       rate.setOnClickListener(v -> {
            Intent targetIntent = new Intent(Profile.this, Rating.class);
            startActivity(targetIntent);
        });


        calculateTotals();
        scheduleNotifications(); // Set up periodic checks
        calculateTotals();

        // Set up the AnimatedBottomBar listener
        bottomBar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, AnimatedBottomBar.Tab lastTab, int newIndex, AnimatedBottomBar.Tab newTab) {
                handleTabSelection(newIndex);
            }

            @Override
            public void onTabReselected(int index, AnimatedBottomBar.Tab tab) {
                handleTabReselection(index);
            }
        });
    }
    private void handleTabSelection(int newIndex) {
        switch (newIndex) {
            case 0:
                // Redirect to Profile page
                Intent profileIntent = new Intent(Profile.this, Home.class);
                startActivity(profileIntent);
                break;

            case 1:
                // Optional: Do nothing if it's the same page
                break;


            case 2:

                // Redirect to Maps page
                Intent mapsIntent = new Intent(Profile.this, Mapping.class);
                startActivity(mapsIntent);
                break;


            case 3:
                // Redirect to Leaderboard page
                Intent boardIntent = new Intent(Profile.this, LeaderboardActivity.class);
                startActivity(boardIntent);
                break;


            default:
                // Handle unexpected index
                Toast.makeText(Profile.this, "Unknown Tab Selected", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    private void handleTabReselection(int index) {
        // Handle tab reselection logic if needed
        Toast.makeText(Profile.this, "Tab Reselected: " + index, Toast.LENGTH_SHORT).show();
    }

    private void refreshData() {
        // Refresh all data from Firebase
        loadStateFromFirebase();
        calculateTotals();
    }

    // Show Toast and announce for accessibility
    private void showToastAndAnnounce(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Download a file directly from a URL
    private void downloadFile(String fileUrl, String fileName) {
        try {
            android.app.DownloadManager downloadManager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(fileUrl));
            request.setTitle(fileName);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
            downloadManager.enqueue(request);
            showToastAndAnnounce("Downloading file...");
        } catch (Exception e) {
            e.printStackTrace();
            showToastAndAnnounce("Download failed.");
        }
    }

    private void loadStateFromFirebase() {
        if (currentUser != null) {
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    selectedSite = documentSnapshot.getString("selectedSite");
                    selectedCategory = documentSnapshot.getString("selectedCategory");
                    currentAvailableSlots = documentSnapshot.getLong("currentAvailableSlots").intValue();

                    // Update UI with loaded state
                    pickedSiteName.setText("Selected Site: " + selectedSite);
                    availableSlots.setText("Slots: " + currentAvailableSlots);
                    showSiteDetails();
                } else {
                    hideSiteDetails();
                    showToastAndAnnounce("No saved site.");
                }
            }).addOnFailureListener(e -> showToastAndAnnounce("Error loading state."));
        }
    }

    private void saveStateToFirebase() {
        if (userRef == null || selectedSite == null) {
            showToastAndAnnounce("Incomplete data. Cannot save state.");
            return;
        }

        Map<String, Object> state = new HashMap<>();
        state.put("selectedSite", selectedSite);
        state.put("currentAvailableSlots", currentAvailableSlots);
        state.put("selectedCategory", selectedCategory);

        userRef.set(state)
                .addOnSuccessListener(aVoid -> showToastAndAnnounce("State saved successfully."))
                .addOnFailureListener(e -> showToastAndAnnounce("Failed to save state: " + e.getMessage()));
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
                            saveStateToFirebase();
                            showSiteDetails();
                        });
                    } else {
                        showToastAndAnnounce("Site details not found.");
                    }
                })
                .addOnFailureListener(e -> showToastAndAnnounce("Error fetching site details."));
    }

    private void dropSelectedSite() {
        if (selectedCategory != null) {
            firestore.collection(selectedCategory)
                    .whereEqualTo("head", selectedSite)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            querySnapshot.getDocuments().forEach(document -> {
                                DocumentReference siteRef = document.getReference();
                                DocumentReference memberRef = siteRef
                                        .collection("members")
                                        .document(currentUser.getUid());

                                // First delete the logs, then proceed with other deletions
                                deleteDailyLogs().addOnCompleteListener(deleteTask -> {
                                    if (deleteTask.isSuccessful()) {
                                        firestore.runBatch(batch -> {
                                            batch.update(siteRef, "availableSlots", currentAvailableSlots + 1);
                                            batch.delete(userRef);
                                            DocumentReference userSelectionRef = firestore.collection("UserSelections")
                                                    .document(currentUser.getUid());
                                            batch.delete(userSelectionRef);
                                            batch.delete(memberRef);
                                        }).addOnSuccessListener(aVoid -> {
                                            showToastAndAnnounce("Deleted successfully.");
                                            recreate();
                                        }).addOnFailureListener(e -> {
                                            showToastAndAnnounce("Failed to complete drop operation: " + e.getMessage());
                                        });
                                    } else {
                                        showToastAndAnnounce("Failed to delete logs: " + deleteTask.getException().getMessage());
                                    }
                                });
                            });
                        } else {
                            showToastAndAnnounce("Site not found.");
                        }
                    })
                    .addOnFailureListener(e -> showToastAndAnnounce("Error fetching site details."));
        } else {
            firestore.collection("UserSelections")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String selectedSite = documentSnapshot.getString("selectedSite");
                            String selectedCategory = documentSnapshot.getString("selectedCategory");

                            if (selectedSite != null && selectedCategory != null) {
                                firestore.collection(selectedCategory)
                                        .whereEqualTo("head", selectedSite)
                                        .get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            if (!querySnapshot.isEmpty()) {
                                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                                DocumentReference siteRef = document.getReference();
                                                Long availableSlots = document.getLong("availableSlots");

                                                if (availableSlots != null) {
                                                    DocumentReference memberRef = siteRef
                                                            .collection("members")
                                                            .document(currentUser.getUid());

                                                    // First delete the logs, then proceed with other deletions
                                                    deleteDailyLogs().addOnCompleteListener(deleteTask -> {
                                                        if (deleteTask.isSuccessful()) {
                                                            firestore.runBatch(batch -> {
                                                                batch.update(siteRef, "availableSlots", availableSlots + 1);
                                                                DocumentReference userSelectionsRef = firestore.collection("UserSelections")
                                                                        .document(currentUser.getUid());
                                                                batch.delete(userSelectionsRef);
                                                                DocumentReference userStatesRef = firestore.collection("UserStates")
                                                                        .document(currentUser.getUid());
                                                                batch.delete(userStatesRef);
                                                                batch.delete(memberRef);
                                                            }).addOnSuccessListener(batchSuccess -> {
                                                                showToastAndAnnounce("You successfully dropped your selection.");
                                                                recreate();
                                                            }).addOnFailureListener(batchFailure -> {
                                                                showToastAndAnnounce("Failed to drop your selection: " + batchFailure.getMessage());
                                                            });
                                                        } else {
                                                            showToastAndAnnounce("Failed to delete logs: " + deleteTask.getException().getMessage());
                                                        }
                                                    });
                                                }
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    private Task<Void> deleteDailyLogs() {
        if (userId == null) {
            Log.e(TAG, "User ID is null. Cannot delete logs.");
            Tasks.forException(new NullPointerException("User ID is null"));
        }

        return firestore.collection("daily_logs").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully deleted logs document"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting logs document", e));
    }
    private void showSiteDetails() {
        siteSection.setVisibility(View.VISIBLE);
    }

    private void hideSiteDetails() {
        siteSection.setVisibility(View.GONE);
    }
    private void calculateTotals() {
        if (userId == null) {
            Log.e(TAG, "User ID is null. Cannot calculate totals.");
            return;
        }

        firestore.collection("daily_logs").document(userId).collection("logs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalDays = queryDocumentSnapshots.size();
                    final int[] totalHours = {0};
                    Set<Integer> weeks = new HashSet<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String hoursStr = doc.getString("hours");
                        if (hoursStr != null && !hoursStr.isEmpty()) {
                            totalHours[0] += Integer.parseInt(hoursStr);
                        }

                        String dateStr = doc.getString("date");
                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(sdf.parse(dateStr));
                                int week = calendar.get(Calendar.WEEK_OF_YEAR);
                                weeks.add(week);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing date: " + dateStr, e);
                            }
                        }
                    }

                    int totalWeeks = weeks.size();

                    int notificationWeek = Math.min(totalWeeks + 1, NotificationWorker.weeklyMessages.length);

                    SharedPreferences prefs = getSharedPreferences("HelpingHandsPrefs", MODE_PRIVATE);
                    int lastNotifiedWeek = prefs.getInt("last_notified_week", 0);

                    // Update week counters
                    prefs.edit()
                            .putInt("current_week", notificationWeek)
                            .apply();

                    runOnUiThread(() -> {
                        TextView weeksDoneTextView = findViewById(R.id.weeksText);
                        TextView daysDoneTextView = findViewById(R.id.daysText);
                        TextView hoursDoneTextView = findViewById(R.id.hoursText);

                        if (weeksDoneTextView != null && daysDoneTextView != null && hoursDoneTextView != null) {
                            weeksDoneTextView.setText(String.valueOf(totalWeeks));
                            daysDoneTextView.setText(String.valueOf(totalDays));
                            hoursDoneTextView.setText(String.valueOf(totalHours[0]));
                        }
                    });
                    // Trigger notification if we entered a new week
                    if (notificationWeek > lastNotifiedWeek) {
                        triggerNotificationCheck();
                        prefs.edit().putInt("last_notified_week", notificationWeek).apply();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error calculating totals", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching logs", e);
                });
    }


    private void scheduleNotifications() {
        PeriodicWorkRequest weeklyCheck = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                7, // Interval
                TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS) // First check tomorrow
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "weekly_encouragement",
                ExistingPeriodicWorkPolicy.UPDATE, // Update existing schedule
                weeklyCheck);
    }

    private void triggerNotificationCheck() {
        OneTimeWorkRequest checkRequest = new OneTimeWorkRequest.Builder(
                NotificationWorker.class)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(this).enqueue(checkRequest);
    }

}