package com.document.immigrantvault.ui.petition;

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
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.PetitionType;
import com.document.immigrantvault.databinding.BottomSheetPetitionFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class PetitionFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_PETITION_ID = "petition_id";

    private BottomSheetPetitionFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private Petition editing;
    private Date filedDate;

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
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        setupDropdowns();
        DatePickerHelper.bind(requireContext(), binding.inputFiled, null, d -> filedDate = d);
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
        }
    }

    private void setupDropdowns() {
        PetitionType[] types = PetitionType.values();
        String[] typeLabels = new String[types.length];
        for (int i = 0; i < types.length; i++) typeLabels[i] = EnumLabels.petitionType(types[i]);
        binding.inputType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, typeLabels));
        binding.inputType.setText(typeLabels[0], false);

        PetitionStatus[] statuses = PetitionStatus.values();
        String[] statusLabels = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) statusLabels[i] = EnumLabels.petitionStatus(statuses[i]);
        binding.inputStatus.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, statusLabels));
        binding.inputStatus.setText(statusLabels[0], false);
    }

    private void populate(Petition p) {
        editing = p;
        binding.inputType.setText(EnumLabels.petitionType(p.type), false);
        binding.inputStatus.setText(EnumLabels.petitionStatus(p.status), false);
        binding.inputReceipt.setText(p.receiptNumber);
        binding.inputInterval.setText(String.valueOf(p.checkIntervalDays));
        filedDate = p.filedDate;
        if (filedDate != null) binding.inputFiled.setText(com.document.immigrantvault.util.DateUtils.formatDate(filedDate));
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
        try {
            p.checkIntervalDays = Integer.parseInt(text(binding.inputInterval));
        } catch (NumberFormatException e) {
            p.checkIntervalDays = 14;
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
}
