package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class Recycle extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<model> modelArrayList; // List to store site data
    itemAdapter adapter;            // Adapter for RecyclerView
    FirebaseFirestore firestore;    // Firestore instance for database access
    String userSelectedSite;        // Store the user's already selected site
    String selectedCategory;        // Category passed from the intent
    FirebaseUser currentUser;
    String searchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize components
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        modelArrayList = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        selectedCategory = getIntent().getStringExtra("CATEGORY_NAME");
        searchQuery = getIntent().getStringExtra("SEARCH_QUERY");


        if (selectedCategory != null) {
            Toast.makeText(this, "Category: " + selectedCategory, Toast.LENGTH_SHORT).show();
            fetchUserSelectedSite(); // Fetch user's selected site
        }
        else {
            Toast.makeText(this, "No category provided", Toast.LENGTH_SHORT).show();
        }
    }

    // Fetch the user's already selected site from Firestore
    private void fetchUserSelectedSite() {
        if (currentUser != null) {
            firestore.collection("UserSelections")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("selectedSite")) {
                            userSelectedSite = documentSnapshot.getString("selectedSite");
                        } else {
                            userSelectedSite = null;
                        }
                        fetchSitesFromFirestore(); // Fetch sites after getting the user's selected site
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to fetch user selection: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        fetchSitesFromFirestore(); // Fetch sites even if there's an error
                    });
        } else {
            fetchSitesFromFirestore(); // Fetch sites if the user is not logged in
        }
    }

    // Fetch sites for the selected category from Firestore
    private void fetchSitesFromFirestore() {
        firestore.collection(selectedCategory)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    modelArrayList.clear(); // Clear previous data if any
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot siteSnapshot : queryDocumentSnapshots) {
                            String head = siteSnapshot.getString("head");
                            Long availableSlots = siteSnapshot.getLong("availableSlots");
                            Long totalSlots = siteSnapshot.getLong("totalSlots");
                            String description = siteSnapshot.getString("description");

                            if (head != null && availableSlots != null && totalSlots != null && description != null) {
                                modelArrayList.add(new model(
                                        head,
                                        availableSlots.intValue(),
                                        totalSlots.intValue(),
                                        description
                                ));
                            }
                        }

                        // Initialize adapter with the user's selected site and category
                        adapter = new itemAdapter(this, modelArrayList, selectedCategory, userSelectedSite,searchQuery);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "No data found for this category", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
