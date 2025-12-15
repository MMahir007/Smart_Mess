package edu.ewubd.smartmessMN;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Expense_List extends AppCompatActivity {
    private TextView tvTotalAmounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tvTotalAmounts = findViewById(R.id.tvTotalAmounts);
    }
}