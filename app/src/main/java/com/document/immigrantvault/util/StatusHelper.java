package com.document.immigrantvault.util;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import com.document.immigrantvault.R;
import com.google.android.material.chip.Chip;

import java.util.Date;

public final class StatusHelper {

    private StatusHelper() {
    }

    /** Status of a single visa entry, where a null end date means open-ended. */
    public static final class VisaStatus {
        @StringRes
        public final int labelRes;
        @ColorRes
        public final int textColorRes;
        @ColorRes
        public final int backgroundColorRes;

        private VisaStatus(@StringRes int labelRes, @ColorRes int textColorRes,
                           @ColorRes int backgroundColorRes) {
            this.labelRes = labelRes;
            this.textColorRes = textColorRes;
            this.backgroundColorRes = backgroundColorRes;
        }
    }

    public static VisaStatus visaStatus(Date visaEndDate) {
        if (visaEndDate == null) {
            return new VisaStatus(R.string.status_active, R.color.status_active,
                    R.color.chip_active_bg);
        }
        int days = DateUtils.daysUntil(visaEndDate);
        if (days < 0) {
            return new VisaStatus(R.string.status_expired, R.color.status_expired,
                    R.color.chip_expired_bg);
        }
        if (days <= 30) {
            return new VisaStatus(R.string.status_expiring_soon, R.color.status_warning,
                    R.color.chip_warning_bg);
        }
        return new VisaStatus(R.string.status_active, R.color.status_active,
                R.color.chip_active_bg);
    }

    public static void applyVisaStatusChip(Chip chip, Date visaEndDate, Context context) {
        if (visaEndDate == null) {
            chip.setText(R.string.status_no_visa);
            chip.setChipBackgroundColorResource(R.color.chip_pending_bg);
            chip.setTextColor(context.getColor(R.color.status_pending));
            return;
        }
        VisaStatus status = visaStatus(visaEndDate);
        chip.setText(status.labelRes);
        chip.setChipBackgroundColorResource(status.backgroundColorRes);
        chip.setTextColor(context.getColor(status.textColorRes));
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
