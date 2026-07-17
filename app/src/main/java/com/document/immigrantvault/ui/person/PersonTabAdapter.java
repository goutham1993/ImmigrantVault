package com.document.immigrantvault.ui.person;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.document.immigrantvault.ui.person.tabs.AddressTabFragment;
import com.document.immigrantvault.ui.person.tabs.DocumentsTabFragment;
import com.document.immigrantvault.ui.person.tabs.EducationTabFragment;
import com.document.immigrantvault.ui.person.tabs.EmployerTabFragment;
import com.document.immigrantvault.ui.person.tabs.LinksTabFragment;
import com.document.immigrantvault.ui.person.tabs.OverviewTabFragment;
import com.document.immigrantvault.ui.person.tabs.PetitionTabFragment;
import com.document.immigrantvault.ui.person.tabs.TimelineTabFragment;
import com.document.immigrantvault.ui.person.tabs.TravelTabFragment;
import com.document.immigrantvault.ui.person.tabs.VisaTabFragment;

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
                return EducationTabFragment.newInstance(personId);
            case 2:
                return DocumentsTabFragment.newInstance(personId);
            case 3:
                return VisaTabFragment.newInstance(personId);
            case 4:
                return TravelTabFragment.newInstance(personId);
            case 5:
                return AddressTabFragment.newInstance(personId);
            case 6:
                return EmployerTabFragment.newInstance(personId);
            case 7:
                return PetitionTabFragment.newInstance(personId);
            case 8:
                return TimelineTabFragment.newInstance(personId);
            case 9:
            default:
                return LinksTabFragment.newInstance(personId);
        }
    }

    @Override
    public int getItemCount() {
        return 10;
    }
}
