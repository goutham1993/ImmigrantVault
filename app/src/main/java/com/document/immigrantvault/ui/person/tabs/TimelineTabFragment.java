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
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.TimelineAdapter;

public class TimelineTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;

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
        FragmentListTabBinding binding = FragmentListTabBinding.inflate(inflater, container, false);
        TimelineAdapter adapter = new TimelineAdapter();
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRecycler.setAdapter(adapter);
        binding.fabAdd.setVisibility(View.GONE);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyTitle.setText(R.string.empty_timeline);
        empty.emptySubtitle.setText(R.string.empty_timeline_subtitle);

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getTimelineRepository().getByPerson(personId).observe(getViewLifecycleOwner(), events -> {
            adapter.setItems(events);
            boolean isEmpty = events == null || events.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });
        return binding.getRoot();
    }
}
