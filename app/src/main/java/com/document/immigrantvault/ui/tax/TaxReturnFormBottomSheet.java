package com.document.immigrantvault.ui.tax;

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
import com.document.immigrantvault.data.db.entity.TaxReturnEntry;
import com.document.immigrantvault.data.db.entity.TaxReturnOutcome;
import com.document.immigrantvault.data.db.entity.TaxReturnType;
import com.document.immigrantvault.databinding.BottomSheetTaxReturnFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Date;

public class TaxReturnFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";
    private static final Double PARSE_ERROR = Double.NaN;

    private BottomSheetTaxReturnFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private TaxReturnEntry editing;
    private Date filedDate;
    private Date refundReceivedDate;

    public static TaxReturnFormBottomSheet newInstance(long personId, Long entryId) {
        TaxReturnFormBottomSheet sheet = new TaxReturnFormBottomSheet();
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
        binding = BottomSheetTaxReturnFormBinding.inflate(inflater, container, false);
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
        DatePickerHelper.bind(requireContext(), binding.inputRefundReceived, null, d -> refundReceivedDate = d);
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        boolean editingExisting = requireArguments().containsKey(ARG_ENTRY_ID);
        if (editingExisting) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long entryId = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                TaxReturnEntry entry = app.getDatabase().taxReturnDao().getByIdSync(entryId);
                if (entry != null) {
                    requireActivity().runOnUiThread(() -> populate(entry));
                }
            });
        } else {
            binding.formTitle.setText(R.string.add_tax_return);
            int year = Calendar.getInstance().get(Calendar.YEAR) - 1;
            binding.inputTaxYear.setText(String.valueOf(year));
            updateStateVisibility();
        }
    }

    private void setupDropdowns() {
        TaxReturnType[] types = TaxReturnType.values();
        String[] typeLabels = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeLabels[i] = EnumLabels.taxReturnType(types[i]);
        }
        binding.inputReturnType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, typeLabels));
        binding.inputReturnType.setText(typeLabels[0], false);
        binding.inputReturnType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateStateVisibility();
            }
        });

        TaxReturnOutcome[] outcomes = TaxReturnOutcome.values();
        String[] outcomeLabels = new String[outcomes.length];
        for (int i = 0; i < outcomes.length; i++) {
            outcomeLabels[i] = EnumLabels.taxReturnOutcome(outcomes[i]);
        }
        binding.inputOutcome.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, outcomeLabels));
        binding.inputOutcome.setText(outcomeLabels[0], false);
    }

    private void updateStateVisibility() {
        boolean isState = typeFromLabel(dropdownText(binding.inputReturnType)) == TaxReturnType.STATE;
        binding.inputStateLayout.setVisibility(isState ? View.VISIBLE : View.GONE);
        if (!isState) {
            binding.inputStateLayout.setError(null);
            binding.inputState.setText(null);
        }
    }

    private void populate(TaxReturnEntry entry) {
        editing = entry;
        binding.inputTaxYear.setText(String.valueOf(entry.taxYear));
        binding.inputReturnType.setText(EnumLabels.taxReturnType(entry.returnType), false);
        binding.inputOutcome.setText(EnumLabels.taxReturnOutcome(entry.outcome), false);
        binding.inputState.setText(entry.state);
        setAmount(binding.inputAmount, entry.amount);
        setAmount(binding.inputAgi, entry.agi);
        setAmount(binding.inputTotalTax, entry.totalTax);
        filedDate = entry.filedDate;
        refundReceivedDate = entry.refundReceivedDate;
        if (filedDate != null) {
            binding.inputFiled.setText(DateUtils.formatDate(filedDate));
        }
        if (refundReceivedDate != null) {
            binding.inputRefundReceived.setText(DateUtils.formatDate(refundReceivedDate));
        }
        binding.inputNotes.setText(entry.notes);
        updateStateVisibility();
    }

    private void save() {
        clearErrors();

        Integer taxYear = parseYear(text(binding.inputTaxYear));
        if (taxYear == null) {
            binding.inputTaxYearLayout.setError(getString(R.string.error_invalid_year));
            return;
        }

        TaxReturnType returnType = typeFromLabel(dropdownText(binding.inputReturnType));
        TaxReturnOutcome outcome = outcomeFromLabel(dropdownText(binding.inputOutcome));

        String state = emptyToNull(text(binding.inputState));
        if (returnType == TaxReturnType.STATE && (state == null || state.isEmpty())) {
            binding.inputStateLayout.setError(getString(R.string.error_required));
            return;
        }
        if (returnType == TaxReturnType.FEDERAL) {
            state = null;
        } else if (state != null) {
            state = state.toUpperCase(java.util.Locale.US);
        }

        Double amount = parseAmount(binding.inputAmount, binding.inputAmountLayout);
        if (isParseError(amount)) {
            return;
        }
        if (amount == null) {
            binding.inputAmountLayout.setError(getString(R.string.error_required));
            return;
        }
        if (amount < 0) {
            binding.inputAmountLayout.setError(getString(R.string.error_invalid_amount));
            return;
        }

        Double agi = parseAmount(binding.inputAgi, binding.inputAgiLayout);
        if (isParseError(agi)) {
            return;
        }
        Double totalTax = parseAmount(binding.inputTotalTax, binding.inputTotalTaxLayout);
        if (isParseError(totalTax)) {
            return;
        }

        TaxReturnEntry entry = editing != null ? editing : new TaxReturnEntry();
        entry.personId = personId;
        entry.taxYear = taxYear;
        entry.returnType = returnType;
        entry.state = state;
        entry.outcome = outcome;
        entry.amount = amount;
        entry.agi = agi;
        entry.totalTax = totalTax;
        entry.filedDate = filedDate;
        entry.refundReceivedDate = refundReceivedDate;
        entry.notes = emptyToNull(text(binding.inputNotes));

        if (editing == null) {
            app.getTaxReturnRepository().insert(entry, this::dismiss);
        } else {
            app.getTaxReturnRepository().update(entry, this::dismiss);
        }
    }

    private void delete() {
        if (editing == null) {
            return;
        }
        UiUtils.confirmDelete(requireContext(), () ->
                app.getTaxReturnRepository().delete(editing, this::dismiss));
    }

    private void clearErrors() {
        binding.inputTaxYearLayout.setError(null);
        binding.inputReturnTypeLayout.setError(null);
        binding.inputStateLayout.setError(null);
        binding.inputOutcomeLayout.setError(null);
        binding.inputAmountLayout.setError(null);
        binding.inputAgiLayout.setError(null);
        binding.inputTotalTaxLayout.setError(null);
    }

    private TaxReturnType typeFromLabel(String label) {
        for (TaxReturnType type : TaxReturnType.values()) {
            if (EnumLabels.taxReturnType(type).equals(label)) {
                return type;
            }
        }
        return TaxReturnType.FEDERAL;
    }

    private TaxReturnOutcome outcomeFromLabel(String label) {
        for (TaxReturnOutcome outcome : TaxReturnOutcome.values()) {
            if (EnumLabels.taxReturnOutcome(outcome).equals(label)) {
                return outcome;
            }
        }
        return TaxReturnOutcome.REFUND;
    }

    private static boolean isParseError(Double value) {
        return value != null && Double.isNaN(value);
    }

    private Double parseAmount(TextInputEditText editText, TextInputLayout layout) {
        String value = text(editText);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            layout.setError(getString(R.string.error_invalid_amount));
            return PARSE_ERROR;
        }
    }

    private Integer parseYear(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            int year = Integer.parseInt(value);
            if (year < 1900 || year > 2100) {
                return null;
            }
            return year;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setAmount(TextInputEditText editText, Double value) {
        if (value != null) {
            editText.setText(stripTrailingZeros(value));
        }
    }

    private static String stripTrailingZeros(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String dropdownText(AutoCompleteTextView e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
