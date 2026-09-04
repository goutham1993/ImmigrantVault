package com.document.immigrantvault.ui.person.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.DocumentType;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.PetitionType;
import com.document.immigrantvault.data.db.entity.PreferenceCategory;
import com.document.immigrantvault.databinding.FragmentOverviewTabBinding;
import com.document.immigrantvault.ui.ViewModelFactory;
import com.document.immigrantvault.ui.person.PersonDetailViewModel;
import com.document.immigrantvault.ui.person.PersonFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.UiUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OverviewTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private FragmentOverviewTabBinding binding;
    private List<Petition> petitions = new ArrayList<>();
    private List<Document> documents = new ArrayList<>();

    public static OverviewTabFragment newInstance(long personId) {
        OverviewTabFragment fragment = new OverviewTabFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            personId = getArguments().getLong(ARG_PERSON_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOverviewTabBinding.inflate(inflater, container, false);

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        PersonDetailViewModel viewModel = new ViewModelProvider(requireParentFragment(),
                new ViewModelFactory(app)).get(PersonDetailViewModel.class);

        viewModel.getPerson(personId).observe(getViewLifecycleOwner(), person -> {
            if (person == null || binding == null) {
                return;
            }
            binding.overviewBirthday.setText(
                    person.dateOfBirth != null
                            ? DateUtils.formatDate(person.dateOfBirth)
                            : getString(R.string.overview_no_birthday));
            binding.overviewSsn.setText(
                    person.ssnLast4 != null && !person.ssnLast4.isEmpty()
                            ? "•••• " + person.ssnLast4
                            : getString(R.string.overview_no_ssn));
            UiUtils.bindCopyOnLongPress(binding.overviewSsn, person.ssnLast4,
                    getString(R.string.ssn_last4_copied));

            if (person.aNumber != null && !person.aNumber.isEmpty()) {
                binding.overviewANumberLabel.setVisibility(View.VISIBLE);
                binding.overviewANumber.setVisibility(View.VISIBLE);
                binding.overviewANumber.setText(person.aNumber);
                UiUtils.bindCopyOnLongPress(binding.overviewANumber, person.aNumber,
                        getString(R.string.a_number_copied));
            } else {
                binding.overviewANumberLabel.setVisibility(View.GONE);
                binding.overviewANumber.setVisibility(View.GONE);
                binding.overviewANumber.setOnLongClickListener(null);
            }

            String visa = person.currentVisaType != null ? person.currentVisaType
                    : getString(R.string.status_no_visa);
            binding.overviewVisa.setText(visa);
            binding.overviewDates.setText(
                    DateUtils.formatDate(person.visaStartDate) + " – "
                            + DateUtils.formatDate(person.visaEndDate));
            binding.overviewDays.setText(DateUtils.daysUntilLabel(person.visaEndDate));

            binding.overviewEmployer.setText(
                    person.currentEmployer != null && !person.currentEmployer.isEmpty()
                            ? person.currentEmployer
                            : getString(R.string.overview_no_employer));
            binding.overviewRole.setText(
                    person.currentRole != null && !person.currentRole.isEmpty()
                            ? person.currentRole
                            : getString(R.string.overview_no_role));
        });

        app.getPetitionRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            petitions = list != null ? list : new ArrayList<>();
            refreshImmigrationCards();
        });
        app.getDocumentRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            documents = list != null ? list : new ArrayList<>();
            refreshImmigrationCards();
        });

        binding.actionEditProfile.setOnClickListener(v ->
                PersonFormBottomSheet.newInstance(personId)
                        .show(getParentFragmentManager(), "edit_person"));

        return binding.getRoot();
    }

    private void refreshImmigrationCards() {
        if (binding == null) {
            return;
        }
        refreshGreenCardCard();
        refreshCitizenshipCard();
    }

    private void refreshGreenCardCard() {
        Petition i140 = firstOfType(PetitionType.I140);
        Petition i485 = firstOfType(PetitionType.I485);
        boolean show = i140 != null || i485 != null;
        binding.cardGreenCardPath.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }

        Date priorityDate = coalesceDate(
                i140 != null ? i140.priorityDate : null,
                i485 != null ? i485.priorityDate : null);
        PreferenceCategory category = coalesceCategory(
                i140 != null ? i140.preferenceCategory : null,
                i485 != null ? i485.preferenceCategory : null);
        String chargeability = coalesceString(
                i140 != null ? i140.countryOfChargeability : null,
                i485 != null ? i485.countryOfChargeability : null);

        binding.overviewPriorityDate.setText(
                priorityDate != null
                        ? DateUtils.formatDate(priorityDate)
                        : getString(R.string.overview_no_priority_date));
        binding.overviewCategory.setText(
                category != null
                        ? EnumLabels.preferenceCategory(category)
                        : getString(R.string.overview_no_category));
        binding.overviewChargeability.setText(
                chargeability != null
                        ? chargeability
                        : getString(R.string.overview_no_chargeability));

        if (i485 != null) {
            String status = EnumLabels.petitionStatus(i485.status);
            if (i485.receiptNumber != null && !i485.receiptNumber.isEmpty()) {
                status += " · " + i485.receiptNumber;
            }
            binding.overviewI485.setText("I-485 — " + status);
            UiUtils.bindCopyOnLongPress(binding.overviewI485, i485.receiptNumber,
                    getString(R.string.receipt_copied));
        } else {
            binding.overviewI485.setText(getString(R.string.overview_no_i485));
            binding.overviewI485.setOnLongClickListener(null);
        }
    }

    private void refreshCitizenshipCard() {
        Petition n400 = firstOfType(PetitionType.N400);
        Document greenCard = firstGreenCard();
        boolean show = n400 != null || greenCard != null;
        binding.cardCitizenship.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }

        if (greenCard != null && greenCard.issueDate != null) {
            binding.overviewLprSince.setText(
                    getString(R.string.overview_lpr_since, DateUtils.formatDate(greenCard.issueDate)));
        } else {
            binding.overviewLprSince.setText(getString(R.string.overview_no_green_card_doc));
        }

        if (n400 != null) {
            String status = "N-400 — " + EnumLabels.petitionStatus(n400.status);
            if (n400.receiptNumber != null && !n400.receiptNumber.isEmpty()) {
                status += " · " + n400.receiptNumber;
            }
            binding.overviewN400.setText(status);
            UiUtils.bindCopyOnLongPress(binding.overviewN400, n400.receiptNumber,
                    getString(R.string.receipt_copied));

            if (n400.interviewDate != null) {
                binding.overviewInterview.setVisibility(View.VISIBLE);
                binding.overviewInterview.setText(
                        getString(R.string.overview_interview, DateUtils.formatDate(n400.interviewDate)));
            } else {
                binding.overviewInterview.setVisibility(View.GONE);
            }
            if (n400.oathDate != null) {
                binding.overviewOath.setVisibility(View.VISIBLE);
                binding.overviewOath.setText(
                        getString(R.string.overview_oath, DateUtils.formatDate(n400.oathDate)));
            } else {
                binding.overviewOath.setVisibility(View.GONE);
            }
        } else {
            binding.overviewN400.setText(getString(R.string.overview_no_n400));
            binding.overviewN400.setOnLongClickListener(null);
            binding.overviewInterview.setVisibility(View.GONE);
            binding.overviewOath.setVisibility(View.GONE);
        }
    }

    private Petition firstOfType(PetitionType type) {
        for (Petition petition : petitions) {
            if (petition.type == type) {
                return petition;
            }
        }
        return null;
    }

    private Document firstGreenCard() {
        for (Document document : documents) {
            if (document.type == DocumentType.GREEN_CARD) {
                return document;
            }
        }
        return null;
    }

    private static Date coalesceDate(Date first, Date second) {
        return first != null ? first : second;
    }

    private static PreferenceCategory coalesceCategory(PreferenceCategory first,
                                                       PreferenceCategory second) {
        return first != null ? first : second;
    }

    private static String coalesceString(String first, String second) {
        if (first != null && !first.isEmpty()) {
            return first;
        }
        if (second != null && !second.isEmpty()) {
            return second;
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
