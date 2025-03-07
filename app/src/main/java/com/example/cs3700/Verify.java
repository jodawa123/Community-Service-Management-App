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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import android.Manifest;

import java.util.Calendar;

public class Verify extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView titleText,error;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private ImageView img;
    private String userId, selectedCategory, selectedSite;
    private double siteLatitude, siteLongitude;
    private FusedLocationProviderClient fusedLocationClient;
    private static final float ALLOWED_RADIUS_METERS = 100; // 100 meters radius
    private static final int WORK_START_HOUR = 7; // 7 AM
    private static final int WORK_END_HOUR = 15; // 3 PM
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LocationCallback locationCallback;
    private Handler handler = new Handler(); // Handler for delays

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify);

        progressBar = findViewById(R.id.progressBar);
        titleText = findViewById(R.id.titleText);
        error=findViewById(R.id.headText);
        img=findViewById(R.id.hold);

        // Apply custom spinner drawable to slow down the animation


        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = (user != null) ? user.getUid() : null;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    showError("Unable to retrieve location.");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        verifyLocation(location);
                        // Stop location updates after getting the location
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                        break;
                    }
                }
            }
        };

        // Check and request location permissions
        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, start verification
            startVerification();
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start verification
                startVerification();
            } else {
                // Permission denied, show error
                showPermissionDeniedError();
            }
        }
    }

    private void showPermissionDeniedError() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("This app requires location permission to verify your presence. Please enable it in the app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void startVerification() {
        Log.d("Verify", "Verification started");
        // Fetch selected site and category
        fetchSelectedSiteAndCategory();
    }

    private void fetchSelectedSiteAndCategory() {
        Log.d("Verify", "Fetching selected site and category");
        DocumentReference userStateRef = db.collection("UserStates").document(userId);
        userStateRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                selectedCategory = document.getString("selectedCategory");
                selectedSite = document.getString("selectedSite");

                if (selectedCategory != null && selectedSite != null) {
                    Log.d("Verify", "Site and category fetched successfully");
                    fetchSiteGeolocationAndVerify(selectedCategory, selectedSite);
                } else {
                    showError("No site or category selected.");
                }
            } else {
                showError("No site currently selected.");
                titleText.setText("Verification Failed");
            }
        }).addOnFailureListener(e -> {
            showError("Error fetching user state.");
            Log.e("Verify", "Error fetching user state", e);
        });
    }

    private void fetchSiteGeolocationAndVerify(String categoryName, String head) {
        Log.d("Verify", "Fetching site geolocation");
        db.collection(categoryName)
                .whereEqualTo("head", head)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                        // Extract the GeoPoint from the "location" field
                        com.google.firebase.firestore.GeoPoint location = document.getGeoPoint("location");

                        if (location != null) {
                            siteLatitude = location.getLatitude(); // Get latitude
                            siteLongitude = location.getLongitude(); // Get longitude

                            Log.d("Verify", "Site geolocation fetched successfully: " +
                                    "Latitude = " + siteLatitude + ", Longitude = " + siteLongitude);

                            verifyLocationAndTime();
                        } else {
                            showError("Site geolocation not found.");
                        }
                    } else {
                        showError("Site not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Error fetching site geolocation.");
                    Log.e("Verify", "Error fetching site geolocation", e);
                });
    }

    private void verifyLocationAndTime() {
        Log.d("Verify", "Verifying location and time");

        // Check if logging is during work hours
        if (!isDuringWorkHours()) {
            showError("You can only log your diary during work hours (9 AM - 5 PM).");
            titleText.setText("Verification Failed");
            return;
        }

        // Request location updates
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        Log.d("Verify", "Requesting location updates");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            showError("Location permission denied. Please enable location services.");
            Log.e("Verify", "SecurityException: Location permission denied", e);
        }
    }

    private void verifyLocation(Location location) {
        Log.d("Verify", "Verifying location");
        float[] results = new float[1];
        Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                siteLatitude, siteLongitude, results
        );
        float distanceInMeters = results[0];

        // Check if location condition is met
        boolean isLocationMet = distanceInMeters <= ALLOWED_RADIUS_METERS;
        // Check if time condition is met
        boolean isTimeMet = isDuringWorkHours();

        // Handle all four cases
        if (isLocationMet && isTimeMet) {
            // Case 1: Both location and time are met
            Log.d("Verify", "Location and time verified successfully");
            showSuccess();
        } else if (!isLocationMet && isTimeMet) {
            // Case 2: Location is not met, but time is met
            showError("You must be at the site to log your diary.");
        } else if (isLocationMet && !isTimeMet) {
            // Case 3: Time is not met, but location is met
            showError("You can only log your diary during work hours (9 AM - 5 PM).");
        } else {
            // Case 4: Both location and time are not met
            showError("You must be at the site and log your diary during work hours (9 AM - 5 PM).");
        }
    }

    private boolean isDuringWorkHours() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        return currentHour >= WORK_START_HOUR && currentHour < WORK_END_HOUR;
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            // Change progress bar color to red
            progressBar.getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(this, android.R.color.holo_red_light),
                    PorterDuff.Mode.SRC_IN
            );
            // Stop the progress bar animation
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE); // Hide the progress bar

            // Show error message and image
            error.setVisibility(View.VISIBLE);
            img.setVisibility(View.VISIBLE);
            img.setImageResource(R.drawable.error);
            img.setContentDescription("Error image");
            error.setText("🚨 " + message); // Add red alert emoji
            error.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark)); // Set text color to red

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void showSuccess() {
        runOnUiThread(() -> {
            // Change progress bar color to green
            progressBar.getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(this, android.R.color.holo_green_light),
                    PorterDuff.Mode.SRC_IN
            );
            // Stop the progress bar animation
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE); // Hide the progress bar

            // Show success message
            error.setVisibility(View.VISIBLE);
            img.setVisibility(View.VISIBLE);
            img.setImageResource(R.drawable.happy_student_cuate);
            img.setContentDescription("Success image");
            error.setText("🎉 SUCCESS"); // Add celebration emoji
            error.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark)); // Set text color to green

            Toast.makeText(this, "Approved! Redirecting to daily diary...", Toast.LENGTH_SHORT).show();

            // Redirect to daily diary activity after a delay
            handler.postDelayed(() -> {
                Log.d("Verify", "Launching daily_Diary activity");
                Intent intent = new Intent(Verify.this, daily_Diary.class);
                startActivity(intent);
                finish(); // Close the Verify activity
            }, 5000); // 5 seconds delay
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null);
    }
}