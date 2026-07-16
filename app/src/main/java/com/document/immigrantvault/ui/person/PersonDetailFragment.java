package com.document.immigrantvault.ui.person;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.databinding.FragmentPersonDetailBinding;
import com.document.immigrantvault.ui.ViewModelFactory;
import com.document.immigrantvault.util.DateUtils;
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

        binding.personToolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        PersonTabAdapter adapter = new PersonTabAdapter(this, personId);
        binding.viewPager.setAdapter(adapter);

        String[] tabTitles = {
                getString(R.string.tab_overview),
                getString(R.string.tab_documents),
                getString(R.string.tab_travel),
                getString(R.string.tab_addresses),
                getString(R.string.tab_employers),
                getString(R.string.tab_petitions),
                getString(R.string.tab_timeline)
        };

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        viewModel.getPerson(personId).observe(getViewLifecycleOwner(), person -> {
            if (person == null) {
                return;
            }
            binding.personToolbar.setTitle(person.name);
            binding.personToolbar.setSubtitle(EnumLabels.relationship(person.relationship));

            String visa = person.currentVisaType != null ? person.currentVisaType
                    : getString(R.string.status_no_visa);
            binding.summaryVisa.setText(visa);
            binding.summaryDays.setText(DateUtils.daysUntilLabel(person.visaEndDate));
            if (person.aNumber != null && !person.aNumber.isEmpty()) {
                binding.summaryANumber.setText("A# " + person.aNumber);
                binding.summaryANumber.setVisibility(View.VISIBLE);
            } else {
                binding.summaryANumber.setVisibility(View.GONE);
            }
            if (person.dateOfBirth != null) {
                binding.summaryBirthday.setText(
                        getString(R.string.label_date_of_birth) + ": "
                                + DateUtils.formatDate(person.dateOfBirth));
                binding.summaryBirthday.setVisibility(View.VISIBLE);
            } else {
                binding.summaryBirthday.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
