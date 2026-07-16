package com.document.immigrantvault.ui.travel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.TravelEntry;
import com.document.immigrantvault.databinding.BottomSheetTravelFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class TravelFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";

    private BottomSheetTravelFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private TravelEntry editing;
    private Date arrivalDate;
    private Date departureDate;

    public static TravelFormBottomSheet newInstance(long personId, Long entryId) {
        TravelFormBottomSheet sheet = new TravelFormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        if (entryId != null) {
            args.putLong(ARG_ENTRY_ID, entryId);
        }
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetTravelFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        DatePickerHelper.bind(requireContext(), binding.inputArrival, null, d -> arrivalDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputDeparture, null, d -> departureDate = d);
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        if (requireArguments().containsKey(ARG_ENTRY_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                TravelEntry entry = app.getDatabase().travelDao().getByIdSync(id);
                if (entry != null) {
                    requireActivity().runOnUiThread(() -> populate(entry));
                }
            });
        } else {
            binding.formTitle.setText(R.string.add_travel);
        }
    }

    private void populate(TravelEntry entry) {
        editing = entry;
        binding.inputArrivalCity.setText(entry.arrivalCity);
        binding.inputDepartureCity.setText(entry.departureCity);
        binding.inputPort.setText(entry.portOfEntry);
        binding.inputAirline.setText(entry.airline);
        binding.inputNotes.setText(entry.notes);
        arrivalDate = entry.arrivalDate;
        departureDate = entry.departureDate;
        if (arrivalDate != null) {
            binding.inputArrival.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(arrivalDate));
        }
        if (departureDate != null) {
            binding.inputDeparture.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(departureDate));
        }
    }

    private void save() {
        if (arrivalDate == null) {
            binding.inputArrivalLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputArrivalLayout.setError(null);

        TravelEntry entry = editing != null ? editing : new TravelEntry();
        entry.personId = personId;
        entry.arrivalCity = text(binding.inputArrivalCity);
        entry.departureCity = text(binding.inputDepartureCity);
        entry.arrivalDate = arrivalDate;
        entry.departureDate = departureDate;
        entry.portOfEntry = text(binding.inputPort);
        entry.airline = emptyToNull(text(binding.inputAirline));
        entry.notes = emptyToNull(text(binding.inputNotes));

        if (editing == null) {
            app.getTravelRepository().insert(entry, this::dismiss);
        } else {
            app.getTravelRepository().update(entry, this::dismiss);
        }
    }

    private void delete() {
        if (editing == null) {
            return;
        }
        UiUtils.confirmDelete(requireContext(), () ->
                app.getTravelRepository().delete(editing, this::dismiss));
    }

    private String text(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
