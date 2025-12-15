package edu.ewubd.smartmessMN;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Calculation_Summary extends AppCompatActivity {
    private TextView total_expenses, totalrate;
    private ListView lvExpense;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        total_expenses = findViewById(R.id.total_expenses);
        totalrate = findViewById(R.id.totalrate);
        lvExpense = findViewById(R.id.lvExpense);

    }
}