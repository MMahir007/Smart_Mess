package edu.ewubd.smartmess;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class MealEntryAdapter extends ArrayAdapter<Meal_Entry> {

    public MealEntryAdapter(@NonNull Context context, @NonNull List<Meal_Entry> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_meal_entry, parent, false);
        }

        Meal_Entry entry = getItem(position);

        TextView tvName      = convertView.findViewById(R.id.tvname);
        TextView tvBreak     = convertView.findViewById(R.id.tvBreakfast);
        TextView tvLunch     = convertView.findViewById(R.id.tvLunch);
        TextView tvDinner    = convertView.findViewById(R.id.tvDinner);
        TextView tvTotal     = convertView.findViewById(R.id.tvtotal);

        // Name
        tvName.setText(entry.name);

        // Breakfast Yes/No
        tvBreak.setText(entry.breakfast == 1 ? "Yes" : "No");

        // Lunch
        tvLunch.setText(entry.lunch == 1 ? "Yes" : "No");

        // Dinner
        tvDinner.setText(entry.dinner == 1 ? "Yes" : "No");

        // Total
        tvTotal.setText(String.valueOf(entry.total));

        return convertView;
    }
}
