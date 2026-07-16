package com.document.immigrantvault.ui.person.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.databinding.FragmentListTabBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.common.ListEntryAdapter;
import com.document.immigrantvault.ui.document.DocumentFormBottomSheet;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;

import java.util.ArrayList;
import java.util.List;

public class DocumentsTabFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private long personId;
    private List<Document> documents = new ArrayList<>();
    private ListEntryAdapter adapter;

    public static DocumentsTabFragment newInstance(long personId) {
        DocumentsTabFragment fragment = new DocumentsTabFragment();
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
        FragmentListTabBinding binding = FragmentListTabBinding.inflate(inflater, container, false);
        adapter = new ListEntryAdapter();
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRecycler.setAdapter(adapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyIcon.setImageResource(R.drawable.ic_document);
        empty.emptyTitle.setText(R.string.empty_documents);
        empty.emptySubtitle.setText(R.string.empty_documents_subtitle);

        adapter.setOnItemClickListener(position -> {
            Document doc = documents.get(position);
            DocumentFormBottomSheet.newInstance(personId, doc.id)
                    .show(getParentFragmentManager(), "document_form");
        });

        binding.fabAdd.setOnClickListener(v ->
                DocumentFormBottomSheet.newInstance(personId, null)
                        .show(getParentFragmentManager(), "document_form"));

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        app.getDocumentRepository().getByPerson(personId).observe(getViewLifecycleOwner(), list -> {
            documents = list != null ? list : new ArrayList<>();
            List<ListEntryAdapter.ListItem> items = new ArrayList<>();
            for (Document doc : documents) {
                items.add(new ListEntryAdapter.ListItem(
                        EnumLabels.documentType(doc.type),
                        doc.documentNumber,
                        "Expires " + DateUtils.formatDate(doc.expiryDate)));
            }
            adapter.setItems(items);
            boolean isEmpty = documents.isEmpty();
            binding.listRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        });

        return binding.getRoot();
    }
}
