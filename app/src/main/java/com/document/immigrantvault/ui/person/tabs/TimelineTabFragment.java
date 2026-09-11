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
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.databinding.FragmentTimelineTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.TimelineAdapter;
import com.document.immigrantvault.ui.common.TimelineCategoryFilter;
import com.document.immigrantvault.ui.common.TimelineCategoryFilter.Category;
import com.google.android.material.chip.ChipGroup;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class TimelineTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;

    private FragmentTimelineTabBinding binding;
    private TimelineAdapter adapter;
    private List<TimelineEvent> allEvents = Collections.emptyList();
    private boolean syncingChips;
    private boolean allWasChecked = true;

    public static TimelineTabFragment newInstance(long personId) {
        TimelineTabFragment f = new TimelineTabFragment();
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
        binding = FragmentTimelineTabBinding.inflate(inflater, container, false);
        adapter = new TimelineAdapter();
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRecycler.setAdapter(adapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyTitle.setText(R.string.empty_timeline);
        empty.emptySubtitle.setText(R.string.empty_timeline_subtitle);

        setupFilterChips();

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getTimelineRepository().getByPerson(personId).observe(getViewLifecycleOwner(), events -> {
            allEvents = events != null ? events : Collections.emptyList();
            applyFilter();
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    private void setupFilterChips() {
        ChipGroup group = binding.timelineFilterChips;
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (syncingChips) {
                return;
            }
            boolean allNow = checkedIds.contains(R.id.chip_filter_all);
            int categoryCount = categoryChipCount(checkedIds);

            syncingChips = true;
            try {
                if (allNow && categoryCount > 0) {
                    if (allWasChecked) {
                        binding.chipFilterAll.setChecked(false);
                    } else {
                        binding.chipFilterVisa.setChecked(false);
                        binding.chipFilterAddress.setChecked(false);
                        binding.chipFilterEmployer.setChecked(false);
                    }
                } else if (!allNow && categoryCount == 0) {
                    binding.chipFilterAll.setChecked(true);
                }
            } finally {
                syncingChips = false;
            }

            allWasChecked = binding.chipFilterAll.isChecked();
            applyFilter();
        });
    }

    private static int categoryChipCount(List<Integer> checkedIds) {
        int count = 0;
        if (checkedIds.contains(R.id.chip_filter_visa)) count++;
        if (checkedIds.contains(R.id.chip_filter_address)) count++;
        if (checkedIds.contains(R.id.chip_filter_employer)) count++;
        return count;
    }

    private Set<Category> selectedCategories() {
        if (binding.chipFilterAll.isChecked()) {
            return Collections.emptySet();
        }
        Set<Category> selected = EnumSet.noneOf(Category.class);
        if (binding.chipFilterVisa.isChecked()) {
            selected.add(Category.VISA);
        }
        if (binding.chipFilterAddress.isChecked()) {
            selected.add(Category.ADDRESS);
        }
        if (binding.chipFilterEmployer.isChecked()) {
            selected.add(Category.EMPLOYER);
        }
        return selected;
    }

    private void applyFilter() {
        if (binding == null || adapter == null) {
            return;
        }
        List<TimelineEvent> filtered = TimelineCategoryFilter.filter(allEvents, selectedCategories());
        adapter.setItems(filtered);

        boolean noSourceEvents = allEvents.isEmpty();
        boolean noMatches = filtered.isEmpty();
        ViewEmptyStateBinding empty = binding.emptyState;
        if (noMatches) {
            if (noSourceEvents) {
                empty.emptyTitle.setText(R.string.empty_timeline);
                empty.emptySubtitle.setText(R.string.empty_timeline_subtitle);
            } else {
                empty.emptyTitle.setText(R.string.empty_timeline_filter);
                empty.emptySubtitle.setText(R.string.empty_timeline_filter_subtitle);
            }
            binding.listRecycler.setVisibility(View.GONE);
            empty.getRoot().setVisibility(View.VISIBLE);
        } else {
            binding.listRecycler.setVisibility(View.VISIBLE);
            empty.getRoot().setVisibility(View.GONE);
        }
    }
}
