package com.example.myapplication.model;

public class Budget {
    private int id;
    private int userId;
    private double weeklyLimit;

    public Budget() {
    }

    public Budget(int id, int userId, double weeklyLimit) {
        this.id = id;
        this.userId = userId;
        this.weeklyLimit = weeklyLimit;
    }

    public Budget(int userId, double weeklyLimit) {
        this.userId = userId;
        this.weeklyLimit = weeklyLimit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getWeeklyLimit() {
        return weeklyLimit;
    }

    public void setWeeklyLimit(double weeklyLimit) {
        this.weeklyLimit = weeklyLimit;
    }
}
