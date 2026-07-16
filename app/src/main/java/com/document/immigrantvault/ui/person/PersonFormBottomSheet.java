package com.document.immigrantvault.ui.person;

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
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Relationship;
import com.document.immigrantvault.databinding.BottomSheetPersonFormBinding;
import com.document.immigrantvault.util.DatePickerHelper;
import com.document.immigrantvault.util.EnumLabels;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

public class PersonFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";

    private BottomSheetPersonFormBinding binding;
    private ImmigrantVaultApplication app;
    private Person editing;
    private Date dateOfBirth;
    private Date visaStart;
    private Date visaEnd;

    public static PersonFormBottomSheet newInstance(Long personId) {
        PersonFormBottomSheet sheet = new PersonFormBottomSheet();
        Bundle args = new Bundle();
        if (personId != null) {
            args.putLong(ARG_PERSON_ID, personId);
        }
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPersonFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();

        setupRelationshipDropdown();
        DatePickerHelper.bind(requireContext(), binding.inputDateOfBirth, null,
                date -> dateOfBirth = date);
        DatePickerHelper.bind(requireContext(), binding.inputVisaStart, null,
                date -> visaStart = date);
        DatePickerHelper.bind(requireContext(), binding.inputVisaEnd, null,
                date -> visaEnd = date);

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_PERSON_ID)) {
            long personId = args.getLong(ARG_PERSON_ID);
            binding.formTitle.setText(R.string.edit_person);
            app.getExecutor().execute(() -> {
                Person person = app.getDatabase().personDao().getByIdSync(personId);
                if (person != null) {
                    requireActivity().runOnUiThread(() -> populate(person));
                }
            });
        } else {
            binding.formTitle.setText(R.string.add_family_member);
        }
    }

    private void setupRelationshipDropdown() {
        String[] labels = {
                EnumLabels.relationship(Relationship.SELF),
                EnumLabels.relationship(Relationship.SPOUSE),
                EnumLabels.relationship(Relationship.CHILD),
                EnumLabels.relationship(Relationship.OTHER)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, labels);
        AutoCompleteTextView dropdown = binding.inputRelationship;
        dropdown.setAdapter(adapter);
        dropdown.setText(labels[0], false);
    }

    private void populate(Person person) {
        editing = person;
        binding.inputName.setText(person.name);
        dateOfBirth = person.dateOfBirth;
        if (dateOfBirth != null) {
            binding.inputDateOfBirth.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(dateOfBirth));
        }
        binding.inputRelationship.setText(EnumLabels.relationship(person.relationship), false);
        binding.inputVisaType.setText(person.currentVisaType);
        binding.inputANumber.setText(person.aNumber);
        binding.inputCountry.setText(person.countryOfBirth);
        binding.inputCurrentEmployer.setText(person.currentEmployer);
        binding.inputCurrentRole.setText(person.currentRole);
        binding.inputNotes.setText(person.notes);
        visaStart = person.visaStartDate;
        visaEnd = person.visaEndDate;
        if (visaStart != null) {
            binding.inputVisaStart.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(visaStart));
        }
        if (visaEnd != null) {
            binding.inputVisaEnd.setText(
                    com.document.immigrantvault.util.DateUtils.formatDate(visaEnd));
        }
        if (person.relationship == Relationship.SELF) {
            binding.inputRelationshipLayout.setEnabled(false);
        }
    }

    private void save() {
        String name = text(binding.inputName);
        if (name.isEmpty()) {
            binding.inputNameLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputNameLayout.setError(null);

        if (visaStart != null && visaEnd != null && !visaEnd.after(visaStart)) {
            binding.inputVisaEndLayout.setError(getString(R.string.error_date_range));
            return;
        }
        binding.inputVisaEndLayout.setError(null);

        Person person = editing != null ? editing : new Person();
        person.name = name;
        person.dateOfBirth = dateOfBirth;
        if (editing == null || editing.relationship != Relationship.SELF) {
            person.relationship = relationshipFromLabel(dropdownText(binding.inputRelationship));
        }
        person.currentVisaType = text(binding.inputVisaType);
        person.visaStartDate = visaStart;
        person.visaEndDate = visaEnd;
        person.aNumber = text(binding.inputANumber);
        person.countryOfBirth = text(binding.inputCountry);
        person.currentEmployer = text(binding.inputCurrentEmployer);
        person.currentRole = text(binding.inputCurrentRole);
        person.notes = text(binding.inputNotes);

        if (editing == null) {
            app.getPersonRepository().insert(person, this::dismiss);
        } else {
            app.getPersonRepository().update(person, this::dismiss);
        }
    }

    private Relationship relationshipFromLabel(String label) {
        if (EnumLabels.relationship(Relationship.SPOUSE).equals(label)) {
            return Relationship.SPOUSE;
        }
        if (EnumLabels.relationship(Relationship.CHILD).equals(label)) {
            return Relationship.CHILD;
        }
        if (EnumLabels.relationship(Relationship.SELF).equals(label)) {
            return Relationship.SELF;
        }
        return Relationship.OTHER;
    }

    private String text(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String dropdownText(AutoCompleteTextView editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
