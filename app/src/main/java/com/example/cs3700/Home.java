package com.example.cs3700;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.Map;

public class Home extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private TextView textView6;
    private String selectedSite; // Stores the selected site name
    private String selectedCategory, locs; // Stores the selected category name
    private Map<String, List<String>> categoryData;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private ViewPager2 imageSlider;
    private TabLayout dotsIndicator;
    private FloatingActionButton home;
    private List<Integer> imageList = Arrays.asList(
            R.drawable.ones,
            R.drawable.twos,
            R.drawable.threes,
            R.drawable.four
    );

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_LOCATION_PERMISSION_REQUESTED = "locationPermissionRequested";

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
        // Ensure the title is not spoken by TalkBack
        setTitle(" ");

        // Get the value of `locs` from the intent
        locs = getIntent().getStringExtra("one");

        // Initialize Firebase and UI components
        firestore = FirebaseFirestore.getInstance();
        textView6 = findViewById(R.id.textView6);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        dotsIndicator = findViewById(R.id.dotsIndicator);
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        home = findViewById(R.id.homebutton); // Initialize the FloatingActionButton

        // Fetch the user's name from the database
        if (currentUser != null) {
            String userId = currentUser.getUid();
            fetchUserName(userId);
        } else {
            announceOrToast("User not logged in");
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
                profileIntent.putExtra("SELECTED_SITE", selectedSite); // Pass the selected site
                profileIntent.putExtra("CATEGORY_NAME", selectedCategory); // Pass the selected category
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
                }, 3000); // Slide every 3 seconds
            }
        });

        home.setOnClickListener(view -> announceOrToast("Home"));

        // Show location permission alert if `locs` equals "yes"
        if ("yes".equals(locs)) {
            showLocationPermissionAlert();
        }
    }

    // Show an alert dialog to request location permission
    private void showLocationPermissionAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("This app requires location permission to verify your presence. Would you like to enable it?")
                .setPositiveButton("Yes", (dialog, which) -> requestLocationPermission())
                .setNegativeButton("No", (dialog, which) -> {
                    // Handle the case where the user denies the permission
                    announceOrToast("Location permission is required for full functionality.");
                })
                .setCancelable(false) // Prevent dismissing the dialog by tapping outside
                .show();
    }

    // Request location permission
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            announceOrToast("Location permission already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                announceOrToast("Location permission granted");
            } else {
                announceOrToast("Location permission denied");
            }
        }
    }

    // Show text as a Toast message or announce it using TalkBack
    private void announceOrToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
                    highlightSite(category, site, i, query); // Pass the query to highlightSite
                    announceOrToast("Found in " + category + ": " + site);
                    return;
                }
            }
        }

        if (!found) {
            announceOrToast("No results found");
        }
    }

    private void highlightSite(String category, String site, int position, String query) {
        Intent intent = new Intent(Home.this, Recycle.class);
        intent.putExtra("CATEGORY_NAME", category);
        intent.putExtra("SITE_NAME", site);
        intent.putExtra("SITE_POSITION", position);
        intent.putExtra("SEARCH_QUERY", query); // Pass the search query
        startActivity(intent);
    }
}