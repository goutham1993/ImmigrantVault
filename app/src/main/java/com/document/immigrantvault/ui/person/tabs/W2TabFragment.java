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
import com.document.immigrantvault.data.db.entity.W2Entry;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.w2.W2FormBottomSheet;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class W2TabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<W2Entry> entries = new ArrayList<>();
    private ListEntryAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public static W2TabFragment newInstance(long personId) {
        W2TabFragment f = new W2TabFragment();
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
        empty.emptyTitle.setText(R.string.empty_w2s);
        empty.emptySubtitle.setText(R.string.empty_w2s_subtitle);

        adapter.setOnItemClickListener(pos -> W2FormBottomSheet.newInstance(personId, entries.get(pos).id)
                .show(getParentFragmentManager(), "w2_form"));
        binding.fabAdd.setOnClickListener(v -> W2FormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "w2_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getW2Repository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            entries = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (W2Entry e : entries) {
                String subtitle = e.wages != null
                        ? getString(R.string.w2_wages_summary, currencyFormat.format(e.wages))
                        : "";
                String meta = getString(R.string.w2_tax_year_meta, e.taxYear);
                items.add(new ListEntryAdapter.ListItem(e.employerName, subtitle, meta));
            }
            adapter.setItems(items);
            boolean isEmpty = entries.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }
}
