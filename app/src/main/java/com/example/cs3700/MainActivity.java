package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final int SPLASH_SCREEN_DELAY = 5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Adjust for system UI insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
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

        // Only hide ActionBar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }


        // Load animations
        // UI Elements
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        // Initialize UI components
        ImageView logoImageView = findViewById(R.id.logoImageView);
        TextView titleTextView = findViewById(R.id.titleTextView);
        TextView taglineTextView = findViewById(R.id.taglineTextView);

        // Apply animations
        logoImageView.setAnimation(slideDownAnimation);
        titleTextView.setAnimation(fadeInAnimation);
        taglineTextView.setAnimation(fadeInAnimation);

        // Ensure TalkBack focuses correctly after animation
        titleTextView.requestFocus();

        // Delayed transition to LoginActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, login.class);
            startActivity(intent);
            finish();
        }, SPLASH_SCREEN_DELAY);
    }
}
