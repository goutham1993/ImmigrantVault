package com.document.immigrantvault.ui.person.tabs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TravelSubTabAdapter extends FragmentStateAdapter {

    private final long personId;

    public TravelSubTabAdapter(@NonNull Fragment fragment, long personId) {
        super(fragment);
        this.personId = personId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return I94TabFragment.newInstance(personId);
        }
        return TravelHistoryTabFragment.newInstance(personId);
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
