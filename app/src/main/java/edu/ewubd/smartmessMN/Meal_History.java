package edu.ewubd.smartmessMN;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Meal_History extends AppCompatActivity {

    // Views
    private EditText etSelectDate, etSearchMember;
    private ListView lvExpense;
    private ImageView icOpenCalendar;
    private LinearLayout searchContainer; // To hide search for members

    // Firebase & Data
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String currentMessCode, currentUserRole, currentUid;
    private Calendar calendar;

    // Lists
    private ArrayList<MealHistoryItem> fullList = new ArrayList<>();
    private ArrayList<MealHistoryItem> displayList = new ArrayList<>();
    private MealHistoryAdapter adapter;

    // Cache for User Names (UID -> Name)
    private Map<String, String> userMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_history);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = mAuth.getCurrentUser().getUid();

        currentMessCode = getIntent().getStringExtra("messCode");
        currentUserRole = getIntent().getStringExtra("role");

        // 2. Bind Views
        etSelectDate = findViewById(R.id.etSelectDate);
        etSearchMember = findViewById(R.id.etSearchMember);
        lvExpense = findViewById(R.id.lvExpense);
        icOpenCalendar = findViewById(R.id.icOpenCalendar);

        // Parent layout of the search bar (to hide it for members)
        // Note: You didn't give the Linear Layout an ID in XML, so we find it by the EditText's parent
        searchContainer = (LinearLayout) etSearchMember.getParent();

        // 3. Setup Adapter
        adapter = new MealHistoryAdapter(this, displayList);
        lvExpense.setAdapter(adapter);

        calendar = Calendar.getInstance();
        updateDateLabel();

        // 4. Permission & UI Setup
        setupRoleUI();

        // 5. Listeners
        etSelectDate.setOnClickListener(v -> showDatePicker());
        icOpenCalendar.setOnClickListener(v -> showDatePicker());

        etSearchMember.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { filter(s.toString()); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // 6. Initial Load
        if (currentMessCode != null) {
            refreshData();
        } else {
            Toast.makeText(this, "Mess Code Missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRoleUI() {
        if ("Manager".equalsIgnoreCase(currentUserRole)) {
            // Manager: Show search, see everyone
            searchContainer.setVisibility(View.VISIBLE);
        } else {
            // Member: Hide search, see only self
            searchContainer.setVisibility(View.GONE);
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateLabel();
            refreshData(); // Reload when date changes
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        etSelectDate.setText(sdf.format(calendar.getTime()));
    }

    private String getSelectedDateKey() {
        // Matches DB Format: yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(calendar.getTime());
    }

    private void refreshData() {
        if ("Manager".equalsIgnoreCase(currentUserRole)) {
            loadManagerData();
        } else {
            loadMemberData();
        }
    }

    // --- MANAGER LOGIC (See Everyone) ---
    private void loadManagerData() {
        // Step 1: Fetch ALL users in this mess to build the roster
        mDatabase.child("users").orderByChild("messCode").equalTo(currentMessCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userMap.clear();
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String uid = userSnap.getKey();
                            String name = userSnap.child("name").getValue(String.class);
                            if (name == null) name = "Unknown";
                            userMap.put(uid, name);
                        }
                        // Step 2: Once roster is ready, fetch meal data
                        fetchMealsForDateAndCombine();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchMealsForDateAndCombine() {
        String dateKey = getSelectedDateKey();

        mDatabase.child("mess").child(currentMessCode).child("meals").child(dateKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fullList.clear();

                        // Loop through ALL users in our roster (userMap)
                        // This ensures even users who didn't eat show up as 0/0/0
                        for (Map.Entry<String, String> entry : userMap.entrySet()) {
                            String uid = entry.getKey();
                            String name = entry.getValue();

                            double b = 0, l = 0, d = 0;

                            // Check if this UID has entries in the meals node
                            if (snapshot.hasChild(uid)) {
                                DataSnapshot mealSnap = snapshot.child(uid);
                                Double bVal = mealSnap.child("breakfast").getValue(Double.class);
                                Double lVal = mealSnap.child("lunch").getValue(Double.class);
                                Double dVal = mealSnap.child("dinner").getValue(Double.class);

                                if (bVal != null) b = bVal;
                                if (lVal != null) l = lVal;
                                if (dVal != null) d = dVal;
                            }

                            fullList.add(new MealHistoryItem(uid, name, b, l, d));
                        }

                        // Update UI
                        filter(etSearchMember.getText().toString());
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- MEMBER LOGIC (See Only Self) ---
    private void loadMemberData() {
        String dateKey = getSelectedDateKey();
        String uid = currentUid;

        // Fetch just this single node
        mDatabase.child("mess").child(currentMessCode).child("meals").child(dateKey).child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fullList.clear();

                        double b = 0, l = 0, d = 0;
                        if (snapshot.exists()) {
                            Double bVal = snapshot.child("breakfast").getValue(Double.class);
                            Double lVal = snapshot.child("lunch").getValue(Double.class);
                            Double dVal = snapshot.child("dinner").getValue(Double.class);

                            if (bVal != null) b = bVal;
                            if (lVal != null) l = lVal;
                            if (dVal != null) d = dVal;
                        }

                        // We need the user's own name (fetch from Auth or DB if needed, simplified here)
                        String myName = mAuth.getCurrentUser().getDisplayName();
                        if (myName == null) myName = "Me";

                        fullList.add(new MealHistoryItem(uid, myName, b, l, d));

                        // Show it
                        filter("");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- SEARCH FILTER ---
    private void filter(String text) {
        displayList.clear();
        if (text.isEmpty()) {
            displayList.addAll(fullList);
        } else {
            text = text.toLowerCase();
            for (MealHistoryItem item : fullList) {
                if (item.userName.toLowerCase().contains(text)) {
                    displayList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}