package com.example.cs3700;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.github.muddz.styleabletoast.StyleableToast;

public class login extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
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


        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        editTextEmail = findViewById(R.id.identity);
        editTextPassword = findViewById(R.id.pass);
        Button buttonLogin = findViewById(R.id.btnLogin);
        TextView textViewSignUp = findViewById(R.id.signUpText);
        TextView textViewForgotPassword = findViewById(R.id.forgot);

        buttonLogin.setContentDescription("Tap to log in");
        textViewSignUp.setContentDescription("Tap to sign up for a new account");
        textViewForgotPassword.setContentDescription("Tap if you forgot your password");

        buttonLogin.setOnClickListener(view -> {
            if (!isInternetAvailable()) {
                showToastAndAnnounce("No internet connection. Please try again later.");
                return;
            }

            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showToastAndAnnounce("Please fill all fields");
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                databaseReference.child(user.getUid()).get().addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful() && dbTask.getResult() != null) {
                                        DataSnapshot snapshot = dbTask.getResult();
                                        String username = snapshot.child("name").getValue(String.class);
                                        if (username != null && !username.isEmpty()) {
                                            showToastAndAnnounce("Login successful. Welcome, " + username + "!");
                                            Intent intent = new Intent(login.this, Home.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            showToastAndAnnounce("Username not found in database");
                                        }
                                    } else {
                                        showToastAndAnnounce("Failed to retrieve user data: " + dbTask.getException().getMessage());
                                    }
                                });
                            } else {
                                showToastAndAnnounce("Please verify your email before logging in");
                                firebaseAuth.signOut();
                            }
                        } else {
                            showToastAndAnnounce("Login Failed: Incorrect email or password");
                        }
                    });
        });

        textViewSignUp.setOnClickListener(v -> startActivity(new Intent(login.this, SignUp.class)));
        textViewForgotPassword.setOnClickListener(view -> startActivity(new Intent(login.this, Mail.class)));
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    private void showToastAndAnnounce(String message) {
        Toast.makeText(login.this, message, Toast.LENGTH_SHORT).show();
        View mainLayout = findViewById(R.id.main); // Use the root ConstraintLayout
        if (mainLayout != null) {
            mainLayout.announceForAccessibility(message);
        } else {
            Log.e("login", "mainLayout is null. Check if the ID is correct and the view exists in the layout.");
        }
    }
}
