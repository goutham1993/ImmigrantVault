package com.document.immigrantvault.util;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.document.immigrantvault.R;

public final class FolderColorHelper {

    public static final class FolderColors {
        @ColorInt
        public final int bodyColor;
        @ColorInt
        public final int tabColor;
        @ColorInt
        public final int onColor;

        public FolderColors(@ColorInt int bodyColor, @ColorInt int tabColor, @ColorInt int onColor) {
            this.bodyColor = bodyColor;
            this.tabColor = tabColor;
            this.onColor = onColor;
        }
    }

    private FolderColorHelper() {
    }

    @NonNull
    public static FolderColors forPersonId(@NonNull Context context, long personId) {
        TypedArray bodies = context.getResources().obtainTypedArray(R.array.folder_body_colors);
        TypedArray tabs = context.getResources().obtainTypedArray(R.array.folder_tab_colors);
        TypedArray ons = context.getResources().obtainTypedArray(R.array.folder_on_colors);
        try {
            int count = Math.min(bodies.length(), Math.min(tabs.length(), ons.length()));
            if (count <= 0) {
                return new FolderColors(
                        context.getColor(R.color.folder_blue_body),
                        context.getColor(R.color.folder_blue_tab),
                        context.getColor(R.color.folder_blue_on));
            }
            int index = (int) (Math.floorMod(personId, count));
            return new FolderColors(
                    bodies.getColor(index, context.getColor(R.color.folder_blue_body)),
                    tabs.getColor(index, context.getColor(R.color.folder_blue_tab)),
                    ons.getColor(index, context.getColor(R.color.folder_blue_on)));
        } finally {
            bodies.recycle();
            tabs.recycle();
            ons.recycle();
        }
    }
}
