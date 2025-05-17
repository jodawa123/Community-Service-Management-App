package com.example.helpinghands;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.github.muddz.styleabletoast.StyleableToast;

public class SignUp extends AppCompatActivity {
    private EditText nameField, emailField, passwordField, idField;
    private Button signUpButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        nameField = findViewById(R.id.editTextText);
        idField = findViewById(R.id.identity);
        emailField = findViewById(R.id.editTextText4);
        passwordField = findViewById(R.id.editTextText5);
        signUpButton = findViewById(R.id.button);

        // Set content descriptions for TalkBack accessibility
        signUpButton.setContentDescription("Tap to sign up and create an account");
        signUpButton.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        String name = nameField.getText().toString().trim();
        String id = idField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            announceMessage("Please fill all fields");
        } else if (password.length() < 6) {
            announceMessage("Password must be at least 6 characters");
        } else if (id.length() != 6) {
            announceMessage("ID must be exactly 6 characters");
        } else {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                // Removed email verification sending
                                databaseReference.child(user.getUid()).setValue(new User(name, id, email))
                                        .addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                announceMessage("Registration successful! You can now log in.");
                                                // Removed firebaseAuth.signOut();
                                                startActivity(new Intent(SignUp.this, login.class));
                                                finish();
                                            } else {
                                                announceMessage("Error saving user data: " + dbTask.getException().getMessage());
                                            }
                                        });
                            }
                        } else {
                            announceMessage("Error: " + task.getException().getMessage());
                        }
                    });
        }
    }

    private void announceMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public static class User {
        public String name, id, email;

        public User() {}

        public User(String name, String id, String email) {
            this.name = name;
            this.id = id;
            this.email = email;
        }
    }
}