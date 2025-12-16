package edu.ewubd.smartmessMN;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // --- XML Views ---
    // Header & Date
    private ImageView btnNotification, btnLogout;
    private TextView tvCurrentDate;

    // Menu Summaries
    private TextView tvMenuBreakfast, tvMenuLunch, tvMenuDinner;

    // Quick Action Buttons
    private LinearLayout btnAddMeal, btnExpenses, btnMembers, btnChecklist;

    // Meal Entry Inputs
    private EditText etBreakfastQty, etLunchQty, etDinnerQty;
    private Button btnConfirmMeal;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference rdb;

    // User Data & State
    private String currentMessCode = null;
    private String currentUserRole = null;
    private String todayDateKey; // Format: yyyy-MM-dd (For Database)
    private String todayDateDisplay; // Format: MMM dd, yyyy (For UI)

    private static boolean persistenceEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        // 1. Persistence Logic
        if (!persistenceEnabled) {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                persistenceEnabled = true;
            } catch (Exception ignored) { }
        }

        mAuth = FirebaseAuth.getInstance();
        rdb = FirebaseDatabase.getInstance().getReference();

        // 2. Initialize Date Formats
        Date now = new Date();
        todayDateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now);
        todayDateDisplay = new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(now);

        // 3. View Bindings
        initViews();

        // 4. Set Initial UI State
        tvCurrentDate.setText(todayDateDisplay);

        // 5. Initial Auth Check & Data Loading
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            sendToLogin();
        } else {
            // Check Intent Extras first, otherwise fetch from DB
            currentMessCode = getIntent().getStringExtra("messCode");
            currentUserRole = getIntent().getStringExtra("role");

            if (currentMessCode == null || currentUserRole == null) {
                fetchUserDetails(user.getUid());
            } else {
                loadDashboardData();
            }
        }

        // 6. Set Click Listeners
        setClickListeners();
    }

    private void initViews() {
        // Header
        btnNotification = findViewById(R.id.btn_notification);
        btnLogout = findViewById(R.id.btn_logout);
        tvCurrentDate = findViewById(R.id.tv_current_date);

        // Menu
        tvMenuBreakfast = findViewById(R.id.tv_menu_breakfast);
        tvMenuLunch = findViewById(R.id.tv_menu_lunch);
        tvMenuDinner = findViewById(R.id.tv_menu_dinner);

        // Actions
        btnAddMeal = findViewById(R.id.btn_add_meal);
        btnExpenses = findViewById(R.id.btn_action_expenses);
        btnMembers = findViewById(R.id.btn_action_members);
        btnChecklist = findViewById(R.id.btn_checklist);

        // Meal Entry
        etBreakfastQty = findViewById(R.id.et_breakfast);
        etLunchQty = findViewById(R.id.et_lunch);
        etDinnerQty = findViewById(R.id.et_dinner);
        btnConfirmMeal = findViewById(R.id.btn_confirm_meal);
    }

    // --- DATA LOADING ---

    private void fetchUserDetails(String uid) {
        rdb.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentMessCode = snapshot.child("messCode").getValue(String.class);
                    currentUserRole = snapshot.child("role").getValue(String.class);
                    loadDashboardData();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDashboardData() {
        if (currentMessCode == null) return;

        // A. Load "Today's Menu"
        rdb.child("mess").child(currentMessCode).child("menu").child(todayDateKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String b = snapshot.child("breakfast").getValue(String.class);
                            String l = snapshot.child("lunch").getValue(String.class);
                            String d = snapshot.child("dinner").getValue(String.class);

                            if(b != null) tvMenuBreakfast.setText(b);
                            if(l != null) tvMenuLunch.setText(l);
                            if(d != null) tvMenuDinner.setText(d);
                        } else {
                            tvMenuBreakfast.setText("Not Set");
                            tvMenuLunch.setText("Not Set");
                            tvMenuDinner.setText("Not Set");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // B. Load "My Meal Entry" for Today (If already entered)
        // Path: mess/{messCode}/meals/{date}/{uid}
        String uid = mAuth.getCurrentUser().getUid();
        rdb.child("mess").child(currentMessCode).child("meals").child(todayDateKey).child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // If data exists, populate the EditTexts so user sees what they entered
                            Double b = snapshot.child("breakfast").getValue(Double.class);
                            Double l = snapshot.child("lunch").getValue(Double.class);
                            Double d = snapshot.child("dinner").getValue(Double.class);

                            // Only update text if view is not focused (prevents typing glitches)
                            if (!etBreakfastQty.hasFocus()) etBreakfastQty.setText(b == null ? "" : String.valueOf(b));
                            if (!etLunchQty.hasFocus()) etLunchQty.setText(l == null ? "" : String.valueOf(l));
                            if (!etDinnerQty.hasFocus()) etDinnerQty.setText(d == null ? "" : String.valueOf(d));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- MEAL ENTRY SAVE LOGIC ---

    private void saveMealEntry() {
        if (currentMessCode == null) return;

        String bStr = etBreakfastQty.getText().toString().trim();
        String lStr = etLunchQty.getText().toString().trim();
        String dStr = etDinnerQty.getText().toString().trim();

        // Parse inputs (Default to 0.0 if empty)
        double b = bStr.isEmpty() ? 0.0 : Double.parseDouble(bStr);
        double l = lStr.isEmpty() ? 0.0 : Double.parseDouble(lStr);
        double d = dStr.isEmpty() ? 0.0 : Double.parseDouble(dStr);

        // Prepare Data Object
        Map<String, Object> mealData = new HashMap<>();
        mealData.put("breakfast", b);
        mealData.put("lunch", l);
        mealData.put("dinner", d);
        // Optional: Calculate total for easy stats later
        mealData.put("total", b + l + d);

        // Path: mess/{messCode}/meals/{yyyy-MM-dd}/{uid}
        String uid = mAuth.getCurrentUser().getUid();

        rdb.child("mess").child(currentMessCode).child("meals").child(uid).child(todayDateKey)
                .setValue(mealData)
                .addOnSuccessListener(v -> Toast.makeText(MainActivity.this, "Meals Confirmed for " + todayDateDisplay, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to save meals", Toast.LENGTH_SHORT).show());
    }

    // --- NAVIGATION & LISTENERS ---

    private void setClickListeners() {
        // 1. Confirm Button
        btnConfirmMeal.setOnClickListener(v -> saveMealEntry());

        // 2. Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
            GoogleSignIn.getClient(MainActivity.this, gso).signOut();
            sendToLogin();
        });

        // 3. Notification (Placeholder)
        btnNotification.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "No new notifications", Toast.LENGTH_SHORT).show()
        );

        // 4. Quick Actions
        View.OnClickListener quickAction = v -> {
            if (currentMessCode == null) {
                Toast.makeText(this, "Loading data...", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = null;
            int id = v.getId();

            if (id == R.id.btn_add_meal) {
                intent = new Intent(MainActivity.this, Meal_History.class);
            } else if (id == R.id.btn_action_expenses) {
                intent = new Intent(MainActivity.this, Expense_List.class);
            } else if (id == R.id.btn_action_members) {
                intent = new Intent(MainActivity.this, Member_List.class);
            } else if (id == R.id.btn_checklist) {
                intent = new Intent(MainActivity.this, Meal_History.class);
            }

            if (intent != null) {
                intent.putExtra("messCode", currentMessCode);
                intent.putExtra("role", currentUserRole);
                startActivity(intent);
            }
        };

        btnAddMeal.setOnClickListener(quickAction);
        btnExpenses.setOnClickListener(quickAction);
        btnMembers.setOnClickListener(quickAction);
        btnChecklist.setOnClickListener(quickAction);
    }

    private void sendToLogin() {
        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}