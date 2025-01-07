package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaos.view.PinView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import io.github.muddz.styleabletoast.StyleableToast;

public class Verify extends AppCompatActivity {
    private TextView instructionTextView;
    private PinView otpPinView;
    private Button verifyOtpButton;
    private String name, id, email, userId, receivedOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        instructionTextView = findViewById(R.id.text);
        otpPinView = findViewById(R.id.pins);
        verifyOtpButton = findViewById(R.id.verifyButton);

        // Set instruction text
        if (email != null) {
            instructionTextView.setText("Enter the 4-digit code sent to " + email);
        }

        // OTP verification
        verifyOtpButton.setOnClickListener(view -> {

        });
    }


}
