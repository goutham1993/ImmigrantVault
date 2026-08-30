package com.document.immigrantvault.ui.files;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.immigrantvault.databinding.BottomSheetAddFileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Lets the user pick how a document enters the vault. The choice is handed back through the
 * fragment result API so the launchers stay registered on the hosting fragment.
 */
public class AddFileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "add_file";

    private static final String RESULT_KEY = "add_file_result";
    private static final String RESULT_CHOICE = "choice";

    public enum Choice {
        SCAN,
        PHOTO,
        UPLOAD
    }

    public interface OnChoiceListener {
        void onChoice(Choice choice);
    }

    private BottomSheetAddFileBinding binding;

    public static AddFileBottomSheet newInstance() {
        return new AddFileBottomSheet();
    }

    public static void setResultListener(Fragment host, OnChoiceListener listener) {
        host.getParentFragmentManager().setFragmentResultListener(
                RESULT_KEY,
                host.getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String choice = result.getString(RESULT_CHOICE);
                    if (choice != null) {
                        listener.onChoice(Choice.valueOf(choice));
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddFileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.optionScan.setOnClickListener(v -> deliver(Choice.SCAN));
        binding.optionPhoto.setOnClickListener(v -> deliver(Choice.PHOTO));
        binding.optionUpload.setOnClickListener(v -> deliver(Choice.UPLOAD));
    }

    private void deliver(Choice choice) {
        Bundle result = new Bundle();
        result.putString(RESULT_CHOICE, choice.name());
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
