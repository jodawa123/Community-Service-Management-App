package com.example.cs3700;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Verify extends AppCompatActivity {

    private static final String TAG = "Verify";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float ALLOWED_RADIUS_METERS = 100; // 100 meters radius
    private static final int WORK_START_HOUR = 7; // 7 AM
    private static final int WORK_END_HOUR = 15; // 3 PM
    private ProgressBar progressBar;
    private TextView titleText, errorText;
    private ImageView img,back;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler = new Handler();
    private String userId, selectedCategory, selectedSite;
    private double siteLatitude, siteLongitude;

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                showError("Unable to retrieve location.");
                return;
            }
            for (Location location : locationResult.getLocations()) {
                if (location != null) {
                    verifyLocation(location);
                    fusedLocationClient.removeLocationUpdates(this); // Stop updates after getting location
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify);

        initializeViews();
        initializeFirebase();
        checkLocationPermissions();


        back.setOnClickListener(view -> {
            Intent intent = new Intent(Verify.this, Home.class);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        titleText = findViewById(R.id.titleText);
        errorText = findViewById(R.id.headText);
        img = findViewById(R.id.hold);
        back=findViewById(R.id.imageView4);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : null;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (userId == null) {
            showToast("User not authenticated");
            finish();
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startVerification();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVerification();
            } else {
                showPermissionDeniedError();
            }
        }
    }

    private void showPermissionDeniedError() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("This app requires location permission to verify your presence. Please enable it in the app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void startVerification() {
        fetchSelectedSiteAndCategory();
    }

    private void fetchSelectedSiteAndCategory() {
        DocumentReference userStateRef = db.collection("UserStates").document(userId);
        userStateRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                selectedCategory = document.getString("selectedCategory");
                selectedSite = document.getString("selectedSite");

                if (selectedCategory != null && selectedSite != null) {
                    fetchSiteGeolocationAndVerify(selectedCategory, selectedSite);
                } else {
                    showError("No site or category selected.");
                }
            } else {
                showError("No site currently selected.");
            }
        }).addOnFailureListener(e -> showError("Error fetching user state"));
    }

    private void fetchSiteGeolocationAndVerify(String categoryName, String head) {
        db.collection(categoryName)
                .whereEqualTo("head", head)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        com.google.firebase.firestore.GeoPoint location = document.getGeoPoint("location");

                        if (location != null) {
                            siteLatitude = location.getLatitude();
                            siteLongitude = location.getLongitude();
                            verifyLocationAndTime();
                        } else {
                            showError("Site geolocation not found.");
                        }
                    } else {
                        showError("Site not found.");
                    }
                })
                .addOnFailureListener(e -> showError("Error fetching site geolocation"));
    }

    private void verifyLocationAndTime() {
        List<String> errors = new ArrayList<>();

        // Check if it's a weekend
        if (isWeekend()) {
            errors.add("1. Logs can only be done Monday to Friday.");
        }

        // Check if it's during work hours
        if (!isDuringWorkHours()) {
            errors.add("2. You can only log your diary during work hours (7 AM - 3 PM).");
        }

        // If there are errors, display them and stop further checks
        if (!errors.isEmpty()) {
            showError(errors);
            return;
        }

        // If no errors, proceed with location verification
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            showError("Location permission denied");
        }
    }

    private void verifyLocation(Location location) {
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), siteLatitude, siteLongitude, results);
        float distanceInMeters = results[0];

        boolean isLocationMet = distanceInMeters <= ALLOWED_RADIUS_METERS;

        if (isLocationMet) {
            showSuccess();
        } else {
            showError("3. You must be at the site to log your diary.");
        }
    }

    private boolean isWeekend() {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }

    private boolean isDuringWorkHours() {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return currentHour >= WORK_START_HOUR && currentHour < WORK_END_HOUR;
    }

    private void showError(List<String> errors) {
        runOnUiThread(() -> {
            // Stop the progress bar and change its color to red
            progressBar.setIndeterminate(false);
            progressBar.getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(this, android.R.color.holo_red_light),
                    PorterDuff.Mode.SRC_IN
            );

            // Hide the progress bar and show the error image and text immediately
            progressBar.setVisibility(View.INVISIBLE); // Hide the progress bar
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("\uD83D\uDEA8ERROR"); // Only display "ERROR"
            errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark)); // Set text color to red

            // Display all errors in the titleText as a numbered list
            StringBuilder errorMessage = new StringBuilder();
            for (String error : errors) {
                errorMessage.append(error).append("\n");
            }
            titleText.setText(errorMessage.toString()); // Use titleText to describe the errors

            img.setVisibility(View.VISIBLE);
            img.setImageResource(R.drawable.error);
            img.setContentDescription("Error image");
        });
    }

    private void showError(String message) {
        List<String> errors = new ArrayList<>();
        errors.add(message);
        showError(errors);
    }

    private void showSuccess() {
        runOnUiThread(() -> {
            // Stop the progress bar and change its color to green
            progressBar.setIndeterminate(false);
            progressBar.getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(this, android.R.color.holo_green_light),
                    PorterDuff.Mode.SRC_IN
            );

            // Hide the progress bar and show the success image and text immediately
            progressBar.setVisibility(View.INVISIBLE); // Hide the progress bar
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("✅SUCCESS"); // Display "SUCCESS"
            errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark)); // Set text color to green
            titleText.setText("Verification successful! Redirecting to daily diary..."); // Use titleText for success message
            img.setVisibility(View.VISIBLE);
            img.setImageResource(R.drawable.happy_student_cuate);
            img.setContentDescription("Success image");

            // Redirect to daily diary activity after a delay
            handler.postDelayed(() -> {
                startActivity(new Intent(Verify.this, daily_Diary.class));
                finish();
            }, 5000); // 5-second delay before redirecting
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}