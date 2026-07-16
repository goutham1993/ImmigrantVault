package com.document.immigrantvault.ui.person.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.I94Entry;
import com.document.immigrantvault.databinding.FragmentI94TabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.travel.I94FormBottomSheet;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.LinkConstants;
import com.document.immigrantvault.util.UiUtils;

public class I94TabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;

    public static I94TabFragment newInstance(long personId) {
        I94TabFragment fragment = new I94TabFragment();
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
        FragmentI94TabBinding binding = FragmentI94TabBinding.inflate(inflater, container, false);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyIcon.setImageResource(R.drawable.ic_travel);
        empty.emptyTitle.setText(R.string.empty_i94);
        empty.emptySubtitle.setText(R.string.empty_i94_subtitle);

        binding.btnExternalLink.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.I94_URL));
        binding.fabAdd.setOnClickListener(v ->
                I94FormBottomSheet.newInstance(personId)
                        .show(getParentFragmentManager(), "i94_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getI94Repository().getByPerson(personId).observe(getViewLifecycleOwner(), entry -> {
            if (entry == null) {
                binding.i94Card.setVisibility(View.GONE);
                empty.getRoot().setVisibility(View.VISIBLE);
            } else {
                bindI94(binding, entry);
                binding.i94Card.setVisibility(View.VISIBLE);
                binding.i94Card.setOnClickListener(v ->
                        I94FormBottomSheet.newInstance(personId)
                                .show(getParentFragmentManager(), "i94_form"));
                empty.getRoot().setVisibility(View.GONE);
            }
        });

        return binding.getRoot();
    }

    private void bindI94(FragmentI94TabBinding binding, I94Entry entry) {
        String i94Number = entry.i94Number != null && !entry.i94Number.isEmpty()
                ? entry.i94Number
                : getString(R.string.travel_no_i94_number);
        binding.i94Number.setText(getString(R.string.travel_i94_number, i94Number));
        binding.i94Document.setText(getString(R.string.travel_document_number,
                valueOrDefault(entry.documentNumber, R.string.travel_no_document_number)));
        binding.i94Citizenship.setText(getString(R.string.travel_country_of_citizenship,
                valueOrDefault(entry.countryOfCitizenship, R.string.travel_no_country_of_citizenship)));
        binding.i94Arrival.setText(getString(R.string.travel_arrived,
                DateUtils.formatDate(entry.arrivalDate)));
        binding.i94AdmitUntil.setText(getString(R.string.travel_admit_until,
                DateUtils.formatDate(entry.admitUntilDate)));
        binding.i94Port.setText(entry.portOfEntry != null && !entry.portOfEntry.isEmpty()
                ? entry.portOfEntry
                : getString(R.string.travel_no_port));
        String classOfAdmission = entry.classOfAdmission != null && !entry.classOfAdmission.isEmpty()
                ? entry.classOfAdmission
                : getString(R.string.travel_no_class);
        binding.i94Class.setText(getString(R.string.travel_class, classOfAdmission));
    }

    private String valueOrDefault(String value, int defaultResId) {
        return value != null && !value.isEmpty() ? value : getString(defaultResId);
    }
}
