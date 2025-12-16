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

public class ExpenseAdapter extends ArrayAdapter<ExpenseItem> {

    public ExpenseAdapter(@NonNull Context context, ArrayList<ExpenseItem> expenseList) {
        super(context, 0, expenseList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.activity_row_expense_list, parent, false);
        }

        ExpenseItem currentExpense = getItem(position);

        TextView tvUser = listItemView.findViewById(R.id.tv_row_user);
        TextView tvItem = listItemView.findViewById(R.id.tv_row_item);
        TextView tvDate = listItemView.findViewById(R.id.tv_row_date);
        TextView tvAmount = listItemView.findViewById(R.id.tv_row_amount);

        if (currentExpense != null) {
            tvUser.setText(currentExpense.user);
            tvItem.setText(currentExpense.item);
            tvDate.setText(currentExpense.date);
            tvAmount.setText(String.format("%.0f", currentExpense.amount));
        }

        return listItemView;
    }
}