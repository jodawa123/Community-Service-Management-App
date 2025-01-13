package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    EditText identity, pass;
    Button btnLogin;
    TextView signUpText;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    TextView forgot;

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

        // Initialize Firebase Authentication and Database Reference
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        identity = findViewById(R.id.identity);
        pass = findViewById(R.id.pass);
        btnLogin = findViewById(R.id.btnLogin);
        signUpText = findViewById(R.id.signUpText);
        forgot=findViewById(R.id.forgot);

        // Login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = identity.getText().toString().trim();
                String password = pass.getText().toString().trim();

                // Check if fields are empty
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(login.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Login using Firebase Authentication
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(login.this, "Login successful !!", Toast.LENGTH_SHORT).show();
                                // Login success
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user != null) {
                                    // Check if email is verified
                                    if (user.isEmailVerified()) {
                                        String userId = user.getUid();

                                        // Fetch username from Firebase Realtime Database
                                        databaseReference.child(userId).get().addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful() && dbTask.getResult() != null) {
                                                DataSnapshot snapshot = dbTask.getResult();
                                                String username = snapshot.child("name").getValue(String.class);

                                                if (username != null && !username.isEmpty()) {
                                                    // Pass username to Home activity
                                                    Intent intent = new Intent(login.this, Home.class);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    StyleableToast.makeText(login.this, "Username not found in database", R.style.mytoast).show();
                                                }
                                            } else {
                                                StyleableToast.makeText(login.this, "Failed to retrieve user data: " + dbTask.getException().getMessage(), R.style.mytoast).show();
                                            }
                                        });
                                    } else {
                                        // Email not verified
                                        StyleableToast.makeText(login.this, "Please verify your email before logging in", R.style.mytoast).show();
                                        firebaseAuth.signOut();
                                    }
                                }
                            } else {
                                // Login failed
                                StyleableToast.makeText(login.this, "Login Failed: " + task.getException().getMessage(), R.style.mytoast).show();
                            }
                        });
            }
        });

        // "Sign up" TextView click listener
        signUpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to SignUpActivity
                Intent intent = new Intent(login.this, SignUp.class);
                startActivity(intent);
            }
        });
        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(login.this, Mail.class);
                startActivity(intent);
            }
        });


    }
}
