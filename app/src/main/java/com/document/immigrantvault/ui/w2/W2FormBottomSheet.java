package com.document.immigrantvault.ui.w2;

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
import com.document.immigrantvault.data.db.entity.EmployerEntry;
import com.document.immigrantvault.data.db.entity.W2Entry;
import com.document.immigrantvault.databinding.BottomSheetW2FormBinding;
import com.document.immigrantvault.extraction.FormScanController;
import com.document.immigrantvault.extraction.OcrText;
import com.document.immigrantvault.extraction.W2Extraction;
import com.document.immigrantvault.extraction.W2FieldParser;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class W2FormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";

    private BottomSheetW2FormBinding binding;
    private ImmigrantVaultApplication app;
    private FormScanController scanController;
    private long personId;
    private W2Entry editing;
    private final List<String> employerNames = new ArrayList<>();

    public static W2FormBottomSheet newInstance(long personId, Long entryId) {
        W2FormBottomSheet sheet = new W2FormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        if (entryId != null) args.putLong(ARG_ENTRY_ID, entryId);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        scanController = new FormScanController(this, app);
        scanController.register();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetW2FormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UiUtils.autoCapitalizeInputs(view);
        personId = requireArguments().getLong(ARG_PERSON_ID);

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());
        binding.btnScan.setOnClickListener(v -> startScan());
        binding.btnUpload.setOnClickListener(v -> startUpload());

        boolean editingExisting = requireArguments().containsKey(ARG_ENTRY_ID);
        if (editingExisting) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
        } else {
            binding.formTitle.setText(R.string.add_w2);
            int year = Calendar.getInstance().get(Calendar.YEAR) - 1;
            binding.inputTaxYear.setText(String.valueOf(year));
        }

        long entryId = editingExisting ? requireArguments().getLong(ARG_ENTRY_ID) : -1L;
        app.getExecutor().execute(() -> {
            List<EmployerEntry> employers = app.getDatabase().employerDao().getByPersonSync(personId);
            List<String> names = uniqueEmployerNames(employers);
            W2Entry entry = entryId >= 0 ? app.getDatabase().w2Dao().getByIdSync(entryId) : null;
            requireActivity().runOnUiThread(() -> {
                setupEmployerDropdown(names, entry != null ? entry.employerName : null);
                if (entry != null) {
                    populate(entry);
                }
            });
        });
    }

    private void startScan() {
        scanController.startScan(
                binding.scanProgress,
                binding.getRoot(),
                this::handleOcrResult,
                binding.btnScan,
                binding.btnUpload);
    }

    private void startUpload() {
        scanController.startUpload(
                binding.scanProgress,
                binding.getRoot(),
                this::handleOcrResult,
                binding.btnScan,
                binding.btnUpload);
    }

    private void handleOcrResult(@NonNull OcrText ocr) {
        W2Extraction extraction = W2FieldParser.parse(ocr);
        if (!extraction.hasAnyField()) {
            scanController.showFailed();
            return;
        }
        applyExtraction(extraction);
        scanController.showFilled();
    }

    private void applyExtraction(@NonNull W2Extraction extraction) {
        if (extraction.taxYear != null) {
            binding.inputTaxYear.setText(String.valueOf(extraction.taxYear));
            binding.inputTaxYearLayout.setError(null);
        }
        if (extraction.employerName != null) {
            applyEmployerName(extraction.employerName);
        }
        if (extraction.ein != null) {
            binding.inputEin.setText(extraction.ein);
        }
        setAmountIfPresent(binding.inputWages, extraction.wages);
        setAmountIfPresent(binding.inputFederalTax, extraction.federalIncomeTax);
        setAmountIfPresent(binding.inputSsWages, extraction.socialSecurityWages);
        setAmountIfPresent(binding.inputSsTax, extraction.socialSecurityTax);
        setAmountIfPresent(binding.inputMedicareWages, extraction.medicareWages);
        setAmountIfPresent(binding.inputMedicareTax, extraction.medicareTax);
        if (extraction.box12aCode != null) {
            binding.inputBox12aCode.setText(extraction.box12aCode);
        }
        setAmountIfPresent(binding.inputBox12aAmount, extraction.box12aAmount);
        if (extraction.box12bCode != null) {
            binding.inputBox12bCode.setText(extraction.box12bCode);
        }
        setAmountIfPresent(binding.inputBox12bAmount, extraction.box12bAmount);
        if (extraction.box12cCode != null) {
            binding.inputBox12cCode.setText(extraction.box12cCode);
        }
        setAmountIfPresent(binding.inputBox12cAmount, extraction.box12cAmount);
        if (extraction.box12dCode != null) {
            binding.inputBox12dCode.setText(extraction.box12dCode);
        }
        setAmountIfPresent(binding.inputBox12dAmount, extraction.box12dAmount);
        if (extraction.box14 != null) {
            binding.inputBox14.setText(extraction.box14);
        }
        if (extraction.state != null) {
            binding.inputState.setText(extraction.state);
        }
        setAmountIfPresent(binding.inputStateWages, extraction.stateWages);
        setAmountIfPresent(binding.inputStateTax, extraction.stateIncomeTax);
    }

    private void applyEmployerName(@NonNull String scannedName) {
        String matched = matchEmployer(scannedName);
        String toUse = matched != null ? matched : scannedName.trim();
        if (!employerNames.contains(toUse)) {
            employerNames.add(0, toUse);
            binding.inputEmployer.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, employerNames));
        }
        binding.inputEmployer.setText(toUse, false);
        binding.inputEmployerLayout.setError(null);
    }

    @Nullable
    private String matchEmployer(@NonNull String scannedName) {
        String needle = scannedName.trim().toLowerCase(Locale.US);
        if (needle.isEmpty()) {
            return null;
        }
        for (String name : employerNames) {
            if (name.equalsIgnoreCase(needle)) {
                return name;
            }
        }
        for (String name : employerNames) {
            String lower = name.toLowerCase(Locale.US);
            if (lower.contains(needle) || needle.contains(lower)) {
                return name;
            }
        }
        return null;
    }

    private void setAmountIfPresent(TextInputEditText editText, @Nullable Double value) {
        if (value != null) {
            setAmount(editText, value);
        }
    }

    private void setupEmployerDropdown(List<String> names, String retainedName) {
        employerNames.clear();
        employerNames.addAll(names);
        if (retainedName != null && !retainedName.isEmpty() && !employerNames.contains(retainedName)) {
            employerNames.add(0, retainedName);
        }
        binding.inputEmployer.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, employerNames));
        if (employerNames.isEmpty()) {
            binding.inputEmployerLayout.setError(getString(R.string.error_w2_no_employers));
        }
    }

    private static List<String> uniqueEmployerNames(List<EmployerEntry> employers) {
        Set<String> unique = new LinkedHashSet<>();
        if (employers != null) {
            for (EmployerEntry employer : employers) {
                if (employer.employerName != null && !employer.employerName.trim().isEmpty()) {
                    unique.add(employer.employerName.trim());
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private void populate(W2Entry entry) {
        editing = entry;
        binding.inputTaxYear.setText(String.valueOf(entry.taxYear));
        if (entry.employerName != null) {
            binding.inputEmployer.setText(entry.employerName, false);
        }
        binding.inputEin.setText(entry.ein);
        setAmount(binding.inputWages, entry.wages);
        setAmount(binding.inputFederalTax, entry.federalIncomeTax);
        setAmount(binding.inputSsWages, entry.socialSecurityWages);
        setAmount(binding.inputSsTax, entry.socialSecurityTax);
        setAmount(binding.inputMedicareWages, entry.medicareWages);
        setAmount(binding.inputMedicareTax, entry.medicareTax);
        binding.inputBox12aCode.setText(entry.box12aCode);
        setAmount(binding.inputBox12aAmount, entry.box12aAmount);
        binding.inputBox12bCode.setText(entry.box12bCode);
        setAmount(binding.inputBox12bAmount, entry.box12bAmount);
        binding.inputBox12cCode.setText(entry.box12cCode);
        setAmount(binding.inputBox12cAmount, entry.box12cAmount);
        binding.inputBox12dCode.setText(entry.box12dCode);
        setAmount(binding.inputBox12dAmount, entry.box12dAmount);
        binding.inputBox14.setText(entry.box14);
        binding.inputState.setText(entry.state);
        setAmount(binding.inputStateWages, entry.stateWages);
        setAmount(binding.inputStateTax, entry.stateIncomeTax);
        binding.inputNotes.setText(entry.notes);
    }

    private void save() {
        clearErrors();

        String yearText = text(binding.inputTaxYear);
        Integer taxYear = parseYear(yearText);
        if (taxYear == null) {
            binding.inputTaxYearLayout.setError(getString(R.string.error_invalid_year));
            return;
        }

        String employerName = dropdownText(binding.inputEmployer);
        if (employerNames.isEmpty()) {
            binding.inputEmployerLayout.setError(getString(R.string.error_w2_no_employers));
            return;
        }
        if (employerName.isEmpty() || !employerNames.contains(employerName)) {
            binding.inputEmployerLayout.setError(getString(R.string.error_w2_select_employer));
            return;
        }

        Double wages = parseAmount(binding.inputWages, binding.inputWagesLayout);
        if (isParseError(wages)) return;
        Double federalTax = parseAmount(binding.inputFederalTax, binding.inputFederalTaxLayout);
        if (isParseError(federalTax)) return;
        Double ssWages = parseAmount(binding.inputSsWages, binding.inputSsWagesLayout);
        if (isParseError(ssWages)) return;
        Double ssTax = parseAmount(binding.inputSsTax, binding.inputSsTaxLayout);
        if (isParseError(ssTax)) return;
        Double medicareWages = parseAmount(binding.inputMedicareWages, binding.inputMedicareWagesLayout);
        if (isParseError(medicareWages)) return;
        Double medicareTax = parseAmount(binding.inputMedicareTax, binding.inputMedicareTaxLayout);
        if (isParseError(medicareTax)) return;
        Double box12a = parseAmount(binding.inputBox12aAmount, binding.inputBox12aAmountLayout);
        if (isParseError(box12a)) return;
        Double box12b = parseAmount(binding.inputBox12bAmount, binding.inputBox12bAmountLayout);
        if (isParseError(box12b)) return;
        Double box12c = parseAmount(binding.inputBox12cAmount, binding.inputBox12cAmountLayout);
        if (isParseError(box12c)) return;
        Double box12d = parseAmount(binding.inputBox12dAmount, binding.inputBox12dAmountLayout);
        if (isParseError(box12d)) return;
        Double stateWages = parseAmount(binding.inputStateWages, binding.inputStateWagesLayout);
        if (isParseError(stateWages)) return;
        Double stateTax = parseAmount(binding.inputStateTax, binding.inputStateTaxLayout);
        if (isParseError(stateTax)) return;

        W2Entry entry = editing != null ? editing : new W2Entry();
        entry.personId = personId;
        entry.taxYear = taxYear;
        entry.employerName = employerName;
        entry.ein = emptyToNull(text(binding.inputEin));
        entry.wages = wages;
        entry.federalIncomeTax = federalTax;
        entry.socialSecurityWages = ssWages;
        entry.socialSecurityTax = ssTax;
        entry.medicareWages = medicareWages;
        entry.medicareTax = medicareTax;
        entry.box12aCode = emptyToNull(text(binding.inputBox12aCode));
        entry.box12aAmount = box12a;
        entry.box12bCode = emptyToNull(text(binding.inputBox12bCode));
        entry.box12bAmount = box12b;
        entry.box12cCode = emptyToNull(text(binding.inputBox12cCode));
        entry.box12cAmount = box12c;
        entry.box12dCode = emptyToNull(text(binding.inputBox12dCode));
        entry.box12dAmount = box12d;
        entry.box14 = emptyToNull(text(binding.inputBox14));
        entry.state = emptyToNull(text(binding.inputState));
        entry.stateWages = stateWages;
        entry.stateIncomeTax = stateTax;
        entry.notes = emptyToNull(text(binding.inputNotes));

        if (editing == null) app.getW2Repository().insert(entry, this::dismiss);
        else app.getW2Repository().update(entry, this::dismiss);
    }

    private void delete() {
        if (editing == null) return;
        UiUtils.confirmDelete(requireContext(), () ->
                app.getW2Repository().delete(editing, this::dismiss));
    }

    private void clearErrors() {
        binding.inputTaxYearLayout.setError(null);
        binding.inputEmployerLayout.setError(null);
        binding.inputWagesLayout.setError(null);
        binding.inputFederalTaxLayout.setError(null);
        binding.inputSsWagesLayout.setError(null);
        binding.inputSsTaxLayout.setError(null);
        binding.inputMedicareWagesLayout.setError(null);
        binding.inputMedicareTaxLayout.setError(null);
        binding.inputBox12aAmountLayout.setError(null);
        binding.inputBox12bAmountLayout.setError(null);
        binding.inputBox12cAmountLayout.setError(null);
        binding.inputBox12dAmountLayout.setError(null);
        binding.inputStateWagesLayout.setError(null);
        binding.inputStateTaxLayout.setError(null);
    }

    private static final Double PARSE_ERROR = Double.NaN;

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
