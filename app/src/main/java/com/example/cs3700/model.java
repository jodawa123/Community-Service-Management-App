package com.example.cs3700;

public class model {
    private String head;
    private int availableSlots;
    private int totalSlots;
    private String description;

    public model(String head, int availableSlots, int totalSlots, String description) {
        this.head = head;
        this.availableSlots = availableSlots;
        this.totalSlots = totalSlots;
        this.description = description;
    }

    public String getHead() {
        return head;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) { // Add this setter method
        this.availableSlots = availableSlots;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public String getDescription() {
        return description;
    }
}
