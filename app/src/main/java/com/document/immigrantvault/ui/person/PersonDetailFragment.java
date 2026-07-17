package com.document.immigrantvault.ui.person;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.databinding.FragmentPersonDetailBinding;
import com.document.immigrantvault.ui.ViewModelFactory;
import com.document.immigrantvault.util.EnumLabels;
import com.google.android.material.tabs.TabLayoutMediator;

public class PersonDetailFragment extends Fragment {

    private FragmentPersonDetailBinding binding;
    private long personId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            personId = getArguments().getLong("personId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPersonDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        PersonDetailViewModel viewModel = new ViewModelProvider(this, new ViewModelFactory(app))
                .get(PersonDetailViewModel.class);

        PersonTabAdapter adapter = new PersonTabAdapter(this, personId);
        binding.viewPager.setAdapter(adapter);

        String[] tabTitles = {
                getString(R.string.tab_overview),
                getString(R.string.tab_education),
                getString(R.string.tab_documents),
                getString(R.string.tab_visas),
                getString(R.string.tab_travel),
                getString(R.string.tab_addresses),
                getString(R.string.tab_employers),
                getString(R.string.tab_petitions),
                getString(R.string.tab_timeline),
                getString(R.string.tab_links)
        };

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        viewModel.getPerson(personId).observe(getViewLifecycleOwner(), person -> {
            if (person == null) {
                return;
            }
            ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(person.name);
                actionBar.setSubtitle(EnumLabels.relationship(person.relationship));
            }
        });
    }

    @Override
    public void onDestroyView() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
        super.onDestroyView();
        binding = null;
    }
}
