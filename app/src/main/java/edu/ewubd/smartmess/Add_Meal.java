package edu.ewubd.smartmess;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Add_Meal extends AppCompatActivity {

    private Spinner memberSpinner;
    private EditText etDate;
    private CheckBox checkboxbreak, checkboxlunch, checkboxdinner;
    private Button btncan, btnsave;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ArrayList<Member> memberList = new ArrayList<>();
    private ArrayAdapter<Member> memberAdapter;
    private Calendar selectedCal = Calendar.getInstance();

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);   // XML file name ta boshan

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupMemberSpinner();
        setupDateField();
        setupButtons();
    }

    private void initViews() {
        memberSpinner   = findViewById(R.id.memberSpinner);
        etDate          = findViewById(R.id.etDate);
        checkboxbreak   = findViewById(R.id.checkboxbreak);
        checkboxlunch   = findViewById(R.id.checkboxlunch);
        checkboxdinner  = findViewById(R.id.checkboxdinner);
        btncan          = findViewById(R.id.btncan);
        btnsave         = findViewById(R.id.btnsave);
    }

    // 🔹 Firestore theke member list load kore Spinner e boshabo
    private void setupMemberSpinner() {

        memberAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                memberList
        );
        memberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        memberSpinner.setAdapter(memberAdapter);

        // dhoren user der info "users" collection e ache
        // field: name, role="member"
        CollectionReference usersRef = db.collection("users");

        usersRef.whereEqualTo("role", "member")   // jodi role differentiate kore thaken
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            memberList.clear();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String id   = doc.getId();                // uid/doc id
                                String name = doc.getString("name");      // member name field
                                memberList.add(new Member(id, name));
                            }
                            memberAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(Add_Meal.this,
                                    "Failed to load members", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // 🔹 Date field: default = today, click korle DatePicker
    private void setupDateField() {
        // set today
        etDate.setText(sdf.format(selectedCal.getTime()));

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int year  = selectedCal.get(Calendar.YEAR);
                int month = selectedCal.get(Calendar.MONTH);
                int day   = selectedCal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(
                        Add_Meal.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int y, int m, int d) {
                                selectedCal.set(y, m, d);
                                etDate.setText(sdf.format(selectedCal.getTime()));
                            }
                        },
                        year, month, day
                );
                dialog.show();
            }
        });
    }

    private void setupButtons() {

        btncan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();   // dialog/activity close
            }
        });

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMealEntry();
            }
        });
    }

    // 🔹 Save button e click -> validation + Firestore e store
    private void saveMealEntry() {

        if (memberList.isEmpty()) {
            Toast.makeText(this, "No members found", Toast.LENGTH_SHORT).show();
            return;
        }

        Member selectedMember = (Member) memberSpinner.getSelectedItem();
        String dateStr = etDate.getText().toString().trim();

        boolean b = checkboxbreak.isChecked();
        boolean l = checkboxlunch.isChecked();
        boolean d = checkboxdinner.isChecked();

        if (TextUtils.isEmpty(dateStr)) {
            etDate.setError("Select date");
            return;
        }

        if (!b && !l && !d) {
            Toast.makeText(this,
                    "Select at least one meal", Toast.LENGTH_SHORT).show();
            return;
        }

        int breakfast = b ? 1 : 0;
        int lunch     = l ? 1 : 0;
        int dinner    = d ? 1 : 0;
        int total     = breakfast + lunch + dinner;

        Map<String, Object> mealMap = new HashMap<>();
        mealMap.put("memberId",   selectedMember.getId());
        mealMap.put("memberName", selectedMember.getName());
        mealMap.put("dateString", dateStr);                        // e.g. 2025-12-08
        mealMap.put("date",       new Timestamp(selectedCal.getTime()));
        mealMap.put("breakfast",  breakfast);
        mealMap.put("lunch",      lunch);
        mealMap.put("dinner",     dinner);
        mealMap.put("totalMeals", total);
        mealMap.put("createdAt",  Timestamp.now());

        db.collection("meals")
                .add(mealMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(Add_Meal.this,
                            "Meal entry saved", Toast.LENGTH_SHORT).show();
                    finish();   // close, list screen Firestore listener diye auto update nibe
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Add_Meal.this,
                            "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
