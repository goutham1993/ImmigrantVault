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
import com.document.immigrantvault.data.db.entity.VisaEntry;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.visa.VisaFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;

import java.util.ArrayList;
import java.util.List;

public class VisaTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<VisaEntry> entries = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static VisaTabFragment newInstance(long personId) {
        VisaTabFragment f = new VisaTabFragment();
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
        FragmentListTabBinding binding = FragmentListTabBinding.inflate(inflater, container, false);
        adapter = new ListEntryAdapter();
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRecycler.setAdapter(adapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyTitle.setText(R.string.empty_visas);
        empty.emptySubtitle.setText(R.string.empty_visas_subtitle);

        adapter.setOnItemClickListener(pos -> VisaFormBottomSheet.newInstance(personId, entries.get(pos).id)
                .show(getParentFragmentManager(), "visa_form"));
        binding.fabAdd.setOnClickListener(v -> VisaFormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "visa_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getVisaRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            entries = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (VisaEntry e : entries) {
                boolean ongoing = e.endDate == null;
                String meta = DateUtils.formatEmploymentDateRange(e.startDate, e.endDate, ongoing);
                items.add(new ListEntryAdapter.ListItem(
                        EnumLabels.visaType(e.type), formatSubtitle(e), meta));
            }
            adapter.setItems(items);
            boolean isEmpty = entries.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }

    private String formatSubtitle(VisaEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.visaNumber != null && !entry.visaNumber.isEmpty()) {
            sb.append(entry.visaNumber);
        }
        if (entry.controlNumber != null && !entry.controlNumber.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(entry.controlNumber);
        }
        return sb.toString();
    }
}
