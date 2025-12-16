package edu.ewubd.smartmessMN;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Add_Expenses extends AppCompatActivity {

    // Views
    private EditText etDate, etItemName, etItemUser, etItemAmount;
    private RadioGroup rgCategory;
    private LinearLayout containerUserSelection;
    private Button btnSave, btnCancel;

    // Data
    private String currentMessCode, currentUserRole;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private Calendar calendar;

    // Lookup Map for Managers: Name -> UID
    private Map<String, String> memberNameMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expenses);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 2. Get Data from Intent
        currentMessCode = getIntent().getStringExtra("messCode");
        currentUserRole = getIntent().getStringExtra("role");

        // 3. Bind Views
        etDate = findViewById(R.id.etSelectDate);
        etItemName = findViewById(R.id.et_item_name);
        etItemUser = findViewById(R.id.et_item_user);
        etItemAmount = findViewById(R.id.et_item_amount);

        rgCategory = findViewById(R.id.rg_category);
        containerUserSelection = findViewById(R.id.container_user_selection);

        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        calendar = Calendar.getInstance();

        // 4. Setup
        setupDatePicker();
        setupRoleLogic();

        // 5. Button Listeners
        btnSave.setOnClickListener(v -> handleSave());
        btnCancel.setOnClickListener(v -> finish());
    }

    // --- SETUP METHODS ---

    private void setupDatePicker() {
        updateDateLabel(); // Set today's date initially
        etDate.setOnClickListener(v -> {
            new DatePickerDialog(Add_Expenses.this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void updateDateLabel() {
        String myFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        etDate.setText(sdf.format(calendar.getTime()));
    }

    private void setupRoleLogic() {
        if ("Manager".equalsIgnoreCase(currentUserRole)) {
            // SHOW User Input Field
            containerUserSelection.setVisibility(View.VISIBLE);
            // Fetch all members to map Names to UIDs for verification
            preloadMemberData();
        } else {
            // HIDE User Input Field
            containerUserSelection.setVisibility(View.GONE);
        }
    }

    // --- DATA PRELOADING (For Managers) ---
    private void preloadMemberData() {
        //
        // Strategy: Search "users" node where "messCode" matches the current one
        mDatabase.child("users").orderByChild("messCode").equalTo(currentMessCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        memberNameMap.clear();
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String name = userSnap.child("name").getValue(String.class);
                            String uid = userSnap.getKey();
                            if (name != null && uid != null) {
                                // Store lowercase name for easy case-insensitive matching
                                memberNameMap.put(name.toLowerCase().trim(), uid);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Add_Expenses.this, "Failed to load member list", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- SAVE LOGIC ---

    private void handleSave() {
        if (currentMessCode == null) return;

        // 1. Basic Validation
        String date = etDate.getText().toString();
        String itemName = etItemName.getText().toString().trim();
        String amountStr = etItemAmount.getText().toString().trim();

        if (itemName.isEmpty()) {
            etItemName.setError("Enter item name");
            return;
        }
        if (amountStr.isEmpty()) {
            etItemAmount.setError("Enter amount");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String category = (rgCategory.getCheckedRadioButtonId() == R.id.rb_grocery) ? "Grocery" : "Other";

        // 2. Resolve User ID
        if ("Manager".equalsIgnoreCase(currentUserRole)) {
            // MANAGER FLOW: Find UID based on entered Name
            String enteredName = etItemUser.getText().toString().trim();
            if (enteredName.isEmpty()) {
                etItemUser.setError("Enter a member name");
                return;
            }

            // Lookup UID from the map we preloaded
            String targetUid = memberNameMap.get(enteredName.toLowerCase());

            if (targetUid != null) {
                // Success: Found the member!
                saveToFirebase(date, itemName, amount, category, targetUid, enteredName); // Use entered name for display
            } else {
                // Failure: Name not found in this mess
                etItemUser.setError("Member not found! Check spelling.");
                Toast.makeText(this, "Member not found in this mess", Toast.LENGTH_SHORT).show();
            }

        } else {
            // MEMBER FLOW: Use their own UID
            String myUid = mAuth.getCurrentUser().getUid();
            String myName = mAuth.getCurrentUser().getDisplayName(); // Optional: or fetch from DB
            if(myName == null || myName.isEmpty()) myName = "Member";

            saveToFirebase(date, itemName, amount, category, myUid, myName);
        }
    }

    private void saveToFirebase(String date, String item, double amount, String type, String uid, String userName) {
        String expenseId = mDatabase.child("mess").child(currentMessCode).child("expenses").push().getKey();

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("id", expenseId);
        expenseData.put("date", date);
        expenseData.put("item", item);
        expenseData.put("amount", amount);
        expenseData.put("type", type);
        expenseData.put("uid", uid);       // The internal User ID (for logic)
        expenseData.put("user", userName); // The Display Name (for the list view)

        if (expenseId != null) {
            mDatabase.child("mess").child(currentMessCode).child("expenses").child(expenseId).setValue(expenseData)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Expense Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
        }
    }
}