package edu.ewubd.smartmessMN;

public class MealHistoryItem {
    public String uid;
    public String userName;
    public double breakfast;
    public double lunch;
    public double dinner;
    public double total;

    public MealHistoryItem(String uid, String userName, double b, double l, double d) {
        this.uid = uid;
        this.userName = userName;
        this.breakfast = b;
        this.lunch = l;
        this.dinner = d;
        this.total = b + l + d; // Auto-calculate total
    }
}