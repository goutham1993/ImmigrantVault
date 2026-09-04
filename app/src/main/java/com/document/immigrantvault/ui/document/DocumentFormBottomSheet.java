package com.document.immigrantvault.ui.document;

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
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.DocumentType;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.repository.ReminderRepository;
import com.document.immigrantvault.databinding.BottomSheetDocumentFormBinding;
import com.document.immigrantvault.extraction.DocumentExtraction;
import com.document.immigrantvault.extraction.DocumentFieldParser;
import com.document.immigrantvault.extraction.FormScanController;
import com.document.immigrantvault.extraction.OcrText;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class DocumentFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_DOCUMENT_ID = "document_id";

    private BottomSheetDocumentFormBinding binding;
    private ImmigrantVaultApplication app;
    private FormScanController scanController;
    private long personId;
    private Document editing;
    private Date issueDate;
    private Date expiryDate;
    private int selectedLeadDays = ReminderRepository.DEFAULT_LEAD_DAYS;

    public static DocumentFormBottomSheet newInstance(long personId, Long documentId) {
        DocumentFormBottomSheet sheet = new DocumentFormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        if (documentId != null) {
            args.putLong(ARG_DOCUMENT_ID, documentId);
        }
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
        binding = BottomSheetDocumentFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UiUtils.autoCapitalizeInputs(view);
        personId = requireArguments().getLong(ARG_PERSON_ID);

        setupTypeDropdown();
        setupLeadDaysDropdown();
        DatePickerHelper.bind(requireContext(), binding.inputIssueDate, null, d -> issueDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputExpiryDate, null, d -> {
            expiryDate = d;
            updateReminderUi();
        });

        binding.switchRemind.setOnCheckedChangeListener((buttonView, isChecked) -> updateReminderUi());
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());
        binding.btnScan.setOnClickListener(v -> startScan());
        binding.btnUpload.setOnClickListener(v -> startUpload());

        if (requireArguments().containsKey(ARG_DOCUMENT_ID)) {
            long docId = requireArguments().getLong(ARG_DOCUMENT_ID);
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            app.getExecutor().execute(() -> {
                Document doc = app.getDatabase().documentDao().getByIdSync(docId);
                Reminder reminder = app.getReminderRepository()
                        .getByLinkedSync(LinkedEntityType.DOCUMENT, docId);
                if (doc != null) {
                    requireActivity().runOnUiThread(() -> populate(doc, reminder));
                }
            });
        } else {
            binding.formTitle.setText(R.string.add_document);
            updatePassportFields(typeFromLabel(dropdownText(binding.inputType)));
            updateReminderUi();
        }
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
        DocumentType selected = typeFromLabel(dropdownText(binding.inputType));
        DocumentExtraction extraction = DocumentFieldParser.parse(ocr, selected);
        if (!extraction.hasAnyField()) {
            scanController.showFailed();
            return;
        }
        applyExtraction(extraction);
        scanController.showFilled();
    }

    private void applyExtraction(@NonNull DocumentExtraction extraction) {
        if (extraction.type != null) {
            binding.inputType.setText(EnumLabels.documentType(extraction.type), false);
            updatePassportFields(extraction.type);
        }
        if (extraction.documentNumber != null) {
            binding.inputNumber.setText(extraction.documentNumber);
            binding.inputNumberLayout.setError(null);
        }
        if (extraction.issuingCountry != null) {
            binding.inputAuthority.setText(extraction.issuingCountry);
        }
        if (extraction.placeOfIssue != null) {
            binding.inputPlaceOfIssue.setText(extraction.placeOfIssue);
        }
        if (extraction.nationality != null) {
            binding.inputNationality.setText(extraction.nationality);
        }
        if (extraction.issueDate != null) {
            issueDate = extraction.issueDate;
            binding.inputIssueDate.setText(DateUtils.formatDate(issueDate));
        }
        if (extraction.expiryDate != null) {
            expiryDate = extraction.expiryDate;
            binding.inputExpiryDate.setText(DateUtils.formatDate(expiryDate));
            updateReminderUi();
        }
    }

    private void setupTypeDropdown() {
        DocumentType[] types = DocumentType.values();
        String[] labels = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            labels[i] = EnumLabels.documentType(types[i]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, labels);
        AutoCompleteTextView dropdown = binding.inputType;
        dropdown.setAdapter(adapter);
        dropdown.setText(labels[0], false);
        dropdown.setOnItemClickListener((parent, view, position, id) ->
                updatePassportFields(types[position]));
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
        boolean hasExpiry = expiryDate != null;
        binding.switchRemind.setEnabled(hasExpiry);
        if (!hasExpiry) {
            binding.switchRemind.setChecked(false);
        }
        binding.inputLeadDaysLayout.setVisibility(
                hasExpiry && binding.switchRemind.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void updatePassportFields(DocumentType type) {
        boolean isPassport = type == DocumentType.PASSPORT;
        binding.inputAuthorityLayout.setHint(isPassport
                ? getString(R.string.label_issuing_country)
                : getString(R.string.label_issuing_authority));
        binding.inputPlaceOfIssueLayout.setVisibility(isPassport ? View.VISIBLE : View.GONE);
        binding.inputNationalityLayout.setVisibility(isPassport ? View.VISIBLE : View.GONE);
    }

    private void populate(Document doc, Reminder reminder) {
        editing = doc;
        binding.inputType.setText(EnumLabels.documentType(doc.type), false);
        updatePassportFields(doc.type);
        binding.inputNumber.setText(doc.documentNumber);
        binding.inputAuthority.setText(doc.issuingCountry);
        binding.inputPlaceOfIssue.setText(doc.placeOfIssue);
        binding.inputNationality.setText(doc.nationality);
        binding.inputNotes.setText(doc.notes);
        issueDate = doc.issueDate;
        expiryDate = doc.expiryDate;
        if (issueDate != null) {
            binding.inputIssueDate.setText(DateUtils.formatDate(issueDate));
        }
        if (expiryDate != null) {
            binding.inputExpiryDate.setText(DateUtils.formatDate(expiryDate));
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

    private DocumentType typeFromLabel(String label) {
        for (DocumentType type : DocumentType.values()) {
            if (EnumLabels.documentType(type).equals(label)) {
                return type;
            }
        }
        return DocumentType.OTHER;
    }

    private void save() {
        String number = text(binding.inputNumber);
        if (number.isEmpty()) {
            binding.inputNumberLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputNumberLayout.setError(null);

        DocumentType type = typeFromLabel(dropdownText(binding.inputType));
        Document doc = editing != null ? editing : new Document();
        doc.personId = personId;
        doc.type = type;
        doc.documentNumber = number;
        doc.issuingCountry = text(binding.inputAuthority);
        if (type == DocumentType.PASSPORT) {
            doc.placeOfIssue = text(binding.inputPlaceOfIssue);
            doc.nationality = text(binding.inputNationality);
        } else {
            doc.placeOfIssue = null;
            doc.nationality = null;
        }
        doc.issueDate = issueDate;
        doc.expiryDate = expiryDate;
        doc.notes = text(binding.inputNotes);

        Integer leadDays = (expiryDate != null && binding.switchRemind.isChecked())
                ? selectedLeadDays
                : null;

        if (editing == null) {
            app.getDocumentRepository().insert(doc, leadDays, this::dismiss);
        } else {
            app.getDocumentRepository().update(doc, leadDays, this::dismiss);
        }
    }

    private void delete() {
        if (editing == null) {
            return;
        }
        UiUtils.confirmDelete(requireContext(), () ->
                app.getDocumentRepository().delete(editing, this::dismiss));
    }

    private String text(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String dropdownText(AutoCompleteTextView editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
