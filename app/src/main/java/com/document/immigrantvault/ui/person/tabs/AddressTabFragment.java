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
import com.document.immigrantvault.data.db.entity.AddressEntry;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.address.AddressFormBottomSheet;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class AddressTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<AddressEntry> entries = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static AddressTabFragment newInstance(long personId) {
        AddressTabFragment f = new AddressTabFragment();
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
        empty.emptyIcon.setImageResource(R.drawable.ic_home);
        empty.emptyTitle.setText(R.string.empty_addresses);
        empty.emptySubtitle.setText(R.string.empty_addresses_subtitle);

        adapter.setOnItemClickListener(pos -> AddressFormBottomSheet.newInstance(personId, entries.get(pos).id)
                .show(getParentFragmentManager(), "address_form"));
        binding.fabAdd.setOnClickListener(v -> AddressFormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "address_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getAddressRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            entries = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (AddressEntry e : entries) {
                String title = e.isCurrent ? "Current" : "Previous";
                String sub = formatAddressSummary(e);
                items.add(new ListEntryAdapter.ListItem(title, sub,
                        DateUtils.formatDate(e.startDate) + " – " + DateUtils.formatDate(e.endDate)));
            }
            adapter.setItems(items);
            boolean isEmpty = entries.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });
        return binding.getRoot();
    }

    private String formatAddressSummary(AddressEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.isApartment()) {
            sb.append(getString(R.string.dwelling_apartment));
        } else {
            sb.append(getString(R.string.dwelling_home));
        }
        if (entry.line1 != null && !entry.line1.isEmpty()) {
            sb.append(" · ").append(entry.line1);
        }
        if (entry.city != null && !entry.city.isEmpty()) {
            sb.append(", ").append(entry.city);
        }
        if (entry.isApartment()) {
            if (entry.apartmentNumber != null && !entry.apartmentNumber.isEmpty()) {
                sb.append(" · Apt ").append(entry.apartmentNumber);
            }
            if (entry.apartmentName != null && !entry.apartmentName.isEmpty()) {
                sb.append(" · ").append(entry.apartmentName);
            }
        }
        return sb.toString();
    }
}
