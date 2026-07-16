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
import com.document.immigrantvault.data.db.entity.TravelEntry;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.travel.TravelFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class TravelHistoryTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<TravelEntry> entries = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static TravelHistoryTabFragment newInstance(long personId) {
        TravelHistoryTabFragment fragment = new TravelHistoryTabFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        fragment.setArguments(args);
        return fragment;
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
        empty.emptyIcon.setImageResource(R.drawable.ic_travel);
        empty.emptyTitle.setText(R.string.empty_travel);
        empty.emptySubtitle.setText(R.string.empty_travel_subtitle);

        adapter.setOnItemClickListener(pos -> TravelFormBottomSheet.newInstance(personId, entries.get(pos).id)
                .show(getParentFragmentManager(), "travel_form"));
        binding.fabAdd.setOnClickListener(v -> TravelFormBottomSheet.newInstance(personId, null)
                .show(getParentFragmentManager(), "travel_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getTravelRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            entries = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (TravelEntry entry : entries) {
                items.add(new ListEntryAdapter.ListItem(
                        formatRoute(entry),
                        formatTravelDates(entry),
                        buildMeta(entry)));
            }
            adapter.setItems(items);
            boolean isEmpty = entries.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });

        return binding.getRoot();
    }

    private String formatRoute(TravelEntry entry) {
        String departureCity = cityOrPlaceholder(entry.departureCity, R.string.travel_no_departure_city);
        String arrivalCity = cityOrPlaceholder(entry.arrivalCity, R.string.travel_no_arrival_city);
        return getString(R.string.travel_route, departureCity, arrivalCity);
    }

    private String cityOrPlaceholder(String city, int placeholderRes) {
        return city != null && !city.isEmpty() ? city : getString(placeholderRes);
    }

    private String formatTravelDates(TravelEntry entry) {
        if (entry.departureDate != null && entry.arrivalDate != null) {
            return DateUtils.formatTravelDateRange(entry.departureDate, entry.arrivalDate);
        }
        if (entry.departureDate != null) {
            return getString(R.string.travel_departed, DateUtils.formatDate(entry.departureDate));
        }
        return getString(R.string.travel_arrived, DateUtils.formatDate(entry.arrivalDate));
    }

    private String buildMeta(TravelEntry entry) {
        StringBuilder meta = new StringBuilder();
        appendMetaPart(meta, DateUtils.formatTravelDays(entry.departureDate, entry.arrivalDate));
        appendMetaPart(meta, entry.portOfEntry);
        appendMetaPart(meta, entry.airline);
        if (entry.notes != null && !entry.notes.isEmpty()) {
            appendMetaPart(meta, getString(R.string.travel_reason, entry.notes));
        }
        return meta.toString();
    }

    private void appendMetaPart(StringBuilder meta, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (meta.length() > 0) {
            meta.append(" · ");
        }
        meta.append(value);
    }
}
