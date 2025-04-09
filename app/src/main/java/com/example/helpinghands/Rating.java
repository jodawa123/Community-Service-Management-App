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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

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
    private String userComment = "";
    private ImageView lastFocusedEmoji = null;
    private FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
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

        firebaseAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupEmojiSelection();
        retrieveSiteAndCategory();
    }

    private void initializeViews() {
        emojiScrollView = findViewById(R.id.emojiScrollView);
        emojiContainer = findViewById(R.id.emojiContainer);
        ratingProgress = findViewById(R.id.ratingProgress);
        submitButton = findViewById(R.id.submitButton);
        funCaption = findViewById(R.id.funCaption);
        commentBox = findViewById(R.id.commentBox);

        // Load GIFs with Glide
        ImageView badEmoji = findViewById(R.id.badEmoji);
        ImageView mehEmoji = findViewById(R.id.mehEmoji);
        ImageView greatEmoji = findViewById(R.id.greatEmoji);
        Glide.with(this).load(R.drawable.donot).into(badEmoji);
        Glide.with(this).load(R.drawable.meh).into(mehEmoji);
        Glide.with(this).load(R.drawable.smile).into(greatEmoji);
    }

    private void setupEmojiSelection() {
        findViewById(R.id.badEmoji).setOnClickListener(v -> handleEmojiSelection(1, (ImageView) v));
        findViewById(R.id.mehEmoji).setOnClickListener(v -> handleEmojiSelection(2, (ImageView) v));
        findViewById(R.id.greatEmoji).setOnClickListener(v -> handleEmojiSelection(3, (ImageView) v));

        submitButton.setOnClickListener(v -> {
            userComment = commentBox.getText().toString().trim();
            if (selectedRating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            } else if (userComment.isEmpty()) {
                Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            } else {
                submitRating();
            }
        });
    }

    private void retrieveSiteAndCategory() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("UserStates")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedSite = documentSnapshot.getString("selectedSite");
                        selectedCategory = documentSnapshot.getString("selectedCategory");
                        if (selectedSite == null || selectedCategory == null) {
                            showErrorAndFinish("Site or category not found");
                        }
                    } else {
                        showErrorAndFinish("User state not found");
                    }
                })
                .addOnFailureListener(e -> showErrorAndFinish("Failed to fetch user state"));
    }

    private void submitRating() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || selectedSite == null || selectedCategory == null) {
            showErrorAndFinish("Invalid user or site information");
            return;
        }

        String userId = user.getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        firestore.collection(selectedCategory)
                .whereEqualTo("head", selectedSite)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentReference siteRef = querySnapshot.getDocuments().get(0).getReference();
                        updateMemberRating(siteRef, userId, date);
                    } else {
                        showErrorAndFinish("Site not found");
                    }
                })
                .addOnFailureListener(e -> showErrorAndFinish("Failed to find site"));
    }

    private void updateMemberRating(DocumentReference siteRef, String userId, String date) {
        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("rating", selectedRating);
        ratingData.put("comment", userComment);
        ratingData.put("date", date);
        ratingData.put("lastUpdated", FieldValue.serverTimestamp());

        siteRef.collection("members")
                .document(userId)
                .update(ratingData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> showErrorAndFinish("Failed to submit rating"));
    }

    private void handleEmojiSelection(int rating, ImageView selectedEmoji) {
        selectedRating = rating;
        ratingProgress.setProgress(rating);

        switch (rating) {
            case 1:
                ratingProgress.setProgressTintList(ColorStateList.valueOf(Color.RED));
                funCaption.setText("I would not recommend!");
                break;
            case 2:
                ratingProgress.setProgressTintList(ColorStateList.valueOf(Color.BLUE));
                funCaption.setText("Meh... Could be better");
                break;
            case 3:
                ratingProgress.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                funCaption.setText("Great! Keep up the good work");
                break;
        }

        if (lastFocusedEmoji != null && lastFocusedEmoji != selectedEmoji) {
            animateEmojiSize(lastFocusedEmoji, 100);
        }

        animateEmojiSize(selectedEmoji, 400);
        lastFocusedEmoji = selectedEmoji;
        emojiScrollView.post(() -> centerEmoji(selectedEmoji));
        slideContainerUp();
        commentBox.setVisibility(View.VISIBLE);
    }

    private void animateEmojiSize(ImageView emoji, int targetSize) {
        float scale = (float) targetSize / emoji.getWidth();
        emoji.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(300)
                .start();
    }

    private void centerEmoji(ImageView selectedEmoji) {
        int scrollX = (selectedEmoji.getLeft() + selectedEmoji.getRight()) / 2 - emojiScrollView.getWidth() / 2;
        emojiScrollView.smoothScrollTo(scrollX, 0);
    }

    private void slideContainerUp() {
        int translationY = (int) (-40 * getResources().getDisplayMetrics().density);
        emojiContainer.animate()
                .translationY(translationY)
                .setDuration(500)
                .start();
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}