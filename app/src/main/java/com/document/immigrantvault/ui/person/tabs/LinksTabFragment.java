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
import com.document.immigrantvault.data.db.entity.UsefulLink;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.link.LinkFormBottomSheet;
import com.document.immigrantvault.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class LinksTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<UsefulLink> links = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static LinksTabFragment newInstance(long personId) {
        LinksTabFragment f = new LinksTabFragment();
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
        empty.emptyTitle.setText(R.string.empty_links);
        empty.emptySubtitle.setText(R.string.empty_links_subtitle);

        adapter.setOnItemClickListener(pos -> {
            UsefulLink link = links.get(pos);
            if (link.url != null && !link.url.isEmpty()) {
                UiUtils.openUrl(requireContext(), link.url);
            }
        });
        adapter.setOnItemLongClickListener(pos ->
                LinkFormBottomSheet.newInstance(personId, links.get(pos).id)
                        .show(getParentFragmentManager(), "link_form"));
        binding.fabAdd.setOnClickListener(v ->
                LinkFormBottomSheet.newInstance(personId, null)
                        .show(getParentFragmentManager(), "link_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getUsefulLinkRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            links = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (UsefulLink link : links) {
                items.add(new ListEntryAdapter.ListItem(
                        link.title != null ? link.title : "",
                        link.url != null ? link.url : "",
                        link.notes != null ? link.notes : ""));
            }
            adapter.setItems(items);
            boolean isEmpty = links.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }
}
