package com.example.helpinghands;


import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {

    // Top student views
    private LinearLayout firstPlaceLayout, secondPlaceLayout, thirdPlaceLayout;
    private TextView firstPlaceName, firstPlaceHours;
    private TextView secondPlaceName, secondPlaceHours;
    private TextView thirdPlaceName, thirdPlaceHours;

    // Other views
    private RecyclerView remainingStudentsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateView;
    private LeaderboardAdapter adapter;
    private List<Student> allStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);

        // Initialize views
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyStateView = findViewById(R.id.emptyStateView);

        // Initialize top student views
        firstPlaceLayout = findViewById(R.id.firstPlaceLayout);
        firstPlaceName = findViewById(R.id.firstPlaceName);
        firstPlaceHours = findViewById(R.id.firstPlaceHours);

        secondPlaceLayout = findViewById(R.id.secondPlaceLayout);
        secondPlaceName = findViewById(R.id.secondPlaceName);
        secondPlaceHours = findViewById(R.id.secondPlaceHours);

        thirdPlaceLayout = findViewById(R.id.thirdPlaceLayout);
        thirdPlaceName = findViewById(R.id.thirdPlaceName);
        thirdPlaceHours = findViewById(R.id.thirdPlaceHours);

        // Initialize RecyclerView
        remainingStudentsRecyclerView = findViewById(R.id.remainingStudentsRecyclerView);
        remainingStudentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(new ArrayList<>());
        remainingStudentsRecyclerView.setAdapter(adapter);

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(() -> refreshLeaderboard());
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        // Load initial data
        refreshLeaderboard();
    }

    private void refreshLeaderboard() {
        swipeRefreshLayout.setRefreshing(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collectionGroup("logs").get().addOnCompleteListener(task -> {
            swipeRefreshLayout.setRefreshing(false);

            if (task.isSuccessful()) {
                allStudents.clear();
                Map<String, Student> studentMap = new HashMap<>();

                for (QueryDocumentSnapshot logDoc : task.getResult()) {
                    String studentName = logDoc.getString("studentName");
                    String hoursStr = logDoc.getString("hours");

                    if (studentName != null && hoursStr != null) {
                        try {
                            int hours = Integer.parseInt(hoursStr);
                            Student existing = studentMap.get(studentName);
                            if (existing != null) {
                                existing.setHours(existing.getHours() + hours);
                            } else {
                                studentMap.put(studentName, new Student(studentName, hours));
                            }
                        } catch (NumberFormatException e) {
                            Log.e("Leaderboard", "Invalid hours format", e);
                        }
                    }
                }

                allStudents.addAll(studentMap.values());
                Collections.sort(allStudents, (s1, s2) -> Integer.compare(s2.getHours(), s1.getHours()));

                updateUI();
            } else {
                Toast.makeText(this, "Error loading leaderboard", Toast.LENGTH_SHORT).show();
                updateUI(); // Still update UI to show empty state if needed
            }
        });
    }

    private void updateUI() {
        // Always show the top 3 placeholders (empty if no data)
        firstPlaceLayout.setVisibility(View.VISIBLE);
        secondPlaceLayout.setVisibility(View.VISIBLE);
        thirdPlaceLayout.setVisibility(View.VISIBLE);

        // Update top students with actual data if available
        if (allStudents.size() > 0) {
            firstPlaceName.setText(allStudents.get(0).getName());
            firstPlaceHours.setText(allStudents.get(0).getHours() + " hours");

        } else {
            firstPlaceName.setText("1st Place");
            firstPlaceHours.setText("0 hours");
        }

        if (allStudents.size() > 1) {
            secondPlaceName.setText(allStudents.get(1).getName());
            secondPlaceHours.setText(allStudents.get(1).getHours() + " hours");
        } else {
            secondPlaceName.setText("2nd Place");
            secondPlaceHours.setText("0 hours");
        }

        if (allStudents.size() > 2) {
            thirdPlaceName.setText(allStudents.get(2).getName());
            thirdPlaceHours.setText(allStudents.get(2).getHours() + " hours");
        } else {
            thirdPlaceName.setText("3rd Place");
            thirdPlaceHours.setText("0 hours");
        }

        // Update remaining students (starting from 4th position)
        List<Student> remainingStudents = allStudents.size() > 3 ?
                allStudents.subList(3, allStudents.size()) : new ArrayList<>();
        adapter.updateData(remainingStudents);

        // Show empty state if no students found
        if (allStudents.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            remainingStudentsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            remainingStudentsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

}