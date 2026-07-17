package com.document.immigrantvault.ui.link;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.UsefulLink;
import com.document.immigrantvault.databinding.BottomSheetLinkFormBinding;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

public class LinkFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_ENTRY_ID = "entry_id";

    private BottomSheetLinkFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private UsefulLink editing;

    public static LinkFormBottomSheet newInstance(long personId, Long entryId) {
        LinkFormBottomSheet sheet = new LinkFormBottomSheet();
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
        binding = BottomSheetLinkFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());

        if (requireArguments().containsKey(ARG_ENTRY_ID)) {
            binding.formTitle.setText(R.string.action_edit);
            binding.btnDelete.setVisibility(View.VISIBLE);
            long id = requireArguments().getLong(ARG_ENTRY_ID);
            app.getExecutor().execute(() -> {
                UsefulLink link = app.getDatabase().usefulLinkDao().getByIdSync(id);
                if (link != null) requireActivity().runOnUiThread(() -> populate(link));
            });
        } else {
            binding.formTitle.setText(R.string.add_link);
        }
    }

    private void populate(UsefulLink link) {
        editing = link;
        binding.inputTitle.setText(link.title);
        binding.inputUrl.setText(link.url);
        binding.inputNotes.setText(link.notes);
    }

    private void save() {
        String title = text(binding.inputTitle);
        String url = text(binding.inputUrl);

        boolean valid = true;
        if (title.isEmpty()) {
            binding.inputTitleLayout.setError(getString(R.string.error_required));
            valid = false;
        } else {
            binding.inputTitleLayout.setError(null);
        }
        if (url.isEmpty()) {
            binding.inputUrlLayout.setError(getString(R.string.error_required));
            valid = false;
        } else {
            binding.inputUrlLayout.setError(null);
        }
        if (!valid) return;

        UsefulLink link = editing != null ? editing : new UsefulLink();
        link.personId = personId;
        link.title = title;
        link.url = normalizeUrl(url);
        link.notes = emptyToNull(text(binding.inputNotes));

        if (editing == null) app.getUsefulLinkRepository().insert(link, this::dismiss);
        else app.getUsefulLinkRepository().update(link, this::dismiss);
    }

    private void delete() {
        if (editing == null) return;
        UiUtils.confirmDelete(requireContext(), () ->
                app.getUsefulLinkRepository().delete(editing, this::dismiss));
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.contains("://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
