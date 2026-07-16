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
import com.document.immigrantvault.data.db.entity.EmployerEntry;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.employer.EmployerFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class EmployerTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<EmployerEntry> entries = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static EmployerTabFragment newInstance(long personId) {
        EmployerTabFragment f = new EmployerTabFragment();
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
        empty.emptyTitle.setText(R.string.empty_employers);
        empty.emptySubtitle.setText(R.string.empty_employers_subtitle);

        adapter.setOnItemClickListener(pos -> EmployerFormBottomSheet.newInstance(personId, entries.get(pos).id)
                .show(getParentFragmentManager(), "employer_form"));
        binding.fabAdd.setOnClickListener(v -> EmployerFormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "employer_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getEmployerRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            entries = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (EmployerEntry e : entries) {
                String meta = DateUtils.formatEmploymentDateRange(e.startDate, e.endDate, e.isCurrent);
                String duration = DateUtils.formatYearsMonths(e.startDate, e.endDate, e.isCurrent);
                if (!duration.isEmpty()) {
                    meta = meta + " · " + duration;
                }
                items.add(new ListEntryAdapter.ListItem(e.employerName, formatSubtitle(e), meta));
            }
            adapter.setItems(items);
            boolean isEmpty = entries.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }

    private String formatSubtitle(EmployerEntry entry) {
        boolean hasJob = entry.jobTitle != null && !entry.jobTitle.isEmpty();
        boolean hasCity = entry.city != null && !entry.city.isEmpty();
        if (hasJob && hasCity) {
            return entry.jobTitle + " · " + entry.city;
        }
        if (hasJob) {
            return entry.jobTitle;
        }
        if (hasCity) {
            return entry.city;
        }
        return "";
    }
}
