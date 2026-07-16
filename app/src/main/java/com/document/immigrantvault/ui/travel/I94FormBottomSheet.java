package com.document.immigrantvault.ui.travel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.I94Entry;
import com.document.immigrantvault.databinding.BottomSheetI94FormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class I94FormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";

    private BottomSheetI94FormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private Date arrivalDate;
    private Date admitUntil;

    public static I94FormBottomSheet newInstance(long personId) {
        I94FormBottomSheet sheet = new I94FormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetI94FormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        binding.formTitle.setText(R.string.add_i94);
        DatePickerHelper.bind(requireContext(), binding.inputArrival, null, d -> arrivalDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputAdmit, null, d -> admitUntil = d);
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());

        app.getExecutor().execute(() -> {
            I94Entry existing = app.getDatabase().i94Dao().getByPersonSync(personId);
            if (existing != null) {
                requireActivity().runOnUiThread(() -> populate(existing));
            }
        });
    }

    private void populate(I94Entry entry) {
        binding.formTitle.setText(R.string.edit_i94);
        binding.inputI94.setText(entry.i94Number);
        binding.inputDocument.setText(entry.documentNumber);
        binding.inputCitizenship.setText(entry.countryOfCitizenship);
        binding.inputPort.setText(entry.portOfEntry);
        binding.inputClass.setText(entry.classOfAdmission);
        arrivalDate = entry.arrivalDate;
        admitUntil = entry.admitUntilDate;
        if (arrivalDate != null) {
            binding.inputArrival.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(arrivalDate));
        }
        if (admitUntil != null) {
            binding.inputAdmit.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(admitUntil));
        }
    }

    private void save() {
        if (arrivalDate == null) {
            binding.inputArrivalLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputArrivalLayout.setError(null);

        I94Entry entry = new I94Entry();
        entry.personId = personId;
        entry.i94Number = text(binding.inputI94);
        entry.documentNumber = emptyToNull(text(binding.inputDocument));
        entry.countryOfCitizenship = emptyToNull(text(binding.inputCitizenship));
        entry.arrivalDate = arrivalDate;
        entry.admitUntilDate = admitUntil;
        entry.portOfEntry = text(binding.inputPort);
        entry.classOfAdmission = text(binding.inputClass);
        app.getI94Repository().save(entry, this::dismiss);
    }

    private String text(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
