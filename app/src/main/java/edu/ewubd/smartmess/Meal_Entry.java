package edu.ewubd.smartmess;

public class Meal_Entry {
    String name;
    int breakfast;
    int lunch;
    int dinner;
    int total;

    public Meal_Entry(String name, int breakfast, int lunch, int dinner, int total) {
        this.name = name;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.total = total;
    }
}
