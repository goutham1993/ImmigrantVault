package com.document.immigrantvault.ui.person.tabs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TaxesSubTabAdapter extends FragmentStateAdapter {

    private final long personId;

    public TaxesSubTabAdapter(@NonNull Fragment fragment, long personId) {
        super(fragment);
        this.personId = personId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return W2TabFragment.newInstance(personId);
        }
        return TaxReturnTabFragment.newInstance(personId);
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
