package com.document.immigrantvault.ui.person;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.document.immigrantvault.ui.person.tabs.AddressTabFragment;
import com.document.immigrantvault.ui.person.tabs.DocumentsTabFragment;
import com.document.immigrantvault.ui.person.tabs.EmployerTabFragment;
import com.document.immigrantvault.ui.person.tabs.OverviewTabFragment;
import com.document.immigrantvault.ui.person.tabs.PetitionTabFragment;
import com.document.immigrantvault.ui.person.tabs.TimelineTabFragment;
import com.document.immigrantvault.ui.person.tabs.TravelTabFragment;

public class PersonTabAdapter extends FragmentStateAdapter {

    private final long personId;

    public PersonTabAdapter(@NonNull Fragment fragment, long personId) {
        super(fragment);
        this.personId = personId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return OverviewTabFragment.newInstance(personId);
            case 1:
                return DocumentsTabFragment.newInstance(personId);
            case 2:
                return TravelTabFragment.newInstance(personId);
            case 3:
                return AddressTabFragment.newInstance(personId);
            case 4:
                return EmployerTabFragment.newInstance(personId);
            case 5:
                return PetitionTabFragment.newInstance(personId);
            case 6:
            default:
                return TimelineTabFragment.newInstance(personId);
        }
    }

    @Override
    public int getItemCount() {
        return 7;
    }
}
