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
import com.document.immigrantvault.data.db.entity.EducationEntry;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.education.EducationFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class EducationTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<EducationEntry> entries = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static EducationTabFragment newInstance(long personId) {
        EducationTabFragment f = new EducationTabFragment();
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
        empty.emptyTitle.setText(R.string.empty_education);
        empty.emptySubtitle.setText(R.string.empty_education_subtitle);

        adapter.setOnItemClickListener(pos -> EducationFormBottomSheet.newInstance(personId, entries.get(pos).id)
                .show(getParentFragmentManager(), "education_form"));
        binding.fabAdd.setOnClickListener(v -> EducationFormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "education_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getEducationRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            entries = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (EducationEntry e : entries) {
                boolean ongoing = e.endDate == null;
                String meta = DateUtils.formatEmploymentDateRange(e.startDate, e.endDate, ongoing);
                String duration = DateUtils.formatYearsMonths(e.startDate, e.endDate, ongoing);
                if (!duration.isEmpty()) {
                    meta = meta + " · " + duration;
                }
                items.add(new ListEntryAdapter.ListItem(e.institutionName, formatSubtitle(e), meta));
            }
            adapter.setItems(items);
            boolean isEmpty = entries.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }

    private String formatSubtitle(EducationEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.degree != null && !entry.degree.isEmpty()) {
            sb.append(entry.degree);
        }
        if (entry.fieldOfStudy != null && !entry.fieldOfStudy.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(entry.fieldOfStudy);
        }
        if (entry.city != null && !entry.city.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(entry.city);
        }
        if (entry.country != null && !entry.country.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(entry.city != null && !entry.city.isEmpty() ? ", " : " · ");
            }
            sb.append(entry.country);
        }
        if (entry.gpa != null && !entry.gpa.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("GPA ").append(entry.gpa);
        }
        return sb.toString();
    }
}
