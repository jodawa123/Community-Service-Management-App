
package com.example.cs3700;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class itemAdapter extends RecyclerView.Adapter<itemAdapter.Holder> {
    private final Context context;
    private List<model> modelArrayList; // Use List for easier handling of filtering
    private final List<model> originalList; // Backup of the full dataset
    private final String categoryName; // Dynamically pass the category name
    private int selectedPosition = -1;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String userSelectedSite = null; // Track user's selected site
    private String searchQuery;
    private int highlightPosition = -1;

    public itemAdapter(Context context, ArrayList<model> modelArrayList, String categoryName, String userSelectedSite, String searchQuery,int highlightPosition) {
        this.context = context;
        this.modelArrayList = modelArrayList;
        this.originalList = new ArrayList<>(modelArrayList); // Backup the original list
        this.categoryName = categoryName;
        this.userSelectedSite = userSelectedSite; // Pass the user's selected site
        this.searchQuery = searchQuery;
        this.highlightPosition = highlightPosition;
        fetchUserSelection(); // Optional: Can be skipped if passed via constructor
    }

    private void fetchUserSelection() {
        if (currentUser != null) {
            firestore.collection("UserSelections")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("selectedSite")) {
                            userSelectedSite = documentSnapshot.getString("selectedSite");
                            notifyDataSetChanged(); // Update the UI
                        }
                    });
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.items, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        model currentModel = modelArrayList.get(position);

        holder.siteTitle.setText(currentModel.getHead());
        holder.studentsNeeded.setText(currentModel.getAvailableSlots() + "/" + currentModel.getTotalSlots());
        holder.siteDescription.setText(currentModel.getDescription());
        holder.phone.setText(currentModel.getContact());

        // Highlight and animate the searched item
        // Highlight and animate the searched item
        if (position==highlightPosition) {
            Animation animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.pulsating_animation);
            holder.itemView.startAnimation(animation);
             // Optional: Highlight color
        } else {
            holder.itemView.clearAnimation();
        }

        boolean isSiteAlreadyPicked = currentModel.getHead().equals(userSelectedSite);

        holder.radioButton.setChecked(position == selectedPosition || isSiteAlreadyPicked);
        holder.radioButton.setEnabled(!isSiteAlreadyPicked && currentModel.getAvailableSlots() > 0);

        holder.radioButton.setOnClickListener(v -> {
            if (userSelectedSite != null) {
                Toast.makeText(context, "You have already selected a site: " + userSelectedSite, Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentModel.getAvailableSlots() > 0) {
                queryAndUpdateSite(currentModel, position);
            } else {
                Toast.makeText(context, "No available slots for this site", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void queryAndUpdateSite(model currentModel, int position) {
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to pick a site", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection(categoryName)
                .whereEqualTo("head", currentModel.getHead())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        DocumentReference siteRef = document.getReference();

                        int newAvailableSlots = currentModel.getAvailableSlots() - 1;
                        currentModel.setAvailableSlots(newAvailableSlots);

                        siteRef.update("availableSlots", newAvailableSlots)
                                .addOnSuccessListener(aVoid -> saveMemberDetails(siteRef, currentModel, position))
                                .addOnFailureListener(e -> Toast.makeText(context, "Failed to update slots: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(context, "No matching site found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error querying site: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void saveMemberDetails(DocumentReference siteRef, model currentModel, int position) {
        String userId = currentUser.getUid();

        fetchUserData(userId, (userName, userCustomId) -> {
            if (userName != null && userCustomId != null) {
                Map<String, Object> memberData = new HashMap<>();
                memberData.put("id", userCustomId);
                memberData.put("name", userName);

                siteRef.collection("members").document(userId)
                        .set(memberData)
                        .addOnSuccessListener(aVoid -> {
                            saveUserSelection(currentModel, position); // Save user selection after saving member
                            Toast.makeText(context, "Member added successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Failed to add member: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(context, "User details not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void fetchUserData(String userId, UserDataCallback callback) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        databaseReference.get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                String userName = dataSnapshot.child("name").getValue(String.class);
                String userCustomId = dataSnapshot.child("id").getValue(String.class);
                callback.onUserDataFetched(userName, userCustomId);
            } else {
                callback.onUserDataFetched(null, null);
            }
        }).addOnFailureListener(e -> callback.onUserDataFetched(null, null));
    }

    // Callback interface for user data
    interface UserDataCallback {
        void onUserDataFetched(String userName, String userCustomId);
    }


    private void saveUserSelection(model currentModel, int position) {
        firestore.collection("UserSelections")
                .document(currentUser.getUid())
                .set(new UserSelection(currentModel.getHead()))
                .addOnSuccessListener(aVoid -> {
                    userSelectedSite = currentModel.getHead();
                    int previousPosition = selectedPosition;
                    selectedPosition = position;

                    notifyItemChanged(previousPosition);
                    notifyItemChanged(selectedPosition);

                    Toast.makeText(context, "You selected: " + currentModel.getHead(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(context, Profile.class);
                    intent.putExtra("CATEGORY_NAME", categoryName);
                    intent.putExtra("SELECTED_SITE", currentModel.getHead());
                    context.startActivity(intent);

                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to save user selection: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return modelArrayList.size();
    }

    // Method to filter the list based on a search query
    public void filter(String query) {
        searchQuery = query; // Update the searchQuery field
        if (query == null || query.isEmpty()) {
            modelArrayList = new ArrayList<>(originalList); // Reset to the original list
        } else {
            List<model> filteredList = new ArrayList<>();
            for (model item : originalList) {
                if (item.getHead().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
            modelArrayList = filteredList;
        }
        notifyDataSetChanged(); // Notify the RecyclerView of the dataset change
    }

    public static class Holder extends RecyclerView.ViewHolder {
        TextView siteTitle, studentsNeeded, siteDescription,phone;
        RadioButton radioButton;

        public Holder(@NonNull View itemView) {
            super(itemView);
            siteTitle = itemView.findViewById(R.id.site_title);
            studentsNeeded = itemView.findViewById(R.id.students_needed);
            siteDescription = itemView.findViewById(R.id.site_description);
            radioButton = itemView.findViewById(R.id.site_radio_button);
            phone=itemView.findViewById(R.id.site_phone);
        }
    }

    public static class UserSelection {
        private String selectedSite;

        public UserSelection(String selectedSite) {
            this.selectedSite = selectedSite;
        }

        public String getSelectedSite() {
            return selectedSite;
        }

        public void setSelectedSite(String selectedSite) {
            this.selectedSite = selectedSite;
        }
    }
    public static class Member {
        private String id;
        private String name;

        public Member(String id, String name) {
            this.id = id;
            this.name = name;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
