package com.example.cs3700;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Mapping extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore db;

    private static final float DEFAULT_ZOOM = 15f;

    private LatLng userLocation;
    private String siteName;
    private double siteLatitude;
    private double siteLongitude;

    private CardView tipsPanel;
    private Button dismissButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping);

        // Initialize Firestore and location provider
        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Retrieve site details from Intent
        Intent intent = getIntent();
        siteName = intent.getStringExtra("SITE_NAME");
        siteLatitude = intent.getDoubleExtra("LATITUDE", 0);
        siteLongitude = intent.getDoubleExtra("LONGITUDE", 0);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup tips panel
        tipsPanel = findViewById(R.id.tips_panel);
        dismissButton = findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(v -> tipsPanel.setVisibility(View.GONE));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        getUserLocation();
    }

    private void pinSpecificSite() {
        LatLng siteLocation = new LatLng(siteLatitude, siteLongitude);

        // Add a blue marker for the site
        mMap.addMarker(new MarkerOptions()
                        .position(siteLocation)
                        .title(siteName)
                        .snippet("Tap to reveal directions")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                .setTag(siteLocation);

        // Move the camera to the specific site
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(siteLocation, DEFAULT_ZOOM));

        // Set up info window click listener
        mMap.setOnInfoWindowClickListener(this::openGoogleMapsDirections);

        // Draw the path between the user's location and the site
        if (userLocation != null) {
            drawPathToSite(siteLocation);
        }
    }

    private void drawPathToSite(LatLng siteLocation) {
        if (userLocation == null) {
            Toast.makeText(this, "User location not available yet. Try again later.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the URL for the Google Directions API
        String url = String.format(Locale.ENGLISH,
                "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&mode=driving&key=YOUR_API_KEY",
                userLocation.latitude, userLocation.longitude,
                siteLocation.latitude, siteLocation.longitude);

        // Fetch the directions and draw the polyline
        new FetchDirectionsTask(url, directionsResult -> {
            if (directionsResult != null) {
                List<LatLng> routePoints = decodePolyline(directionsResult);
                if (routePoints != null) {
                    mMap.addPolyline(new PolylineOptions()
                            .addAll(routePoints)
                            .width(8f)
                            .color(ContextCompat.getColor(this, R.color.blue)));
                } else {
                    Toast.makeText(this, "Failed to decode route.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to fetch directions.", Toast.LENGTH_SHORT).show();
            }
        }).execute();
    }

    private static class FetchDirectionsTask extends AsyncTask<Void, Void, String> {
        private final String url;
        private final OnDirectionsResultListener listener;

        interface OnDirectionsResultListener {
            void onResult(String result);
        }

        FetchDirectionsTask(String url, OnDirectionsResultListener listener) {
            this.url = url;
            this.listener = listener;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL urlObject = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                return builder.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            listener.onResult(result);
        }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
        }

        return poly;
    }

    private void fetchAllSites() {
        String[] collections = {"ChildrenHomes", "Rehab", "SpecialNeeds", "Hospice", "RescueCenter", "HealthCenters"};

        for (String collection : collections) {
            db.collection(collection).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String name = document.getString("head");
                        double latitude = document.getGeoPoint("location").getLatitude();
                        double longitude = document.getGeoPoint("location").getLongitude();

                        LatLng siteLocation = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions()
                                        .position(siteLocation)
                                        .title(name != null ? name : "Unnamed Site")
                                        .snippet("Tap to reveal directions")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                                .setTag(siteLocation);
                    }
                } else {
                    Toast.makeText(this, "Failed to fetch sites from " + collection, Toast.LENGTH_SHORT).show();
                }
            });
        }

        mMap.setOnInfoWindowClickListener(this::openGoogleMapsDirections);
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {

                if (location != null) {

                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    mMap.addMarker(new MarkerOptions()
                            .position(userLocation)
                            .title("You are here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                    if (siteName == null) {
                        fetchAllSites();
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
                    } else {
                        pinSpecificSite();
                    }
                } else {
                    Toast.makeText(this, "Unable to get your location.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }
    }

    private void openGoogleMapsDirections(Marker marker) {
        LatLng siteLocation = (LatLng) marker.getTag();
        if (userLocation != null && siteLocation != null) {
            String uri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                    userLocation.latitude, userLocation.longitude, siteLocation.latitude, siteLocation.longitude);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Google Maps app is not installed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void promptEnableLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(builder.build())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } else {
                        getUserLocation();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Permission denied. Unable to show your location.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}