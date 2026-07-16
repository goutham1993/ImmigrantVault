package com.document.immigrantvault.ui.education;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.EducationEntry;
import com.document.immigrantvault.databinding.BottomSheetEducationFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class EducationFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";

    private BottomSheetEducationFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private EducationEntry editing;
    private Date startDate;
    private Date endDate;

    public static EducationFormBottomSheet newInstance(long personId, Long entryId) {
        EducationFormBottomSheet sheet = new EducationFormBottomSheet();
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
        binding = BottomSheetEducationFormBinding.inflate(inflater, container, false);
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

        if (requireArguments().containsKey(ARG_ENTRY_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                EducationEntry entry = app.getDatabase().educationDao().getByIdSync(id);
                if (entry != null) requireActivity().runOnUiThread(() -> populate(entry));
            });
        } else {
            binding.formTitle.setText(R.string.add_education);
        }
    }

    private void populate(EducationEntry entry) {
        editing = entry;
        binding.inputInstitution.setText(entry.institutionName);
        binding.inputDegree.setText(entry.degree);
        binding.inputField.setText(entry.fieldOfStudy);
        binding.inputCity.setText(entry.city);
        binding.inputCountry.setText(entry.country);
        binding.inputGpa.setText(entry.gpa);
        startDate = entry.startDate;
        endDate = entry.endDate;
        if (startDate != null) {
            binding.inputStart.setText(DateUtils.formatDate(startDate));
        }
        if (endDate != null) {
            binding.inputEnd.setText(DateUtils.formatDate(endDate));
        }
    }

    private void save() {
        if (text(binding.inputInstitution).isEmpty()) {
            binding.inputInstitutionLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputInstitutionLayout.setError(null);

        EducationEntry entry = editing != null ? editing : new EducationEntry();
        entry.personId = personId;
        entry.institutionName = text(binding.inputInstitution);
        entry.degree = emptyToNull(text(binding.inputDegree));
        entry.fieldOfStudy = emptyToNull(text(binding.inputField));
        entry.city = emptyToNull(text(binding.inputCity));
        entry.country = emptyToNull(text(binding.inputCountry));
        entry.gpa = emptyToNull(text(binding.inputGpa));
        entry.startDate = startDate;
        entry.endDate = endDate;

        if (editing == null) app.getEducationRepository().insert(entry, this::dismiss);
        else app.getEducationRepository().update(entry, this::dismiss);
    }

    private void delete() {
        if (editing == null) return;
        UiUtils.confirmDelete(requireContext(), () ->
                app.getEducationRepository().delete(editing, this::dismiss));
    }

    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
