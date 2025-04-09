package com.example.helpinghands;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Reviews extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ReviewAdapter reviewAdapter;
    private List<Reviewmodel> reviewList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reviews);

        // Disable page number announcements by hiding the action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Ensure the title is not spoken by TalkBack
        setTitle(" ");

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firestore and Firebase Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        // Retrieve the site name from the Intent
        String siteName = getIntent().getStringExtra("SITE_NAME");
        if (siteName == null) {
            Toast.makeText(this, "Site name not provided.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no site name is provided
            return;
        }

        // Fetch reviews for the specific site
        fetchReviews(siteName);
    }

    private void fetchReviews(String siteName) {
        // List of collections to search for the site
        String[] collections = {"ChildrenHomes", "HealthCenters", "Hospice", "Rehab", "RescueCenter", "SpecialNeeds"};

        // Fetch reviews from all collections
        for (String collection : collections) {
            db.collection(collection)
                    .whereEqualTo("head", siteName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            // No document found with the matching site name
                            //Toast.makeText(this, "No document found for site: " + siteName, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Get the first matching document (assuming site names are unique)
                        String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        fetchAllMembersReviews(collection, documentId, siteName); // Fetch all reviews for the site
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to fetch site document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchAllMembersReviews(String collection, String documentId, String siteName) {
        db.collection(collection)
                .document(documentId)
                .collection("members")
                .get()
                .addOnSuccessListener(membersQueryDocumentSnapshots -> {
                    if (membersQueryDocumentSnapshots.isEmpty()) {
                        // No members found (site hasn't been picked)
                        Toast.makeText(this, "No reviews found for site: " + siteName, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Clear the existing review list
                    reviewList.clear();

                    // Fetch all reviews from the members subcollection
                    for (QueryDocumentSnapshot memberSnapshot : membersQueryDocumentSnapshots) {
                        String reviewerName = memberSnapshot.getString("name");
                        String reviewTime = memberSnapshot.getString("date");
                        Long rating = memberSnapshot.getLong("rating");
                        String comment = memberSnapshot.getString("comment");

                        if (reviewerName != null && reviewTime != null && rating != null && comment != null) {
                            // Add the review to the list
                            reviewList.add(new Reviewmodel(reviewerName, reviewTime, rating.intValue(), comment));
                        }
                    }

                    // Set up RecyclerView
                    reviewAdapter = new ReviewAdapter(reviewList);
                    recyclerView.setAdapter(reviewAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch reviews: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}