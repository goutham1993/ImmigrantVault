package com.document.immigrantvault.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;

public final class DatePickerHelper {

    private DatePickerHelper() {
    }

    public static void bind(Context context, EditText editText, Date initial, OnDateSelectedListener listener) {
        editText.setOnClickListener(v -> show(context, editText, initial, listener));
    }

    public static void show(Context context, EditText editText, Date initial, OnDateSelectedListener listener) {
        Calendar cal = Calendar.getInstance();
        if (initial != null) {
            cal.setTime(initial);
        }
        new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            Date date = selected.getTime();
            editText.setText(DateUtils.formatDate(date));
            if (listener != null) {
                listener.onDateSelected(date);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    public interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }
}
