package com.example.cs3700;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import java.util.Locale;

public class Mapping extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final float DEFAULT_ZOOM = 15f;

    private double siteLatitude;
    private double siteLongitude;
    private String siteName;

    private LatLng userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping);

        // Retrieve site details from Intent
        Intent intent = getIntent();
        siteLatitude = intent.getDoubleExtra("LATITUDE", 0);
        siteLongitude = intent.getDoubleExtra("LONGITUDE", 0);
        siteName = intent.getStringExtra("SITE_NAME");

        // Initialize FusedLocationProviderClient for user's location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Add a blue marker for the site location
        LatLng siteLocation = new LatLng(siteLatitude, siteLongitude);
        mMap.addMarker(new MarkerOptions()
                .position(siteLocation)
                .title(siteName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        // Move the camera to the site location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(siteLocation, DEFAULT_ZOOM));

        // Get user's current location and draw the path
        getUserLocation();
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Add a blue marker for the user's location
                    mMap.addMarker(new MarkerOptions()
                            .position(userLocation)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                    // Open Google Maps for navigation
                    openGoogleMapsDirections(userLocation, new LatLng(siteLatitude, siteLongitude));
                } else {
                    Toast.makeText(Mapping.this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(Mapping.this, "Error retrieving location: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }
    }

    private void openGoogleMapsDirections(LatLng start, LatLng end) {
        // Create a Google Maps Directions URI
        String uri = String.format(Locale.ENGLISH,
                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                start.latitude, start.longitude, end.latitude, end.longitude);

        // Open Google Maps with driving directions
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps app is not installed.", Toast.LENGTH_LONG).show();
        }
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
