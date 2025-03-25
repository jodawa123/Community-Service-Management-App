package com.example.helpinghands;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Reviewmodel> reviewList;

    public ReviewAdapter(List<Reviewmodel> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Reviewmodel review = reviewList.get(position);
        holder.reviewerName.setText(review.getReviewerName());
        holder.reviewTime.setText(review.getReviewTime());
        holder.reviewComment.setText(review.getComment());

        // Set the rating for the RatingBar
        holder.ratingBar.setRating(review.getRating());
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerName, reviewTime, reviewComment;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerName = itemView.findViewById(R.id.reviewerName);
            reviewTime = itemView.findViewById(R.id.reviewTime);
            reviewComment = itemView.findViewById(R.id.reviewComment);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}