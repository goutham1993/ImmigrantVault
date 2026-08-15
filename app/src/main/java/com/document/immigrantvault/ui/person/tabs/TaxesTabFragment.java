package com.document.immigrantvault.ui.person.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.immigrantvault.R;
import com.document.immigrantvault.databinding.FragmentTaxesTabBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class TaxesTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;

    public static TaxesTabFragment newInstance(long personId) {
        TaxesTabFragment fragment = new TaxesTabFragment();
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
        FragmentTaxesTabBinding binding = FragmentTaxesTabBinding.inflate(inflater, container, false);

        TaxesSubTabAdapter adapter = new TaxesSubTabAdapter(this, personId);
        binding.taxesViewPager.setAdapter(adapter);

        String[] tabTitles = {
                getString(R.string.taxes_sub_w2s),
                getString(R.string.taxes_sub_returns)
        };
        new TabLayoutMediator(binding.taxesSubTabs, binding.taxesViewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        return binding.getRoot();
    }
}
