package com.document.immigrantvault.ui.address;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.AddressEntry;
import com.document.immigrantvault.databinding.BottomSheetAddressFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class AddressFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";

    private BottomSheetAddressFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private AddressEntry editing;
    private Date startDate;
    private Date endDate;

    public static AddressFormBottomSheet newInstance(long personId, Long entryId) {
        AddressFormBottomSheet sheet = new AddressFormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        if (entryId != null) args.putLong(ARG_ENTRY_ID, entryId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddressFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        DatePickerHelper.bind(requireContext(), binding.inputStart, null, d -> startDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputEnd, null, d -> endDate = d);
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        binding.toggleDwellingType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateApartmentFieldsVisibility(checkedId == R.id.btn_dwelling_apartment);
            }
        });
        binding.toggleDwellingType.check(R.id.btn_dwelling_home);

        if (requireArguments().containsKey(ARG_ENTRY_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                AddressEntry entry = app.getDatabase().addressDao().getByIdSync(id);
                if (entry != null) requireActivity().runOnUiThread(() -> populate(entry));
            });
        } else {
            binding.formTitle.setText(R.string.add_address);
        }
    }

    private void populate(AddressEntry entry) {
        editing = entry;
        binding.inputLine1.setText(entry.line1);
        binding.inputCity.setText(entry.city);
        binding.inputState.setText(entry.state);
        binding.inputZip.setText(entry.zip);
        binding.switchCurrent.setChecked(entry.isCurrent);
        startDate = entry.startDate;
        endDate = entry.endDate;
        if (startDate != null) {
            binding.inputStart.setText(com.document.immigrantvault.util.DateUtils.formatDate(startDate));
        }
        if (endDate != null) {
            binding.inputEnd.setText(com.document.immigrantvault.util.DateUtils.formatDate(endDate));
        }

        if (entry.isApartment()) {
            binding.toggleDwellingType.check(R.id.btn_dwelling_apartment);
            binding.inputApartmentName.setText(entry.apartmentName);
            binding.inputApartmentNumber.setText(entry.apartmentNumber);
        } else {
            binding.toggleDwellingType.check(R.id.btn_dwelling_home);
        }
        updateApartmentFieldsVisibility(entry.isApartment());
    }

    private void updateApartmentFieldsVisibility(boolean showApartmentFields) {
        binding.apartmentFields.setVisibility(showApartmentFields ? View.VISIBLE : View.GONE);
    }

    private void save() {
        if (text(binding.inputLine1).isEmpty()) {
            binding.inputLine1Layout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputLine1Layout.setError(null);

        AddressEntry entry = editing != null ? editing : new AddressEntry();
        entry.personId = personId;
        entry.line1 = text(binding.inputLine1);
        entry.city = text(binding.inputCity);
        entry.state = text(binding.inputState);
        entry.zip = text(binding.inputZip);
        entry.country = "USA";
        entry.startDate = startDate;
        entry.endDate = endDate;
        entry.isCurrent = binding.switchCurrent.isChecked();

        boolean isApartment = binding.toggleDwellingType.getCheckedButtonId() == R.id.btn_dwelling_apartment;
        entry.dwellingType = isApartment ? AddressEntry.DWELLING_APARTMENT : AddressEntry.DWELLING_HOME;
        if (isApartment) {
            entry.apartmentName = emptyToNull(text(binding.inputApartmentName));
            entry.apartmentNumber = emptyToNull(text(binding.inputApartmentNumber));
        } else {
            entry.apartmentName = null;
            entry.apartmentNumber = null;
        }

        if (editing == null) app.getAddressRepository().insert(entry, this::dismiss);
        else app.getAddressRepository().update(entry, this::dismiss);
    }

    private void delete() {
        if (editing == null) return;
        UiUtils.confirmDelete(requireContext(), () ->
                app.getAddressRepository().delete(editing, this::dismiss));
    }

    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
