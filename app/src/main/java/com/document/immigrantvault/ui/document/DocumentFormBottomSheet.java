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
import com.document.immigrantvault.databinding.BottomSheetDocumentFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
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
    private long personId;
    private Document editing;
    private Date issueDate;
    private Date expiryDate;

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
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        setupTypeDropdown();
        DatePickerHelper.bind(requireContext(), binding.inputIssueDate, null, d -> issueDate = d);
        DatePickerHelper.bind(requireContext(), binding.inputExpiryDate, null, d -> expiryDate = d);

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        if (requireArguments().containsKey(ARG_DOCUMENT_ID)) {
            long docId = requireArguments().getLong(ARG_DOCUMENT_ID);
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            app.getExecutor().execute(() -> {
                Document doc = app.getDatabase().documentDao().getByIdSync(docId);
                if (doc != null) {
                    requireActivity().runOnUiThread(() -> populate(doc));
                }
            });
        } else {
            binding.formTitle.setText(R.string.add_document);
            updatePassportFields(typeFromLabel(dropdownText(binding.inputType)));
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

    private void updatePassportFields(DocumentType type) {
        boolean isPassport = type == DocumentType.PASSPORT;
        binding.inputAuthorityLayout.setHint(isPassport
                ? getString(R.string.label_issuing_country)
                : getString(R.string.label_issuing_authority));
        binding.inputPlaceOfIssueLayout.setVisibility(isPassport ? View.VISIBLE : View.GONE);
        binding.inputNationalityLayout.setVisibility(isPassport ? View.VISIBLE : View.GONE);
    }

    private void populate(Document doc) {
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
            binding.inputIssueDate.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(issueDate));
        }
        if (expiryDate != null) {
            binding.inputExpiryDate.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(expiryDate));
        }
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

        if (editing == null) {
            app.getDocumentRepository().insert(doc, this::dismiss);
        } else {
            app.getDocumentRepository().update(doc, this::dismiss);
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
