package com.document.immigrantvault.ui.visa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.VisaEntry;
import com.document.immigrantvault.data.db.entity.VisaType;
import com.document.immigrantvault.databinding.BottomSheetVisaFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class VisaFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";

    private BottomSheetVisaFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private VisaEntry editing;
    private Date startDate;
    private Date endDate;

    public static VisaFormBottomSheet newInstance(long personId, Long entryId) {
        VisaFormBottomSheet sheet = new VisaFormBottomSheet();
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
        binding = BottomSheetVisaFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        setupTypeDropdown();
        DatePickerHelper.bind(requireContext(), binding.inputStart, null, d -> startDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputEnd, null, d -> endDate = d);
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        if (requireArguments().containsKey(ARG_ENTRY_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                VisaEntry entry = app.getDatabase().visaDao().getByIdSync(id);
                if (entry != null) requireActivity().runOnUiThread(() -> populate(entry));
            });
        } else {
            binding.formTitle.setText(R.string.add_visa);
        }
    }

    private void setupTypeDropdown() {
        VisaType[] types = VisaType.values();
        String[] typeLabels = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeLabels[i] = EnumLabels.visaType(types[i]);
        }
        binding.inputType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, typeLabels));
        binding.inputType.setText(typeLabels[0], false);
    }

    private void populate(VisaEntry entry) {
        editing = entry;
        binding.inputType.setText(EnumLabels.visaType(entry.type), false);
        binding.inputVisaNumber.setText(entry.visaNumber);
        binding.inputControlNumber.setText(entry.controlNumber);
        binding.inputNotes.setText(entry.notes);
        startDate = entry.startDate;
        endDate = entry.endDate;
        if (startDate != null) {
            binding.inputStart.setText(DateUtils.formatDate(startDate));
        }
        if (endDate != null) {
            binding.inputEnd.setText(DateUtils.formatDate(endDate));
        }
    }

    private VisaType typeFromLabel(String label) {
        for (VisaType t : VisaType.values()) {
            if (EnumLabels.visaType(t).equals(label)) return t;
        }
        return VisaType.OTHER;
    }

    private void save() {
        VisaEntry entry = editing != null ? editing : new VisaEntry();
        entry.personId = personId;
        entry.type = typeFromLabel(text(binding.inputType));
        entry.visaNumber = emptyToNull(text(binding.inputVisaNumber));
        entry.controlNumber = emptyToNull(text(binding.inputControlNumber));
        entry.startDate = startDate;
        entry.endDate = endDate;
        entry.notes = emptyToNull(text(binding.inputNotes));

        if (editing == null) app.getVisaRepository().insert(entry, this::dismiss);
        else app.getVisaRepository().update(entry, this::dismiss);
    }

    private void delete() {
        if (editing == null) return;
        UiUtils.confirmDelete(requireContext(), () ->
                app.getVisaRepository().delete(editing, this::dismiss));
    }

    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String text(AutoCompleteTextView e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
