package com.example.helpinghands;

public class model {
    private String head;
    private int availableSlots;
    private int totalSlots;
    private String description;
    private String contact;
    private double latitude;
    private double longitude;

    public model(String head, int availableSlots, int totalSlots, String description, String contact, double latitude, double longitude) {
        this.head = head;
        this.availableSlots = availableSlots;
        this.totalSlots = totalSlots;
        this.description = description;
        this.contact=contact;
        this.latitude = latitude;
        this.longitude = longitude;
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
    public String getContact() {
        return contact;
    }
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
