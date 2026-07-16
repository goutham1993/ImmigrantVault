package com.document.immigrantvault.ui.person.tabs;

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
import com.document.immigrantvault.databinding.FragmentOverviewTabBinding;
import com.document.immigrantvault.ui.ViewModelFactory;
import com.document.immigrantvault.ui.person.PersonDetailViewModel;
import com.document.immigrantvault.ui.person.PersonFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;

public class OverviewTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;

    public static OverviewTabFragment newInstance(long personId) {
        OverviewTabFragment fragment = new OverviewTabFragment();
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
        FragmentOverviewTabBinding binding = FragmentOverviewTabBinding.inflate(
                inflater, container, false);

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        PersonDetailViewModel viewModel = new ViewModelProvider(requireParentFragment(),
                new ViewModelFactory(app)).get(PersonDetailViewModel.class);

        viewModel.getPerson(personId).observe(getViewLifecycleOwner(), person -> {
            if (person == null) {
                return;
            }
            binding.overviewBirthday.setText(
                    person.dateOfBirth != null
                            ? DateUtils.formatDate(person.dateOfBirth)
                            : getString(R.string.overview_no_birthday));

            String visa = person.currentVisaType != null ? person.currentVisaType
                    : getString(R.string.status_no_visa);
            binding.overviewVisa.setText(visa);
            binding.overviewDates.setText(
                    DateUtils.formatDate(person.visaStartDate) + " – "
                            + DateUtils.formatDate(person.visaEndDate));
            binding.overviewDays.setText(DateUtils.daysUntilLabel(person.visaEndDate));

            binding.overviewEmployer.setText(
                    person.currentEmployer != null && !person.currentEmployer.isEmpty()
                            ? person.currentEmployer
                            : getString(R.string.overview_no_employer));
            binding.overviewRole.setText(
                    person.currentRole != null && !person.currentRole.isEmpty()
                            ? person.currentRole
                            : getString(R.string.overview_no_role));
        });

        binding.actionEditProfile.setOnClickListener(v ->
                PersonFormBottomSheet.newInstance(personId)
                        .show(getParentFragmentManager(), "edit_person"));

        return binding.getRoot();
    }
}
