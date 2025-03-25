package com.example.helpinghands;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import io.github.muddz.styleabletoast.StyleableToast;

public class Mail extends AppCompatActivity {
    private EditText emailEditText;
    private Button resetPasswordButton;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);

        // Set content descriptions for accessibility
        emailEditText.setContentDescription("Enter your email address to reset your password");
        resetPasswordButton.setContentDescription("Tap to reset your password and receive an email");

        // Set up button click listener
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();

                if (email.isEmpty()) {
                    announceError("Please enter your email address");
                } else {
                    // Send password reset email
                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    announceSuccess("Password reset email sent! Check your inbox.");
                                } else {
                                    announceError("Error: " + task.getException().getMessage());
                                }
                            });
                }
            }
        });
    }

    private void announceError(String message) {
        StyleableToast.makeText(this, message, R.style.mytoast).show();
        resetPasswordButton.announceForAccessibility(message);
    }

    private void announceSuccess(String message) {
        StyleableToast.makeText(this, message, R.style.mytoast).show();
        resetPasswordButton.announceForAccessibility(message);
    }
}