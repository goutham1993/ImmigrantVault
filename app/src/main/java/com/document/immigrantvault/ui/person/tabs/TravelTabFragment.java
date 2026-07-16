package com.document.immigrantvault.ui.person.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.immigrantvault.R;
import com.document.immigrantvault.databinding.FragmentTravelTabBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class TravelTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;

    public static TravelTabFragment newInstance(long personId) {
        TravelTabFragment fragment = new TravelTabFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            personId = getArguments().getLong(ARG_PERSON_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentTravelTabBinding binding = FragmentTravelTabBinding.inflate(inflater, container, false);

        TravelSubTabAdapter adapter = new TravelSubTabAdapter(this, personId);
        binding.travelViewPager.setAdapter(adapter);

        String[] tabTitles = {
                getString(R.string.travel_latest_i94),
                getString(R.string.travel_history)
        };
        new TabLayoutMediator(binding.travelSubTabs, binding.travelViewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        return binding.getRoot();
    }
}
