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

public class login extends AppCompatActivity {
    EditText identity, pass;
    Button btnLogin;
    TextView signUpText; // TextView for Sign Up navigation
    FirebaseAuth firebaseAuth;  // Firebase Authentication instance

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

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        identity = findViewById(R.id.identity);
        pass = findViewById(R.id.pass);
        btnLogin = findViewById(R.id.btnLogin);
        signUpText = findViewById(R.id.signUpText);

        // Login button click
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
                                // Login success
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                Toast.makeText(login.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                                // Proceed to next activity
                                startActivity(new Intent(login.this, Home.class));
                                finish();
                            } else {
                                // Login failed
                                Toast.makeText(login.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
    }
}
