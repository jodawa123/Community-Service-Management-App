package com.example.cs3700;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
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
    TextView signUpText, forgot;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

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
        forgot = findViewById(R.id.forgot);

        btnLogin.setOnClickListener(view -> {
            if (!isInternetAvailable()) {
                Toast.makeText(login.this, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = identity.getText().toString().trim();
            String password = pass.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(login.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(login.this, "Login successful !!", Toast.LENGTH_SHORT).show();
                            // Login success
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                databaseReference.child(user.getUid()).get().addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful() && dbTask.getResult() != null) {
                                        DataSnapshot snapshot = dbTask.getResult();
                                        String username = snapshot.child("name").getValue(String.class);
                                        if (username != null && !username.isEmpty()) {
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
                                StyleableToast.makeText(login.this, "Please verify your email before logging in", R.style.mytoast).show();
                                firebaseAuth.signOut();
                            }
                        } else {
                            StyleableToast.makeText(login.this, "Login Failed: Incorrect username or password", R.style.mytoast).show();
                        }
                    });
        });

        signUpText.setOnClickListener(v -> startActivity(new Intent(login.this, SignUp.class)));
        forgot.setOnClickListener(view -> startActivity(new Intent(login.this, Mail.class)));
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
}
