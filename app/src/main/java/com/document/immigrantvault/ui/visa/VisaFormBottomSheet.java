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
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.db.entity.VisaEntry;
import com.document.immigrantvault.data.db.entity.VisaType;
import com.document.immigrantvault.data.repository.ReminderRepository;
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
    private int selectedLeadDays = ReminderRepository.DEFAULT_LEAD_DAYS;

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
        UiUtils.autoCapitalizeInputs(view);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        setupTypeDropdown();
        setupLeadDaysDropdown();
        DatePickerHelper.bind(requireContext(), binding.inputStart, null, d -> startDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputEnd, null, d -> {
            endDate = d;
            updateReminderUi();
        });
        binding.switchRemind.setOnCheckedChangeListener((buttonView, isChecked) -> updateReminderUi());
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        if (requireArguments().containsKey(ARG_ENTRY_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                VisaEntry entry = app.getDatabase().visaDao().getByIdSync(id);
                Reminder reminder = app.getReminderRepository()
                        .getByLinkedSync(LinkedEntityType.VISA, id);
                if (entry != null) {
                    requireActivity().runOnUiThread(() -> populate(entry, reminder));
                }
            });
        } else {
            binding.formTitle.setText(R.string.add_visa);
            updateReminderUi();
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

    private void setupLeadDaysDropdown() {
        String[] labels = new String[ReminderRepository.LEAD_DAY_OPTIONS.length];
        for (int i = 0; i < ReminderRepository.LEAD_DAY_OPTIONS.length; i++) {
            labels[i] = getString(R.string.remind_lead_days, ReminderRepository.LEAD_DAY_OPTIONS[i]);
        }
        binding.inputLeadDays.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        setLeadDaysSelection(ReminderRepository.DEFAULT_LEAD_DAYS);
        binding.inputLeadDays.setOnItemClickListener((parent, view, position, id) ->
                selectedLeadDays = ReminderRepository.LEAD_DAY_OPTIONS[position]);
    }

    private void setLeadDaysSelection(int leadDays) {
        selectedLeadDays = leadDays;
        for (int i = 0; i < ReminderRepository.LEAD_DAY_OPTIONS.length; i++) {
            if (ReminderRepository.LEAD_DAY_OPTIONS[i] == leadDays) {
                binding.inputLeadDays.setText(
                        getString(R.string.remind_lead_days, leadDays), false);
                return;
            }
        }
        selectedLeadDays = ReminderRepository.DEFAULT_LEAD_DAYS;
        binding.inputLeadDays.setText(
                getString(R.string.remind_lead_days, selectedLeadDays), false);
    }

    private void updateReminderUi() {
        boolean hasEnd = endDate != null;
        binding.switchRemind.setEnabled(hasEnd);
        if (!hasEnd) {
            binding.switchRemind.setChecked(false);
        }
        binding.inputLeadDaysLayout.setVisibility(
                hasEnd && binding.switchRemind.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void populate(VisaEntry entry, Reminder reminder) {
        editing = entry;
        binding.inputType.setText(EnumLabels.visaType(entry.type), false);
        binding.inputVisaNumber.setText(entry.visaNumber);
        binding.inputControlNumber.setText(entry.controlNumber);
        binding.inputEmployer.setText(entry.employer);
        binding.inputNotes.setText(entry.notes);
        startDate = entry.startDate;
        endDate = entry.endDate;
        if (startDate != null) {
            binding.inputStart.setText(DateUtils.formatDate(startDate));
        }
        if (endDate != null) {
            binding.inputEnd.setText(DateUtils.formatDate(endDate));
        }
        if (reminder != null && reminder.enabled) {
            binding.switchRemind.setChecked(true);
            setLeadDaysSelection(reminder.leadDays);
        } else {
            binding.switchRemind.setChecked(false);
            setLeadDaysSelection(ReminderRepository.DEFAULT_LEAD_DAYS);
        }
        updateReminderUi();
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
        entry.employer = emptyToNull(text(binding.inputEmployer));
        entry.startDate = startDate;
        entry.endDate = endDate;
        entry.notes = emptyToNull(text(binding.inputNotes));

        Integer leadDays = (endDate != null && binding.switchRemind.isChecked())
                ? selectedLeadDays
                : null;

        if (editing == null) {
            app.getVisaRepository().insert(entry, leadDays, this::dismiss);
        } else {
            app.getVisaRepository().update(entry, leadDays, this::dismiss);
        }
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
