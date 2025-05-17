package com.example.helpinghands;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.joery.animatedbottombar.AnimatedBottomBar;

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
    private AnimatedBottomBar bottomBar;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);

        // Disable page number announcements by hiding the action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Ensure the title is not spoken by TalkBack
        setTitle(" ");

        //Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        setupRecyclerView();
        setupSwipeRefresh();

        // Get current user and refresh leaderboard
        fetchCurrentUserAndRefresh();



        bottomBar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, AnimatedBottomBar.Tab lastTab, int newIndex, AnimatedBottomBar.Tab newTab) {
                handleTabSelection(newIndex);
            }

            @Override
            public void onTabReselected(int index, AnimatedBottomBar.Tab tab) {
                handleTabReselection(index);
            }
        });
    }
    private void handleTabSelection(int newIndex) {
        switch (newIndex) {
            case 0:
            // Redirect to Home page
            Intent boardIntent = new Intent(LeaderboardActivity.this, Home.class);
            startActivity(boardIntent);
            break;

            case 1:

                // Redirect to Profile page
                Intent profileIntent = new Intent(LeaderboardActivity.this, Profile.class);
                startActivity(profileIntent);
                break;


            case 2:
                // Redirect to Maps page
                Intent mapsIntent = new Intent(LeaderboardActivity.this, Mapping.class);
                startActivity(mapsIntent);
                break;


            case 3:
                // Redirect to Home page
                // Optional: Do nothing if it's the same page
                break;

            default:
                // Handle unexpected index
                Toast.makeText(LeaderboardActivity.this, "Unknown Tab Selected", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void handleTabReselection(int index) {
        // Handle tab reselection logic if needed
        Toast.makeText(LeaderboardActivity.this, "Tab Reselected: " + index, Toast.LENGTH_SHORT).show();
    }

    private void initializeViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyStateView = findViewById(R.id.emptyStateView);

        firstPlaceLayout = findViewById(R.id.firstPlaceLayout);
        firstPlaceName = findViewById(R.id.firstPlaceName);
        firstPlaceHours = findViewById(R.id.firstPlaceHours);

        secondPlaceLayout = findViewById(R.id.secondPlaceLayout);
        secondPlaceName = findViewById(R.id.secondPlaceName);
        secondPlaceHours = findViewById(R.id.secondPlaceHours);

        thirdPlaceLayout = findViewById(R.id.thirdPlaceLayout);
        thirdPlaceName = findViewById(R.id.thirdPlaceName);
        thirdPlaceHours = findViewById(R.id.thirdPlaceHours);

        remainingStudentsRecyclerView = findViewById(R.id.remainingStudentsRecyclerView);
        bottomBar = findViewById(R.id.bottom);
    }

    private void setupRecyclerView() {
        remainingStudentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(new ArrayList<>());
        remainingStudentsRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> refreshLeaderboard());
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }
    private void fetchCurrentUserAndRefresh() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            databaseReference.child(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DataSnapshot snapshot = task.getResult();
                    currentUserName = snapshot.child("name").getValue(String.class);
                    refreshLeaderboard();
                } else {
                    Log.e("Leaderboard", "Error fetching user name", task.getException());
                    refreshLeaderboard();
                }
            });
        } else {
            refreshLeaderboard();
        }
    }

    private void refreshLeaderboard() {
        swipeRefreshLayout.setRefreshing(true);
        allStudents.clear();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collectionGroup("logs").get().addOnCompleteListener(task -> {
            swipeRefreshLayout.setRefreshing(false);

            if (task.isSuccessful()) {
                Map<String, Student> studentMap = new HashMap<>();

                for (QueryDocumentSnapshot logDoc : task.getResult()) {
                    try {
                        String studentName = logDoc.getString("studentName");
                        String hoursStr = logDoc.getString("hours");

                        if (studentName != null && hoursStr != null) {
                            int hours = Integer.parseInt(hoursStr);

                            if (studentMap.containsKey(studentName)) {
                                Student existing = studentMap.get(studentName);
                                existing.setHours(existing.getHours() + hours);
                            } else {
                                studentMap.put(studentName, new Student(studentName, hours));
                            }
                        }
                    } catch (NumberFormatException e) {
                        Log.e("Leaderboard", "Error parsing log data", e);
                    }
                }

                allStudents.addAll(studentMap.values());
                Collections.sort(allStudents, (s1, s2) -> Integer.compare(s2.getHours(), s1.getHours()));
                updateUI();

                // Highlight current user after data loads
                if (currentUserName != null) {
                    adapter.setCurrentUserName(currentUserName);
                }
            } else {
                Log.e("Leaderboard", "Error getting logs", task.getException());
                Toast.makeText(this, "Failed to load leaderboard", Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }

    private void updateUI() {
        // Update top 3 students
        updateTopStudent(0, firstPlaceName, firstPlaceHours, "1st Place");
        updateTopStudent(1, secondPlaceName, secondPlaceHours, "2nd Place");
        updateTopStudent(2, thirdPlaceName, thirdPlaceHours, "3rd Place");

        // Update remaining students (position 3+)
        List<Student> remainingStudents = allStudents.size() > 3 ?
                allStudents.subList(3, allStudents.size()) : new ArrayList<>();
        adapter.updateData(remainingStudents);

        // Show empty state if no students, otherwise show leaderboard
        boolean isEmpty = allStudents.isEmpty();
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        // Show/hide leaderboard sections
        firstPlaceLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        secondPlaceLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        thirdPlaceLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        remainingStudentsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateTopStudent(int position, TextView nameView, TextView hoursView, String placeholder) {
        if (allStudents.size() > position) {
            Student student = allStudents.get(position);
            nameView.setText(student.getName());
            hoursView.setText(student.getHours() + " hours");
        } else {
            nameView.setText(placeholder);
            hoursView.setText("0 hours");
        }
    }
}