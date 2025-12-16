package edu.ewubd.smartmessMN;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

public class MealHistoryAdapter extends ArrayAdapter<MealHistoryItem> {

    public MealHistoryAdapter(@NonNull Context context, ArrayList<MealHistoryItem> list) {
        super(context, 0, list);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Reuse view if possible for performance
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_row_meal, parent, false);
        }

        // Get the data item for this position
        MealHistoryItem item = getItem(position);

        // Bind the views from row_meal_history.xml
        TextView tvName = convertView.findViewById(R.id.tv_row_name);
        TextView tvB = convertView.findViewById(R.id.tv_row_b);
        TextView tvL = convertView.findViewById(R.id.tv_row_l);
        TextView tvD = convertView.findViewById(R.id.tv_row_d);
        TextView tvTotal = convertView.findViewById(R.id.tv_row_total);

        // Populate the data
        if (item != null) {
            tvName.setText(item.userName);

            // Use formatValue helper to hide decimals if it's a whole number (e.g. "1.0" -> "1")
            tvB.setText(formatValue(item.breakfast));
            tvL.setText(formatValue(item.lunch));
            tvD.setText(formatValue(item.dinner));
            tvTotal.setText(formatValue(item.total));
        }

        return convertView;
    }

    // Helper: Formats double to string. Removes ".0" if it's a whole number.
    private String formatValue(double val) {
        if(val == (long) val)
            return String.format("%d", (long)val);
        else
            return String.format("%s", val);
    }
}