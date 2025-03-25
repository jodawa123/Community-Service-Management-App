package com.example.helpinghands;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Rating extends AppCompatActivity {
    private int selectedRating = 0;
    private HorizontalScrollView emojiScrollView;
    private LinearLayout emojiContainer;
    private ProgressBar ratingProgress;
    private Button submitButton;
    private TextView funCaption;
    private EditText commentBox;
    private String userComment = ""; // Store the user's comment
    private ImageView lastFocusedEmoji = null; // Track the last focused emoji
    private FirebaseAuth firebaseAuth;

    // Firestore instance
    private FirebaseFirestore db;

    // Variables to store site and category
    private String selectedSite;
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rating);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore and Firebase Auth
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize views
        emojiScrollView = findViewById(R.id.emojiScrollView);
        emojiContainer = findViewById(R.id.emojiContainer);
        ratingProgress = findViewById(R.id.ratingProgress);
        submitButton = findViewById(R.id.submitButton);
        funCaption = findViewById(R.id.funCaption);
        commentBox = findViewById(R.id.commentBox);

        ImageView badEmoji = findViewById(R.id.badEmoji);
        ImageView mehEmoji = findViewById(R.id.mehEmoji);
        ImageView greatEmoji = findViewById(R.id.greatEmoji);

        // Load GIFs with Glide
        Glide.with(this).load(R.drawable.donot).into(badEmoji);
        Glide.with(this).load(R.drawable.meh).into(mehEmoji);
        Glide.with(this).load(R.drawable.smile).into(greatEmoji);

        // Handle emoji selection
        badEmoji.setOnClickListener(v -> handleEmojiSelection(1, badEmoji));
        mehEmoji.setOnClickListener(v -> handleEmojiSelection(2, mehEmoji));
        greatEmoji.setOnClickListener(v -> handleEmojiSelection(3, greatEmoji));

        // Handle submit button click
        submitButton.setOnClickListener(v -> {
            userComment = commentBox.getText().toString().trim();
            if (!userComment.isEmpty()) {
                submitReview(selectedRating, userComment);
            } else {
                Toast.makeText(this, "Please write a comment.", Toast.LENGTH_SHORT).show();
            }
        });

        // Retrieve selectedSite and selectedCategory from Firestore during onCreate
        retrieveSiteAndCategory();
    }

    private void retrieveSiteAndCategory() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Fetch selectedSite and selectedCategory from UserStates
        db.collection("UserStates")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedSite = documentSnapshot.getString("selectedSite");
                        selectedCategory = documentSnapshot.getString("selectedCategory");

                        if (selectedSite == null || selectedCategory == null) {
                            Toast.makeText(this, "Selected site or category not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "User state not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user state: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void submitReview(int rating, String comment) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the current user's ID
        String currentUserId = currentUser.getUid();

        // Get the current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Check if selectedSite and selectedCategory are available
        if (selectedSite != null && selectedCategory != null) {
            // Save the review data
            saveReviewToFirestore(selectedCategory, selectedSite, currentUserId, rating, comment, currentDate);
        } else {
            Toast.makeText(this, "Selected site or category not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveReviewToFirestore(String selectedCategory, String selectedSite, String userId, int rating, String comment, String date) {
        // Create a map for the review data
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("rating", rating);
        reviewData.put("comment", comment);
        reviewData.put("date", date);

        // Save the review data to Firestore
        db.collection(selectedCategory)
                .whereEqualTo("head", selectedSite) // Query for the document with matching "head"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the first document (assuming there's only one match)
                        DocumentSnapshot siteDocument = queryDocumentSnapshots.getDocuments().get(0);

                        // Navigate to the "members" subcollection and update the current user's document
                        siteDocument.getReference()
                                .collection("members")
                                .document(userId)
                                .set(reviewData, SetOptions.merge()) // Use .set() with merge to add fields without overwriting
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Selected site not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to query Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleEmojiSelection(int rating, ImageView selectedEmoji) {
        selectedRating = rating;
        ratingProgress.setProgress(rating);

        // Change the progress bar color based on the selected emoji
        switch (rating) {
            case 1:
                // Bad (Red)
                ratingProgress.setProgressTintList(ColorStateList.valueOf(Color.RED));
                funCaption.setText("I would not recommend!");
                break;
            case 2:
                // Meh (Yellow)
                ratingProgress.setProgressTintList(ColorStateList.valueOf(Color.BLUE));
                funCaption.setText("Meh... Could be better");
                break;
            case 3: // Great (Green)
                ratingProgress.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                funCaption.setText("Great! Keep up the good work");
                break;
        }

        // If an emoji was previously focused, shrink it back to 100x100
        if (lastFocusedEmoji != null && lastFocusedEmoji != selectedEmoji) {
            animateEmojiSize(lastFocusedEmoji, 100);
        }

        // Grow the selected emoji to 400x400
        animateEmojiSize(selectedEmoji, 400);

        // Update the last focused emoji
        lastFocusedEmoji = selectedEmoji;

        // Center the selected emoji after a small delay
        emojiScrollView.post(() -> centerEmoji(selectedEmoji));

        // Slide the emojis and progress bar upwards
        slideContainerUp();

        // Show the comment box
        commentBox.setVisibility(View.VISIBLE);
    }

    private void animateEmojiSize(ImageView emoji, int targetSize) {
        float scale = (float) targetSize / emoji.getWidth(); // Calculate the scale factor
        emoji.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(300) // Animation duration in milliseconds
                .start();
    }

    private void centerEmoji(ImageView selectedEmoji) {
        // Calculate the scroll position to center the selected emoji
        int scrollX = (selectedEmoji.getLeft() + selectedEmoji.getRight()) / 2 - emojiScrollView.getWidth() / 2;
        emojiScrollView.smoothScrollTo(scrollX, 0);
    }

    private void slideContainerUp() {
        // Calculate the target translation (e.g., move up by 40dp)
        int translationY = (int) (-40 * getResources().getDisplayMetrics().density);

        // Animate the container
        emojiContainer.animate()
                .translationY(translationY)
                .setDuration(500)
                .start();
    }
}