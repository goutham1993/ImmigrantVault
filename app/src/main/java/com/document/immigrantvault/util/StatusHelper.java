package com.document.immigrantvault.util;

import android.content.Context;
import android.graphics.Color;

import com.document.immigrantvault.R;
import com.google.android.material.chip.Chip;

import java.util.Date;

public final class StatusHelper {

    private StatusHelper() {
    }

    public static void applyVisaStatusChip(Chip chip, Date visaEndDate, Context context) {
        if (visaEndDate == null) {
            chip.setText(R.string.status_no_visa);
            chip.setChipBackgroundColorResource(R.color.chip_pending_bg);
            return;
        }
        int days = DateUtils.daysUntil(visaEndDate);
        if (days < 0) {
            chip.setText(R.string.status_expired);
            chip.setChipBackgroundColorResource(R.color.chip_expired_bg);
            chip.setTextColor(context.getColor(R.color.status_expired));
        } else if (days <= 30) {
            chip.setText(R.string.status_expiring_soon);
            chip.setChipBackgroundColorResource(R.color.chip_warning_bg);
            chip.setTextColor(context.getColor(R.color.status_warning));
        } else {
            chip.setText(R.string.status_active);
            chip.setChipBackgroundColorResource(R.color.chip_active_bg);
            chip.setTextColor(context.getColor(R.color.status_active));
        }
    }

    public static int deadlineColorRes(Date date) {
        if (date == null) {
            return R.color.status_pending;
        }
        int days = DateUtils.daysUntil(date);
        if (days < 0) {
            return R.color.status_expired;
        }
        if (days <= 7) {
            return R.color.status_expired;
        }
        if (days <= 30) {
            return R.color.status_warning;
        }
        return R.color.status_active;
    }
}
