package com.document.immigrantvault.ui.petition;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.PetitionType;
import com.document.immigrantvault.data.db.entity.PreferenceCategory;
import com.document.immigrantvault.databinding.BottomSheetPetitionFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class PetitionFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_PETITION_ID = "petition_id";
    private static final String CATEGORY_NONE = "—";

    private BottomSheetPetitionFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private Petition editing;
    private Date filedDate;
    private Date priorityDate;
    private Date interviewDate;
    private Date oathDate;

    public static PetitionFormBottomSheet newInstance(long personId, Long petitionId) {
        PetitionFormBottomSheet sheet = new PetitionFormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        if (petitionId != null) args.putLong(ARG_PETITION_ID, petitionId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPetitionFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UiUtils.autoCapitalizeInputs(view);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        setupDropdowns();
        DatePickerHelper.bind(requireContext(), binding.inputFiled, null, d -> filedDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputPriority, null, d -> priorityDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputInterview, null, d -> interviewDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputOath, null, d -> oathDate = d);
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        if (requireArguments().containsKey(ARG_PETITION_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_PETITION_ID);
            app.getExecutor().execute(() -> {
                Petition p = app.getDatabase().petitionDao().getByIdSync(id);
                if (p != null) requireActivity().runOnUiThread(() -> populate(p));
            });
        } else {
            binding.formTitle.setText(R.string.add_petition);
            binding.inputInterval.setText("14");
            updateSectionVisibility();
        }
    }

    private void setupDropdowns() {
        PetitionType[] types = PetitionType.values();
        String[] typeLabels = new String[types.length];
        for (int i = 0; i < types.length; i++) typeLabels[i] = EnumLabels.petitionType(types[i]);
        binding.inputType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, typeLabels));
        binding.inputType.setText(typeLabels[0], false);
        binding.inputType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSectionVisibility();
            }
        });

        PetitionStatus[] statuses = PetitionStatus.values();
        String[] statusLabels = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) statusLabels[i] = EnumLabels.petitionStatus(statuses[i]);
        binding.inputStatus.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, statusLabels));
        binding.inputStatus.setText(statusLabels[0], false);

        PreferenceCategory[] categories = PreferenceCategory.values();
        String[] categoryLabels = new String[categories.length + 1];
        categoryLabels[0] = CATEGORY_NONE;
        for (int i = 0; i < categories.length; i++) {
            categoryLabels[i + 1] = EnumLabels.preferenceCategory(categories[i]);
        }
        binding.inputCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categoryLabels));
        binding.inputCategory.setText(CATEGORY_NONE, false);
    }

    private void updateSectionVisibility() {
        PetitionType type = typeFromLabel(text(binding.inputType));
        boolean showGreenCard = type == PetitionType.I140 || type == PetitionType.I485;
        boolean showMilestones = type == PetitionType.N400 || type == PetitionType.I485;
        binding.sectionGreenCard.setVisibility(showGreenCard ? View.VISIBLE : View.GONE);
        binding.sectionMilestones.setVisibility(showMilestones ? View.VISIBLE : View.GONE);
    }

    private void populate(Petition p) {
        editing = p;
        binding.inputType.setText(EnumLabels.petitionType(p.type), false);
        binding.inputStatus.setText(EnumLabels.petitionStatus(p.status), false);
        binding.inputReceipt.setText(p.receiptNumber);
        binding.inputInterval.setText(String.valueOf(p.checkIntervalDays));
        binding.inputNotes.setText(p.notes);
        binding.inputChargeability.setText(p.countryOfChargeability);

        filedDate = p.filedDate;
        if (filedDate != null) binding.inputFiled.setText(DateUtils.formatDate(filedDate));

        priorityDate = p.priorityDate;
        if (priorityDate != null) binding.inputPriority.setText(DateUtils.formatDate(priorityDate));

        if (p.preferenceCategory != null) {
            binding.inputCategory.setText(EnumLabels.preferenceCategory(p.preferenceCategory), false);
        } else {
            binding.inputCategory.setText(CATEGORY_NONE, false);
        }

        interviewDate = p.interviewDate;
        if (interviewDate != null) binding.inputInterview.setText(DateUtils.formatDate(interviewDate));

        oathDate = p.oathDate;
        if (oathDate != null) binding.inputOath.setText(DateUtils.formatDate(oathDate));

        updateSectionVisibility();
    }

    private PetitionType typeFromLabel(String label) {
        for (PetitionType t : PetitionType.values()) {
            if (EnumLabels.petitionType(t).equals(label)) return t;
        }
        return PetitionType.OTHER;
    }

    private PetitionStatus statusFromLabel(String label) {
        for (PetitionStatus s : PetitionStatus.values()) {
            if (EnumLabels.petitionStatus(s).equals(label)) return s;
        }
        return PetitionStatus.OTHER;
    }

    private PreferenceCategory categoryFromLabel(String label) {
        if (label == null || label.isEmpty() || CATEGORY_NONE.equals(label)) {
            return null;
        }
        for (PreferenceCategory c : PreferenceCategory.values()) {
            if (EnumLabels.preferenceCategory(c).equals(label)) return c;
        }
        return PreferenceCategory.OTHER;
    }

    private void save() {
        if (text(binding.inputReceipt).isEmpty()) {
            binding.inputReceiptLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputReceiptLayout.setError(null);

        Petition p = editing != null ? editing : new Petition();
        p.personId = personId;
        p.type = typeFromLabel(text(binding.inputType));
        p.status = statusFromLabel(text(binding.inputStatus));
        p.receiptNumber = text(binding.inputReceipt);
        p.filedDate = filedDate;
        p.notes = emptyToNull(text(binding.inputNotes));
        try {
            p.checkIntervalDays = Integer.parseInt(text(binding.inputInterval));
        } catch (NumberFormatException e) {
            p.checkIntervalDays = 14;
        }

        boolean greenCardType = p.type == PetitionType.I140 || p.type == PetitionType.I485;
        boolean milestoneType = p.type == PetitionType.N400 || p.type == PetitionType.I485;
        if (greenCardType) {
            p.priorityDate = priorityDate;
            p.preferenceCategory = categoryFromLabel(text(binding.inputCategory));
            p.countryOfChargeability = emptyToNull(text(binding.inputChargeability));
        } else {
            p.priorityDate = null;
            p.preferenceCategory = null;
            p.countryOfChargeability = null;
        }
        if (milestoneType) {
            p.interviewDate = interviewDate;
            p.oathDate = oathDate;
        } else {
            p.interviewDate = null;
            p.oathDate = null;
        }

        if (editing == null) app.getPetitionRepository().insert(p, this::dismiss);
        else app.getPetitionRepository().update(p, this::dismiss);
    }

    private void delete() {
        if (editing == null) return;
        UiUtils.confirmDelete(requireContext(), () ->
                app.getPetitionRepository().delete(editing, this::dismiss));
    }

    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String text(AutoCompleteTextView e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
