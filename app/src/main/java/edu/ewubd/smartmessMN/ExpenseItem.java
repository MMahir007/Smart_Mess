package edu.ewubd.smartmessMN;

public class ExpenseItem {
    public String id;
    public String user;
    public String item;
    public String date;
    public double amount;
    public String type; // "Grocery" or "Other"

    public ExpenseItem() {
        // Default constructor required for Firebase
    }

    public ExpenseItem(String id, String user, String item, String date, double amount, String type) {
        this.id = id;
        this.user = user;
        this.item = item;
        this.date = date;
        this.amount = amount;
        this.type = type;
    }
}