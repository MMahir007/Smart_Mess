package edu.ewubd.smartmess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
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
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LinearLayout MealD, MemberList, ExpenseL, MealH, CalculationB;
    private LinearLayout recentact;
    private TextView tvrecent;
    private Button btnLogout;
    private TextView todaymeal, totalex, totalmem;
    private TextView tvWelcomeRole; // Optional: To show "Manager Dashboard" etc.

    private FirebaseAuth mAuth;
    private DatabaseReference rdb;

    // Persist user details
    private String currentMessCode = null;
    private String currentUserRole = null;

    private static boolean persistenceEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        // 1. Persistence (Disk Cache)
        if (!persistenceEnabled) {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                persistenceEnabled = true;
            } catch (Exception ignored) { }
        }

        mAuth = FirebaseAuth.getInstance();
        rdb = FirebaseDatabase.getInstance().getReference();

        // 2. View Bindings
        MealD = findViewById(R.id.MealD);
        MemberList = findViewById(R.id.MemberList);
        ExpenseL = findViewById(R.id.ExpenseL);
        MealH = findViewById(R.id.MealH);
        CalculationB = findViewById(R.id.CalculationB);

        recentact = findViewById(R.id.recentact);
        btnLogout = findViewById(R.id.btnLogout);
        tvrecent = findViewById(R.id.tvrecent);

        todaymeal = findViewById(R.id.todaymeal);
        totalex = findViewById(R.id.totalex);
        totalmem = findViewById(R.id.totalmem);

        // 3. Get Data passed from Login/SignUp Activity
        currentMessCode = getIntent().getStringExtra("messCode");
        currentUserRole = getIntent().getStringExtra("role");

        // 4. Initial Check
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            sendToLogin();
        } else {
            // If Intent extras are missing (e.g. app restart), fetch from DB
            if (currentMessCode == null || currentUserRole == null) {
                fetchUserDetails(user.getUid());
            } else {
                loadDashboardStats(); // We have the code, load data immediately
            }
        }

        setClickListeners();
    }

    // --- DATA LOADING LOGIC ---

    private void fetchUserDetails(String uid) {
        // Read users/{uid} to get the messCode
        rdb.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentMessCode = snapshot.child("messCode").getValue(String.class);
                    currentUserRole = snapshot.child("role").getValue(String.class);

                    loadDashboardStats(); // Now we can load the data
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDashboardStats() {
        if (currentMessCode == null) return;

        // 1. Load Total Members
        // Path: mess/{messCode}/members
        rdb.child("mess").child(currentMessCode).child("members")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        totalmem.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 2. Load Today's Meals
        // Path: mess/{messCode}/meals/{date}/totalCount (You need to implement this structure later)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        // For now, let's just count total meals logged today
        // Note: You will need to align this with how you save meals in Meal_Dashboard
        rdb.child("mess").child(currentMessCode).child("meals").child(today)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Example logic: Summing up values
                        long count = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Assuming structure: meals -> date -> uid -> 2.5
                            Double val = child.getValue(Double.class);
                            if (val != null) count += val;
                        }
                        todaymeal.setText(String.valueOf(count));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 3. Load Total Expenses
        // Path: mess/{messCode}/expenses
        rdb.child("mess").child(currentMessCode).child("expenses")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long total = 0;
                        for (DataSnapshot d : snapshot.getChildren()) {
                            // Assuming structure: expenses -> expenseId -> { amount: 500 }
                            Long amount = d.child("amount").getValue(Long.class);
                            if (amount != null) {
                                total += amount;
                            }
                        }
                        totalex.setText("TK. " + total);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- NAVIGATION LOGIC ---

    private final View.OnClickListener quickAction = v -> {
        if (currentMessCode == null) {
            Toast.makeText(this, "Loading data... please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = null;
        int id = v.getId();

        if (id == R.id.MealD) {
            intent = new Intent(MainActivity.this, Meal_Dashboard.class);
        } else if (id == R.id.MemberList) {
            intent = new Intent(MainActivity.this, Member_List.class);
        } else if (id == R.id.ExpenseL) {
            intent = new Intent(MainActivity.this, Expense_List.class);
        } else if (id == R.id.MealH) {
            intent = new Intent(MainActivity.this, Meal_History.class);
        }

        if (intent != null) {
            // CRITICAL: Pass the messCode and role to the next activity
            intent.putExtra("messCode", currentMessCode);
            intent.putExtra("role", currentUserRole);
            startActivity(intent);
        }
    };

    private void setClickListeners() {
        MealD.setOnClickListener(quickAction);
        MemberList.setOnClickListener(quickAction);
        ExpenseL.setOnClickListener(quickAction);
        MealH.setOnClickListener(quickAction);

        btnLogout.setOnClickListener(view -> {
            mAuth.signOut();
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
            GoogleSignIn.getClient(MainActivity.this, gso).signOut();
            sendToLogin();
        });
    }

    private void sendToLogin() {
        Intent intent = new Intent(MainActivity.this, Login.class); // Or SignUp
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}