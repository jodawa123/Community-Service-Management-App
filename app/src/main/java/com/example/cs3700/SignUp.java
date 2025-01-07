package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.github.muddz.styleabletoast.StyleableToast;

public class SignUp extends AppCompatActivity {
    EditText editTextText, editTextText4, editTextText5, identity;
    Button button;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize UI components
        editTextText = findViewById(R.id.editTextText);   // Name field
        identity = findViewById(R.id.identity);          // Identity field
        editTextText4 = findViewById(R.id.editTextText4); // Email field
        editTextText5 = findViewById(R.id.editTextText5); // Password field
        button = findViewById(R.id.button);              // Sign-Up button

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = editTextText.getText().toString().trim();
                String id = identity.getText().toString().trim();
                String email = editTextText4.getText().toString().trim();
                String password = editTextText5.getText().toString().trim();

                // Validate inputs
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignUp.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 6) {
                    Toast.makeText(SignUp.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else if (id.length() != 6) {
                    Toast.makeText(SignUp.this, "ID must be exactly 6 characters", Toast.LENGTH_SHORT).show();
                } else {
                    // Create a new user in Firebase Authentication
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = firebaseAuth.getCurrentUser();
                                    if (user != null) {
                                        // Send verification email
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        String userId = user.getUid();

                                                        // Save user details to the database
                                                        User newUser = new User(name, id, email);
                                                        databaseReference.child(userId).setValue(newUser)
                                                                .addOnCompleteListener(dbTask -> {
                                                                    if (dbTask.isSuccessful()) {
                                                                        StyleableToast.makeText(SignUp.this, "Registration successful! Verify your email before logging in.", R.style.mytoast).show();

                                                                        // Log out the user and redirect to Login page
                                                                        firebaseAuth.signOut();
                                                                        Intent intent = new Intent(SignUp.this, login.class);
                                                                        startActivity(intent);
                                                                        finish();
                                                                    } else {
                                                                        StyleableToast.makeText(SignUp.this, "Error saving user data: " + dbTask.getException().getMessage(), R.style.mytoast).show();
                                                                    }
                                                                });
                                                    } else {
                                                        StyleableToast.makeText(SignUp.this, "Failed to send verification email: " + verificationTask.getException().getMessage(), R.style.mytoast).show();
                                                    }
                                                });
                                    }
                                } else {
                                    StyleableToast.makeText(SignUp.this, "Error: " + task.getException().getMessage(), R.style.mytoast).show();
                                }
                            });
                }
            }
        });
    }

    // User class to hold data
    public static class User {
        public String name;
        public String id;
        public String email;

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public User(String name, String id, String email) {
            this.name = name;
            this.id = id;
            this.email = email;
        }
    }
}