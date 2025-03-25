package com.example.helpinghands;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<Student> students;

    public LeaderboardAdapter(List<Student> students) {
        this.students = students;
    }

    public void updateData(List<Student> newStudents) {
        this.students = newStudents;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_remaining_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Student student = students.get(position);
        holder.rankTextView.setText(String.valueOf(position + 4)); // Starting from 4
        holder.nameTextView.setText(student.getName());
        holder.hoursTextView.setText(student.getHours() + " hours");
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView rankTextView;
        public TextView nameTextView;
        public TextView hoursTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.rankTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            hoursTextView = itemView.findViewById(R.id.hoursTextView);
        }
    }
}