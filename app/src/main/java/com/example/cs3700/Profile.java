package com.example.cs3700;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import io.github.muddz.styleabletoast.StyleableToast;

public class Profile extends AppCompatActivity {

    private TextView pickedSiteName, availableSlots, pick,documents_header,doc1_title;
    private FirebaseFirestore firestore;
    private String selectedCategory, selectedSite;
    private int currentAvailableSlots;
    private View siteSection;
    private CalendarView calendarView;
    private static final int TOTAL_REQUIRED_HOURS = 90;
    private static final int HOURS_PER_WEEK = 9;
    private Date startDate, endDate;
    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private ImageView imageView4, down;
    private Button resetDateButton, track;
    private TextToSpeech textToSpeech;
    private FloatingActionButton ttsButton;
    private boolean isTTSActive = false; // Tracks whether TTS is active

    private List<TextItem> textItems = new ArrayList<>(); // List to store text items
    private int currentIndex = 0; // Track the current text item being read

    private static class TextItem {
        View view; // Can be TextView or EditText
        String text;

        TextItem(View view, String text) {
            this.view = view;
            this.text = text;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Disable page number announcements by hiding the action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        //Ensure the title is not spoken by TalkBack
        setTitle(" ");

        // Only hide ActionBar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    showToast("Language not supported");
                }
            } else {
                showToast("Text-to-Speech initialization failed");
            }
        });

        Animation ani = AnimationUtils.loadAnimation(this, R.anim.pulsating_animation);
        pick = findViewById(R.id.pick);
        pick.setAnimation(ani);

        // Initialize Views
        pickedSiteName = findViewById(R.id.picked_site_name);
        availableSlots = findViewById(R.id.available_slots);
        RadioButton dropSiteButton = findViewById(R.id.drop_site_button);
        calendarView = findViewById(R.id.calendarView);
        imageView4 = findViewById(R.id.imageView4);
        down = findViewById(R.id.doc1_download);
        resetDateButton = findViewById(R.id.bt);
        track = findViewById(R.id.bt1);
        siteSection = findViewById(R.id.site_section);
        documents_header = findViewById(R.id.documents_header);
        doc1_title = findViewById(R.id.doc1_title);

        // Initialize TTS Button
        ttsButton = findViewById(R.id.ttsButton);
        ttsButton.setOnClickListener(v -> activateTTS());

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
                showToastAndAnnounce("Start date set to: " + startDate);
            } else {
                showToastAndAnnounce("You already have a start date.");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    // Show Toast and announce for accessibility
    private void showToastAndAnnounce(String message) {
        // Show Toast for all users
        showToast(message);

        // Check if TalkBack (or another screen reader) is active
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

    }

    // Show Toast message
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
            showToastAndAnnounce("Downloading file...");
        } catch (Exception e) {
            e.printStackTrace();
            showToastAndAnnounce("Download failed.");
        }
    }

    private void resetStartDate() {
        startDate = null;
        endDate = null;
        calendarView.setEnabled(true);
        showToastAndAnnounce("Start date reset. Select a new date.");
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
        state.put("startDate", startDate != null ? startDate.getTime() : null);
        state.put("endDate", endDate != null ? endDate.getTime() : null);
        state.put("selectedCategory", selectedCategory);

        /*userRef.set(state)
                .addOnSuccessListener(aVoid -> showToastAndAnnounce("State saved successfully."))
                .addOnFailureListener(e -> showToastAndAnnounce("Failed to save state: " + e.getMessage()));*/
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
                        showToastAndAnnounce("Site details not found.");
                    }
                })
                .addOnFailureListener(e -> showToastAndAnnounce("Error fetching site details."));
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
                                    showToastAndAnnounce("Deleted successfully.");
                                    hideSiteDetails();
                                    selectedSite = null; // Clear the current site
                                }).addOnFailureListener(e -> {
                                    showToastAndAnnounce("Failed to complete drop operation: " + e.getMessage());
                                });
                            });
                        } else {
                            showToastAndAnnounce("Site not found.");
                        }
                    })
                    .addOnFailureListener(e -> showToastAndAnnounce("Error fetching site details."));
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
                                                        showToastAndAnnounce("You successfully dropped your selection.");
                                                        hideSiteDetails();
                                                    }).addOnFailureListener(batchFailure -> {
                                                        showToastAndAnnounce("Failed to drop your selection: " + batchFailure.getMessage());
                                                    });
                                                } else {
                                                    showToastAndAnnounce("Invalid available slots data for the site.");
                                                }
                                            } else {
                                                showToastAndAnnounce("No matching site found in the selected category.");
                                            }
                                        })
                                        .addOnFailureListener(queryFailure -> {
                                            showToastAndAnnounce("Error querying site: " + queryFailure.getMessage());
                                        });
                            } else {
                                showToastAndAnnounce("No site or category found in your selection.");
                            }
                        } else {
                            showToastAndAnnounce("No selection found for the user.");
                        }
                    })
                    .addOnFailureListener(fetchFailure -> {
                        showToastAndAnnounce("Failed to fetch user selection: " + fetchFailure.getMessage());
                    });
        }
    }

    private void showSiteDetails() {
        siteSection.setVisibility(View.VISIBLE);
        calendarView.setVisibility(View.VISIBLE);
        pick.setVisibility(View.VISIBLE);
    }

    private void hideSiteDetails() {
        siteSection.setVisibility(View.GONE);
        calendarView.setVisibility(View.GONE);
        pick.setVisibility(View.GONE);
    }

    private void initializeCountdown(Date start) {
        if (start == null) {
            showToastAndAnnounce("Start date is not set.");
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
            showToastAndAnnounce("Please select a start date first.");
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

        if (weeksRemaining <= 0) {
            showToastAndAnnounce("Community service completed! 🏁");
        }
    }

    // Activate TTS to read screen content
    private void activateTTS() {
        if (isTTSActive) {
            // If TTS is active, stop it
            if (textToSpeech != null) {
                textToSpeech.stop(); // Stop speaking
            }
            isTTSActive = false;
            ttsButton.setImageResource(R.drawable.baseline_mic_off_24); // Change button icon to indicate TTS is off
            showToastAndAnnounce("Text-to-Speech stopped.");
        } else {
            // If TTS is not active, start it
            // Clear the list
            textItems.clear();
            currentIndex = 0; // Reset the index

            // Collect text from various components
            textItems.add(new TextItem(pickedSiteName, pickedSiteName.getText().toString()));
            textItems.add(new TextItem(availableSlots, availableSlots.getText().toString()));
            textItems.add(new TextItem(documents_header, documents_header.getText().toString()));
            textItems.add(new TextItem(doc1_title, doc1_title.getText().toString()));
            textItems.add(new TextItem(pick, pick.getText().toString()));

            // Start reading the text
            if (!textItems.isEmpty()) {
                isTTSActive = true;
                ttsButton.setImageResource(R.drawable.baseline_mic_24); // Change button icon to indicate TTS is on
                readTextItem(currentIndex);
            } else {
                showToastAndAnnounce("No text to read");
            }
        }
    }

    // Read and highlight text
    private void readTextItem(int index) {
        if (index >= textItems.size()) {
            isTTSActive = false; // Reset the flag when done
            ttsButton.setImageResource(R.drawable.baseline_mic_off_24); // Update button icon
            showToastAndAnnounce("Finished reading screen content.");
            return;
        }

        TextItem textItem = textItems.get(index);
        View view = textItem.view;
        String text = textItem.text;

        // Highlight the text
        runOnUiThread(() -> highlightText(view, text));

        // Set up UtteranceProgressListener
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // No action needed
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    // Reset the text appearance
                    resetTextAppearance(view);

                    // Move to the next text item
                    currentIndex++;
                    readTextItem(currentIndex);
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> showToastAndAnnounce("Error reading text"));
            }
        });

        // Speak the text
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId" + index);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utteranceId" + index);
    }

    // Highlight the text in its original component
    private void highlightText(View view, String text) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(
                    new BackgroundColorSpan(Color.GRAY), // Highlight color
                    0, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            textView.setText(spannableString);
        }
    }

    // Reset the text appearance
    private void resetTextAppearance(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setText(textItems.get(currentIndex).text); // Restore original text
        }
    }
}