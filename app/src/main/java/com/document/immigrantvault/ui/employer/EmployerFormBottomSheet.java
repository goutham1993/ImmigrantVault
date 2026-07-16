package com.document.immigrantvault.ui.employer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.EmployerEntry;
import com.document.immigrantvault.databinding.BottomSheetEmployerFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class EmployerFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";

    private BottomSheetEmployerFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private EmployerEntry editing;
    private Date startDate;
    private Date endDate;

    public static EmployerFormBottomSheet newInstance(long personId, Long entryId) {
        EmployerFormBottomSheet sheet = new EmployerFormBottomSheet();
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
        binding = BottomSheetEmployerFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        DatePickerHelper.bind(requireContext(), binding.inputStart, null, d -> startDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputEnd, null, d -> endDate = d);
        binding.switchCurrent.setOnCheckedChangeListener((button, isChecked) -> updateEndDateState(isChecked));
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        if (requireArguments().containsKey(ARG_ENTRY_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                EmployerEntry entry = app.getDatabase().employerDao().getByIdSync(id);
                if (entry != null) requireActivity().runOnUiThread(() -> populate(entry));
            });
        } else {
            binding.formTitle.setText(R.string.add_employer);
            updateEndDateState(false);
        }
    }

    private void populate(EmployerEntry entry) {
        editing = entry;
        binding.inputEmployer.setText(entry.employerName);
        binding.inputJob.setText(entry.jobTitle);
        binding.inputCity.setText(entry.city);
        binding.inputNotes.setText(entry.notes);
        startDate = entry.startDate;
        endDate = entry.endDate;
        if (startDate != null) {
            binding.inputStart.setText(com.document.immigrantvault.util.DateUtils.formatDate(startDate));
        }
        binding.switchCurrent.setChecked(entry.isCurrent);
        if (entry.isCurrent) {
            updateEndDateState(true);
        } else if (endDate != null) {
            binding.inputEnd.setText(com.document.immigrantvault.util.DateUtils.formatDate(endDate));
            updateEndDateState(false);
        } else {
            updateEndDateState(false);
        }
    }

    private void updateEndDateState(boolean isCurrent) {
        binding.inputEnd.setEnabled(!isCurrent);
        binding.inputEndLayout.setEnabled(!isCurrent);
        binding.inputEnd.setClickable(!isCurrent);
        if (isCurrent) {
            endDate = null;
            binding.inputEnd.setText("");
        }
    }

    private void save() {
        if (text(binding.inputEmployer).isEmpty()) {
            binding.inputEmployerLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputEmployerLayout.setError(null);

        EmployerEntry entry = editing != null ? editing : new EmployerEntry();
        entry.personId = personId;
        entry.employerName = text(binding.inputEmployer);
        entry.jobTitle = text(binding.inputJob);
        entry.city = emptyToNull(text(binding.inputCity));
        entry.startDate = startDate;
        entry.isCurrent = binding.switchCurrent.isChecked();
        entry.endDate = entry.isCurrent ? null : endDate;
        entry.notes = text(binding.inputNotes);

        if (editing == null) app.getEmployerRepository().insert(entry, this::dismiss);
        else app.getEmployerRepository().update(entry, this::dismiss);
    }

    private void delete() {
        if (editing == null) return;
        UiUtils.confirmDelete(requireContext(), () ->
                app.getEmployerRepository().delete(editing, this::dismiss));
    }

    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
