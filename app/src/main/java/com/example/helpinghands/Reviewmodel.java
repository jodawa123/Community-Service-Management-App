package com.example.helpinghands;

public class Reviewmodel {
    private String reviewerName;
    private String reviewTime;
    private int rating;
    private String comment;

    // Constructor, getters, and setters
    public Reviewmodel(String reviewerName, String reviewTime, int rating, String comment) {
        this.reviewerName = reviewerName;
        this.reviewTime = reviewTime;
        this.rating = rating;
        this.comment = comment;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public String getReviewTime() {
        return reviewTime;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

}
