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
import com.document.immigrantvault.data.db.entity.TaxReturnEntry;
import com.document.immigrantvault.data.db.entity.TaxReturnType;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.tax.TaxReturnFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaxReturnTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<TaxReturnEntry> entries = new ArrayList<>();
    private ListEntryAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public static TaxReturnTabFragment newInstance(long personId) {
        TaxReturnTabFragment f = new TaxReturnTabFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        f.setArguments(args);
        return f;
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
        FragmentListTabBinding binding = FragmentListTabBinding.inflate(inflater, container, false);
        adapter = new ListEntryAdapter();
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRecycler.setAdapter(adapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyTitle.setText(R.string.empty_tax_returns);
        empty.emptySubtitle.setText(R.string.empty_tax_returns_subtitle);

        adapter.setOnItemClickListener(pos -> TaxReturnFormBottomSheet.newInstance(personId, entries.get(pos).id)
                .show(getParentFragmentManager(), "tax_return_form"));
        binding.fabAdd.setOnClickListener(v -> TaxReturnFormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "tax_return_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getTaxReturnRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            entries = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (TaxReturnEntry entry : entries) {
                items.add(new ListEntryAdapter.ListItem(
                        formatTitle(entry),
                        formatSubtitle(entry),
                        formatMeta(entry)));
            }
            adapter.setItems(items);
            boolean isEmpty = entries.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }

    private String formatTitle(TaxReturnEntry entry) {
        if (entry.returnType == TaxReturnType.STATE) {
            if (entry.state != null && !entry.state.isEmpty()) {
                return getString(R.string.tax_return_state_title, entry.state);
            }
            return EnumLabels.taxReturnType(TaxReturnType.STATE);
        }
        return EnumLabels.taxReturnType(TaxReturnType.FEDERAL);
    }

    private String formatSubtitle(TaxReturnEntry entry) {
        String outcome = EnumLabels.taxReturnOutcome(entry.outcome);
        String amount = currencyFormat.format(entry.amount != null ? entry.amount : 0d);
        return getString(R.string.tax_return_card_subtitle, outcome, amount);
    }

    private String formatMeta(TaxReturnEntry entry) {
        String yearMeta = getString(R.string.w2_tax_year_meta, entry.taxYear);
        if (entry.filedDate != null) {
            return getString(R.string.tax_return_card_meta_filed,
                    yearMeta, DateUtils.formatDate(entry.filedDate));
        }
        return yearMeta;
    }
}
