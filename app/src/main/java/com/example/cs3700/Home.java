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
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Home extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private TextView textView6;
    private Map<String, List<String>> categoryData;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private ViewPager2 imageSlider;
    private TabLayout dotsIndicator;
    private TextToSpeech textToSpeech;
    private List<Integer> imageList = Arrays.asList(
            R.drawable.ones,
            R.drawable.twos,
            R.drawable.threes,
            R.drawable.four
    );
    private List<TextItem> textItems = new ArrayList<>();
    private int currentIndex = 0; // Track the current text item being read
    private boolean isTTSActive = false; // Track TTS state
    private FloatingActionButton ttsButton; // Declare ttsButton as a class variable

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
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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


        // Initialize Firebase and UI components
        firestore = FirebaseFirestore.getInstance();
        textView6 = findViewById(R.id.textView6);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        dotsIndicator = findViewById(R.id.dotsIndicator);
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    showToast("Language not supported");
                } else {
                    // Set the speech rate (e.g., 1.0 is normal speed, 0.5 is slower, 2.0 is faster)
                    textToSpeech.setSpeechRate(1.0f); // Default speed
                }
            } else {
                showToast("Text-to-Speech initialization failed");
            }
        });

        // Fetch the user's name from the database
        if (currentUser != null) {
            String userId = currentUser.getUid();
            fetchUserName(userId);
        } else {
            showToast("User not logged in");
            // Redirect to login screen
            Intent loginIntent = new Intent(Home.this, login.class);
            startActivity(loginIntent);
            finish();
        }

        // Initialize UI components
        ImageView imageHospice = findViewById(R.id.imageHospice);
        ImageView imageRehab = findViewById(R.id.imageRehab);
        ImageView imageHealth = findViewById(R.id.imageHealth);
        ImageView imageChildren = findViewById(R.id.imageChildren);
        ImageView imageRescue = findViewById(R.id.imageRescue);
        ImageView imageSpecial = findViewById(R.id.imageSpecial);
        ImageView searchIcon = findViewById(R.id.searchIcon);
        EditText searchBar = findViewById(R.id.searchBar);
        BottomNavigationView bottomBar = findViewById(R.id.bottomNavigationView);

        // TTS Button
        ttsButton = findViewById(R.id.ttsButton); // Initialize ttsButton
        ttsButton.setOnClickListener(v -> {
            if (isTTSActive) {
                // If TTS is active, stop it
                textToSpeech.stop(); // Stop speaking
                isTTSActive = false;
                ttsButton.setImageResource(R.drawable.baseline_mic_off_24); // Change button icon to indicate TTS is off
                showToast("Text-to-Speech stopped.");
            } else {
                // If TTS is not active, start it
                activateTTS(); // Start reading text
                isTTSActive = true;
                ttsButton.setImageResource(R.drawable.baseline_mic_24); // Change button icon to indicate TTS is on
            }
        });

        // Set click listeners for category images
        imageHospice.setOnClickListener(v -> openCategory("Hospice"));
        imageSpecial.setOnClickListener(v -> openCategory("SpecialNeeds"));
        imageRehab.setOnClickListener(v -> openCategory("Rehab"));
        imageHealth.setOnClickListener(v -> openCategory("HealthCenters"));
        imageChildren.setOnClickListener(v -> openCategory("ChildrenHomes"));
        imageRescue.setOnClickListener(v -> openCategory("RescueCenter"));

        // Load data for search functionality
        categoryData = new HashMap<>();
        loadAllData(() -> searchBar.setEnabled(true));

        // Search functionality
        searchIcon.setOnClickListener(v -> {
            String query = searchBar.getText().toString().toLowerCase();
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                announceOrToast("Enter a search term");
            }
        });

        // Bottom Navigation
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_maps) {
                // Handle Maps tab selection
                Intent mapsIntent = new Intent(Home.this, Mapping.class);
                startActivity(mapsIntent);
                announceOrToast("Opening Maps");
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Handle Profile tab selection
                Intent profileIntent = new Intent(Home.this, Profile.class);
                startActivity(profileIntent);
                announceOrToast("Opening Profile");
                return true;
            } else {
                // Handle unexpected tab selection
                announceOrToast("Unknown tab selected");
                return false;
            }
        });

        // Initialize the image slider
        imageSlider = findViewById(R.id.imageSlider);
        ImageSliderAdapter adapter = new ImageSliderAdapter(this, imageList);
        imageSlider.setAdapter(adapter);

        // Attach the dots indicator to the ViewPager2
        new TabLayoutMediator(dotsIndicator, imageSlider, (tab, position) -> {}).attach();

        // Auto-slide images
        imageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Schedule the next slide with a delay
                imageSlider.postDelayed(() -> {
                    int nextPosition = (position + 1) % imageList.size();
                    imageSlider.setCurrentItem(nextPosition, true);
                }, 5000); // Slide every 5 seconds
            }
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

    // Show text as a Toast message
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Announce text using TTS only if TalkBack is active, otherwise show Toast
    private void announceOrToast(String message) {
        // Show Toast by default
        showToast(message);

        // Check if TalkBack (or another screen reader) is active
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

    }

    private void fetchUserName(String userId) {
        databaseReference.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) {
                    textView6.setText(name);
                }
            } else {
                textView6.setText("User");
                announceOrToast("Failed to fetch user data");
            }
        });
    }

    private void openCategory(String categoryName) {
        Intent intent = new Intent(Home.this, Recycle.class);
        intent.putExtra("CATEGORY_NAME", categoryName);
        startActivity(intent);
    }

    private void loadAllData(Runnable onDataLoaded) {
        String[] categories = {"HealthCenters", "ChildrenHomes", "Hospice", "Rehab", "RescueCenter", "SpecialNeeds"};
        int[] remainingCategories = {categories.length};

        for (String category : categories) {
            firestore.collection(category).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<String> sites = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String head = document.getString("head");
                        if (head != null) {
                            sites.add(head.toLowerCase());
                        }
                    }
                    categoryData.put(category, sites);
                }
                remainingCategories[0]--;
                if (remainingCategories[0] == 0) {
                    onDataLoaded.run();
                }
            });
        }
    }

    private void performSearch(String query) {
        boolean found = false;

        for (Map.Entry<String, List<String>> entry : categoryData.entrySet()) {
            String category = entry.getKey();
            List<String> sites = entry.getValue();

            for (int i = 0; i < sites.size(); i++) {
                String site = sites.get(i);
                if (site.contains(query)) {
                    found = true;
                    highlightSite(category, site, i);
                    announceOrToast("Found in " + category + ": " + site);
                    return;
                }
            }
        }

        if (!found) {
            announceOrToast("No results found");
        }
    }

    private void highlightSite(String category, String site, int position) {
        Intent intent = new Intent(Home.this, Recycle.class);
        intent.putExtra("CATEGORY_NAME", category);
        intent.putExtra("SITE_NAME", site);
        intent.putExtra("SITE_POSITION", position);
        startActivity(intent);
    }

    private void activateTTS() {
        // Clear the list
        textItems.clear();
        currentIndex = 0; // Reset the index

        // Collect text from various components
        TextView greetingText = findViewById(R.id.textView5);
        TextView userNameText = findViewById(R.id.textView6);
        EditText searchBarText = findViewById(R.id.searchBar);
        TextView categoriesTitleText = findViewById(R.id.categoriesTitle);

        // Read EditText hint if empty
        String searchText = searchBarText.getText().toString().trim();
        if (searchText.isEmpty()) {
            searchText = searchBarText.getHint().toString(); // Read hint instead
        }

        // Add text items to the list
        textItems.add(new TextItem(greetingText, greetingText.getText().toString()));
        textItems.add(new TextItem(userNameText, userNameText.getText().toString()));
        textItems.add(new TextItem(searchBarText, searchText));
        textItems.add(new TextItem(categoriesTitleText, categoriesTitleText.getText().toString()));

        // Add text from category items
        GridLayout categoriesGrid = findViewById(R.id.categoriesGrid);
        for (int i = 0; i < categoriesGrid.getChildCount(); i++) {
            View child = categoriesGrid.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout categoryItem = (LinearLayout) child;
                for (int j = 0; j < categoryItem.getChildCount(); j++) {
                    View innerChild = categoryItem.getChildAt(j);
                    if (innerChild instanceof TextView) {
                        TextView categoryText = (TextView) innerChild;
                        textItems.add(new TextItem(categoryText, categoryText.getText().toString()));
                    }
                }
            }
        }

        // Start reading the text
        if (!textItems.isEmpty()) {
            readTextItem(currentIndex);
        } else {
            announceOrToast("No text to read");
        }
    }

    private void readTextItem(int index) {
        if (index >= textItems.size()) {
            isTTSActive = false; // Reset TTS state when done
            ttsButton.setImageResource(R.drawable.baseline_mic_off_24); // Change button icon to indicate TTS is off
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
                runOnUiThread(() -> showToast("Error reading text"));
            }
        });

        // Speak the text
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId" + index);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utteranceId" + index);
    }

    private void highlightText(View view, String text) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(
                    new BackgroundColorSpan(Color.YELLOW), // Highlight color
                    0, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            textView.setText(spannableString);
        } else if (view instanceof EditText) {
            EditText editText = (EditText) view;
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(
                    new BackgroundColorSpan(Color.YELLOW), // Highlight color
                    0, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            editText.setText(spannableString);
        }
    }

    private void resetTextAppearance(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setText(textItems.get(currentIndex).text); // Restore original text
        } else if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setText(textItems.get(currentIndex).text);
        }
    }
}