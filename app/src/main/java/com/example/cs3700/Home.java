package com.example.cs3700;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.FirebaseFirestore;

public class Home extends AppCompatActivity {

    FirebaseFirestore firestore;
    ImageView profileImage;
    TextView textView6;
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();
        profileImage=findViewById(R.id.profileImage);
        textView6=findViewById(R.id.textView6);

        ImageView imageHospice = findViewById(R.id.imageHospice);
        ImageView imageRehab = findViewById(R.id.imageRehab);
        ImageView imageHealth = findViewById(R.id.imageHealth);
        ImageView imageChildren = findViewById(R.id.imageChildren);
        ImageView imageRescue = findViewById(R.id.imageRescue);
        ImageView imageSpecial = findViewById(R.id.imageSpecial);

        imageHospice.setOnClickListener(v -> {selectedCategory = "Hospice";openCategory(selectedCategory);});
        imageSpecial.setOnClickListener(v -> {selectedCategory = "SpecialNeeds";openCategory(selectedCategory);});
        imageRehab.setOnClickListener(v -> {selectedCategory = "Rehab";openCategory(selectedCategory);});
        imageHealth.setOnClickListener(v -> {selectedCategory = "HealthCenters";openCategory(selectedCategory);});
        imageChildren.setOnClickListener(v -> {selectedCategory = "ChildrenHomes";openCategory(selectedCategory);});
        imageRescue.setOnClickListener(v -> {selectedCategory = "RescueCenter";openCategory(selectedCategory);});

        profileImage.setOnClickListener(view -> {
            Intent intent = new Intent(Home.this, Profile.class);
            startActivity(intent);
        });

        ImageView searchIcon = findViewById(R.id.searchIcon);
        EditText searchBar = findViewById(R.id.searchBar);

        searchIcon.setOnClickListener(v -> {
            String query = searchBar.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(Home.this, "Please enter a search query", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Home.this, Recycle.class);
            intent.putExtra("SEARCH_QUERY", query);
            intent.putExtra("CATEGORY_NAME", selectedCategory);
            startActivity(intent);
        });

    }
    private void openCategory(String categoryName) {
        Intent intent = new Intent(Home.this, Recycle.class);
        intent.putExtra("CATEGORY_NAME", categoryName);
        startActivity(intent);
    }
}