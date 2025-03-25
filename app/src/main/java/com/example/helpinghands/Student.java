package com.example.helpinghands;

public  class Student {
    private String name;
    private int hours;

    public Student(String name, int hours) {
        this.name = name;
        this.hours = hours;
    }

    public String getName() { return name; }
    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }
}