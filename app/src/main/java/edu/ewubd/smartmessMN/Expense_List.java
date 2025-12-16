package edu.ewubd.smartmessMN;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Expense_List extends AppCompatActivity {

    // Views
    private TextView tvBalance, tvDueDate;
    // Changed to EditText as per your request
    private EditText etBillRent, etBillElectric, etBillGas, etBillWater, etBillUtility, etBillInternet;
    private ListView lvGrocery, lvOther;
    private Button btnAddExpense;

    // Adapters & Lists
    private ExpenseAdapter groceryAdapter, otherAdapter;
    private ArrayList<ExpenseItem> groceryList, otherList;

    // Firebase
    private DatabaseReference mDatabase;
    private String currentMessCode, currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        // 1. Initialize Views
        tvBalance = findViewById(R.id.tv_balance_amount);
        tvDueDate = findViewById(R.id.tv_due_date);

        // Bill EditTexts (Ensure IDs match your XML)
        etBillRent = findViewById(R.id.et_bill_rent); // Note: XML ID might still be 'tv_', check your XML!
        etBillElectric = findViewById(R.id.et_bill_electric);
        etBillGas = findViewById(R.id.et_bill_gas);
        etBillWater = findViewById(R.id.et_bill_water);
        etBillUtility = findViewById(R.id.et_bill_utility);
        etBillInternet = findViewById(R.id.et_bill_internet);

        // Lists
        lvGrocery = findViewById(R.id.lv_grocery_items);
        lvOther = findViewById(R.id.lv_other_items);
        btnAddExpense = findViewById(R.id.btn_add_expense);

        // 2. Initialize Firebase & Data
        mDatabase = FirebaseDatabase.getInstance().getReference();

        currentMessCode = getIntent().getStringExtra("messCode");
        currentUserRole = getIntent().getStringExtra("role");

        groceryList = new ArrayList<>();
        otherList = new ArrayList<>();

        groceryAdapter = new ExpenseAdapter(this, groceryList);
        otherAdapter = new ExpenseAdapter(this, otherList);

        lvGrocery.setAdapter(groceryAdapter);
        lvOther.setAdapter(otherAdapter);

        // 3. Permission Logic (Manager vs Member)
        setupPermissions();

        // 4. Load Data
        if (currentMessCode != null) {
            loadBills();
            loadExpenses();
        } else {
            Toast.makeText(this, "Error: Mess Code Missing", Toast.LENGTH_SHORT).show();
        }

        // 5. Button Action
        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(Expense_List.this, Add_Expenses.class);
            intent.putExtra("messCode", currentMessCode);
            intent.putExtra("role", currentUserRole);
            startActivity(intent);
        });
    }

    // --- PERMISSION & EDIT LOGIC ---
    private void setupPermissions() {
        boolean isManager = "Manager".equalsIgnoreCase(currentUserRole);

        // Enable editing ONLY if Manager
        etBillRent.setEnabled(isManager);
        etBillElectric.setEnabled(isManager);
        etBillGas.setEnabled(isManager);
        etBillWater.setEnabled(isManager);
        etBillUtility.setEnabled(isManager);
        etBillInternet.setEnabled(isManager);

        // If Manager, attach listeners to save changes to DB automatically
        if (isManager) {
            attachSaveListener(etBillRent, "Home Rent");
            attachSaveListener(etBillElectric, "Electric Bill");
            attachSaveListener(etBillGas, "Gas Bill");
            attachSaveListener(etBillWater, "Water Bill");
            attachSaveListener(etBillUtility, "Utility");
            attachSaveListener(etBillInternet, "Internet");
        }
    }

    private void attachSaveListener(EditText editText, String billKey) {
        // Saves data when the user finishes typing and clicks away (loses focus)
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String valStr = editText.getText().toString().trim();
                if (!valStr.isEmpty()) {
                    try {
                        double amount = Double.parseDouble(valStr);
                        // Save to Firebase: mess/{code}/bills/{billKey}
                        mDatabase.child("mess").child(currentMessCode).child("bills").child(billKey).setValue(amount);
                    } catch (NumberFormatException e) {
                        editText.setError("Invalid Number");
                    }
                }
            }
        });
    }

    // --- DATA LOADING LOGIC ---
    private void loadBills() {
        mDatabase.child("mess").child(currentMessCode).child("bills")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Helper to safely get string, default to "0"
                            updateBillField(etBillRent, snapshot.child("Home Rent"));
                            updateBillField(etBillElectric, snapshot.child("Electric Bill"));
                            updateBillField(etBillGas, snapshot.child("Gas Bill"));
                            updateBillField(etBillWater, snapshot.child("Water Bill"));
                            updateBillField(etBillUtility, snapshot.child("Utility"));
                            updateBillField(etBillInternet, snapshot.child("Internet"));

                            calculateTotalBalance();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateBillField(EditText et, DataSnapshot snap) {
        // Only update text if the user is NOT currently typing (to avoid cursor jumping)
        if (!et.hasFocus()) {
            Object val = snap.getValue();
            et.setText(val == null ? "0" : String.valueOf(val));
        }
    }

    private void loadExpenses() {
        mDatabase.child("mess").child(currentMessCode).child("expenses")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        groceryList.clear();
                        otherList.clear();
                        double totalExpenseSum = 0;

                        for (DataSnapshot data : snapshot.getChildren()) {
                            ExpenseItem expense = data.getValue(ExpenseItem.class);
                            if (expense != null) {
                                totalExpenseSum += expense.amount;

                                if ("Grocery".equalsIgnoreCase(expense.type)) {
                                    groceryList.add(expense);
                                } else {
                                    otherList.add(expense);
                                }
                            }
                        }
                        groceryAdapter.notifyDataSetChanged();
                        otherAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Expense_List.this, "Failed to load expenses", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateTotalBalance() {
        mDatabase.child("mess").child(currentMessCode).child("balance")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object balance = snapshot.getValue();
                        if (balance != null) {
                            tvBalance.setText("TK. " + balance);
                        } else {
                            tvBalance.setText("TK. 0");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}