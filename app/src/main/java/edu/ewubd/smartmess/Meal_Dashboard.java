package edu.ewubd.smartmess;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class Meal_Dashboard extends AppCompatActivity {

    private EditText etDate;
    private Button btnAddMeal;
    private ListView listMeals;

    private TextView tvBreakfastCount, tvLunchCount, tvDinnerCount, tvTotalCount;

    private FirebaseFirestore db;

    private Calendar selectedCal = Calendar.getInstance();
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // 🔹 Now we use MealEntry + custom adapter
    private ArrayList<Meal_Entry> mealRows = new ArrayList<>();
    private MealEntryAdapter mealAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_dashboard); // dashboard layout

        db = FirebaseFirestore.getInstance();

        initViews();
        setupDateField();
        setupAddMealButton();

        //  Custom adapter with row_meal.xml
        mealAdapter = new MealEntryAdapter(this,mealRows);
        listMeals.setAdapter(mealAdapter);

        // start e ajker  meal load
        String today = sdf.format(selectedCal.getTime());
        etDate.setText(today);
        loadMealsForDate(today);
    }

    private void initViews() {
        etDate          = findViewById(R.id.etDate);
        btnAddMeal      = findViewById(R.id.btnAddMeal);
        listMeals       = findViewById(R.id.listMeals);

        tvBreakfastCount = findViewById(R.id.tvBreakfastCount);
        tvLunchCount     = findViewById(R.id.tvLunchCount);
        tvDinnerCount    = findViewById(R.id.tvDinnerCount);
        tvTotalCount     = findViewById(R.id.tvTotalCount);
    }

    private void setupDateField() {
        etDate.setOnClickListener(v -> {
            int y = selectedCal.get(Calendar.YEAR);
            int m = selectedCal.get(Calendar.MONTH);
            int d = selectedCal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    Meal_Dashboard.this,
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        selectedCal.set(year, month, dayOfMonth);
                        String dateStr = sdf.format(selectedCal.getTime());
                        etDate.setText(dateStr);
                        loadMealsForDate(dateStr);   // new date select korle abar load
                    },
                    y, m, d
            );
            dialog.show();
        });
    }

    private void setupAddMealButton() {
        btnAddMeal.setOnClickListener(v -> {
            Intent i = new Intent(Meal_Dashboard.this, Add_Meal.class);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Add_Meal theke back ashle abar selected date er data reload
        String dateStr = etDate.getText().toString().trim();
        if (!dateStr.isEmpty()) {
            loadMealsForDate(dateStr);
        }
    }

    // Firestore theke oi date er sob meals ene ListView + counts update
    private void loadMealsForDate(String dateStr) {
        db.collection("meals")
                .whereEqualTo("dateString", dateStr)
                .get()
                .addOnSuccessListener(snapshot -> {
                    mealRows.clear();

                    int bCount = 0;
                    int lCount = 0;
                    int dCount = 0;
                    int total  = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String memberName = doc.getString("memberName");
                        Long b = doc.getLong("breakfast");
                        Long l = doc.getLong("lunch");
                        Long d = doc.getLong("dinner");
                        Long t = doc.getLong("totalMeals");

                        int bi = b == null ? 0 : b.intValue();
                        int li = l == null ? 0 : l.intValue();
                        int di = d == null ? 0 : d.intValue();
                        int ti = t == null ? 0 : t.intValue();

                        bCount += bi;
                        lCount += li;
                        dCount += di;
                        total  += ti;

                        // 🔹 ek ek row model hishebe add
                        mealRows.add(new Meal_Entry(memberName, bi, li, di, ti));
                    }

                    mealAdapter.notifyDataSetChanged();

                    tvBreakfastCount.setText(bCount + " meals");
                    tvLunchCount.setText(lCount + " meals");
                    tvDinnerCount.setText(dCount + " meals");
                    tvTotalCount.setText(total + " meals");
                });
    }
}
