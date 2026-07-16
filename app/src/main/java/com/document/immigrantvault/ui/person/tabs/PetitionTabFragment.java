package com.document.immigrantvault.ui.person.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.databinding.FragmentPetitionTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.petition.PetitionFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.LinkConstants;
import com.document.immigrantvault.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class PetitionTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<Petition> petitions = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static PetitionTabFragment newInstance(long personId) {
        PetitionTabFragment f = new PetitionTabFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) personId = getArguments().getLong(ARG_PERSON_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentPetitionTabBinding binding = FragmentPetitionTabBinding.inflate(inflater, container, false);
        adapter = new ListEntryAdapter();
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRecycler.setAdapter(adapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyTitle.setText(R.string.empty_petitions);
        empty.emptySubtitle.setText(R.string.empty_petitions_subtitle);

        binding.btnCheckStatus.setOnClickListener(v -> {
            if (!petitions.isEmpty()) {
                Petition p = petitions.get(0);
                UiUtils.copyAndOpen(requireContext(), p.receiptNumber,
                        LinkConstants.USCIS_CASE_STATUS_URL, getString(R.string.receipt_copied));
            } else {
                UiUtils.openUrl(requireContext(), LinkConstants.USCIS_CASE_STATUS_URL);
            }
        });

        adapter.setOnItemClickListener(pos -> {
            Petition p = petitions.get(pos);
            PetitionFormBottomSheet.newInstance(personId, p.id)
                    .show(getParentFragmentManager(), "petition_form");
        });
        binding.fabAdd.setOnClickListener(v -> PetitionFormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "petition_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getPetitionRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            petitions = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (Petition p : petitions) {
                String meta = EnumLabels.petitionStatus(p.status);
                if (p.lastCheckedDate != null) {
                    meta += " · Checked " + DateUtils.formatDate(p.lastCheckedDate);
                }
                items.add(new ListEntryAdapter.ListItem(
                        EnumLabels.petitionType(p.type), p.receiptNumber, meta));
            }
            adapter.setItems(items);
            boolean isEmpty = petitions.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }
}
