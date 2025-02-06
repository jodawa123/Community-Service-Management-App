package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;
import nl.joery.animatedbottombar.AnimatedBottomBar;

public class Home extends AppCompatActivity {

    FirebaseFirestore firestore;
    ImageView profileImage;
    TextView textView6;
    private String selectedCategory;
    private Map<String, List<String>> categoryData;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String LOCATION_PROMPT_SHOWN = "LocationPromptShown";

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

        firestore = FirebaseFirestore.getInstance();
        profileImage = findViewById(R.id.profileImage);
        textView6 = findViewById(R.id.textView6);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Fetch the user's name from the database
        if (currentUser != null) {
            String userId = currentUser.getUid();
            fetchUserName(userId);
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            // Redirect to login screen
            Intent loginIntent = new Intent(Home.this, login.class);
            startActivity(loginIntent);
            finish();
        }

        ImageView imageHospice = findViewById(R.id.imageHospice);
        ImageView imageRehab = findViewById(R.id.imageRehab);
        ImageView imageHealth = findViewById(R.id.imageHealth);
        ImageView imageChildren = findViewById(R.id.imageChildren);
        ImageView imageRescue = findViewById(R.id.imageRescue);
        ImageView imageSpecial = findViewById(R.id.imageSpecial);
        ImageView searchIcon = findViewById(R.id.searchIcon);
        EditText searchBar = findViewById(R.id.searchBar);
        AnimatedBottomBar bottomBar = findViewById(R.id.bottom);

        imageHospice.setOnClickListener(v -> openCategory("Hospice"));
        imageSpecial.setOnClickListener(v -> openCategory("SpecialNeeds"));
        imageRehab.setOnClickListener(v -> openCategory("Rehab"));
        imageHealth.setOnClickListener(v -> openCategory("HealthCenters"));
        imageChildren.setOnClickListener(v -> openCategory("ChildrenHomes"));
        imageRescue.setOnClickListener(v -> openCategory("RescueCenter"));

        categoryData = new HashMap<>();
        loadAllData(() -> searchBar.setEnabled(true));

        searchIcon.setOnClickListener(v -> {
            String query = searchBar.getText().toString().toLowerCase();
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show();
            }
        });
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

        // Reset the location prompt flag when the activity is created
        resetLocationPromptShown();

        // Show the location prompt every time the activity is created
        checkLocationSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Reset the location prompt flag when the activity is destroyed
        resetLocationPromptShown();
    }

    private void fetchUserName(String userId) {
        databaseReference.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) {
                    textView6.setText(name);
                } else {
                    textView6.setText("User");
                    Toast.makeText(this, "Name not found in database", Toast.LENGTH_SHORT).show();
                }
            } else {
                textView6.setText("User");
                Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
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
                    return;
                }
            }
        }

        if (!found) {
            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
        }
    }

    private void highlightSite(String category, String site, int position) {
        Toast.makeText(this, "Found in " + category + ": " + site, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Home.this, Recycle.class);
        intent.putExtra("CATEGORY_NAME", category);
        intent.putExtra("SITE_NAME", site);
        intent.putExtra("SITE_POSITION", position);
        startActivity(intent);
    }

    private void handleTabSelection(int newIndex) {
        switch (newIndex) {
            case 0:
                // Redirect to Profile page
                Intent profileIntent = new Intent(Home.this, Profile.class);
                startActivity(profileIntent);
                break;

            case 1:
                // Redirect to Home page
                // Optional: Do nothing if it's the same page
                break;

            case 2:
                // Redirect to Maps page
                Intent mapsIntent = new Intent(Home.this, Mapping.class);
                startActivity(mapsIntent);
                break;

            default:
                // Handle unexpected index
                Toast.makeText(Home.this, "Unknown Tab Selected", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void handleTabReselection(int index) {
        // Handle tab reselection logic if needed
        Toast.makeText(Home.this, "Tab Reselected: " + index, Toast.LENGTH_SHORT).show();
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(builder.build())
                .addOnCompleteListener(task -> {
                    try {
                        task.getResult(ApiException.class);
                    } catch (ApiException e) {
                        if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                            try {
                                ((ResolvableApiException) e).startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                            } catch (Exception ignored) {}
                        }
                    }
                });
    }

    private boolean hasLocationPromptBeenShown() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(LOCATION_PROMPT_SHOWN, false);
    }

    private void saveLocationPromptShown() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(LOCATION_PROMPT_SHOWN, true).apply();
    }

    private void resetLocationPromptShown() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(LOCATION_PROMPT_SHOWN, false).apply();
    }
}