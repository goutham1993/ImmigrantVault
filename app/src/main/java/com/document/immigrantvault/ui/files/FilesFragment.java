package com.document.immigrantvault.ui.files;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.dao.VaultFileDao;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.databinding.FragmentFilesBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root of the Files section: one row per person, acting as the top-level folder.
 */
public class FilesFragment extends Fragment {

    private FragmentFilesBinding binding;
    private VaultRowAdapter adapter;
    private List<Person> persons = new ArrayList<>();
    private final Map<Long, Integer> fileCounts = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFilesBinding.inflate(inflater, container, false);

        adapter = new VaultRowAdapter();
        binding.filesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.filesRecycler.setAdapter(adapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyIcon.setImageResource(R.drawable.ic_files);
        empty.emptyTitle.setText(R.string.files_empty_people);
        empty.emptySubtitle.setText(R.string.files_empty_people_subtitle);

        adapter.setOnRowClickListener(position -> {
            Person person = persons.get(position);
            Bundle args = new Bundle();
            args.putLong("personId", person.id);
            args.putLong("folderId", -1L);
            args.putString("title", person.name);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_files_to_browser, args);
        });

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();

        app.getVaultFolderRepository().getPersonFileCounts()
                .observe(getViewLifecycleOwner(), counts -> {
                    fileCounts.clear();
                    if (counts != null) {
                        for (VaultFileDao.PersonFileCount count : counts) {
                            fileCounts.put(count.ownerId, count.fileCount);
                        }
                    }
                    render();
                });

        app.getPersonRepository().getAll().observe(getViewLifecycleOwner(), list -> {
            persons = list != null ? list : new ArrayList<>();
            render();
        });

        return binding.getRoot();
    }

    private void render() {
        if (binding == null) {
            return;
        }
        List<VaultRowAdapter.Row> rows = new ArrayList<>();
        for (Person person : persons) {
            Integer count = fileCounts.get(person.id);
            rows.add(new VaultRowAdapter.Row(
                    person.name,
                    FileFormat.fileCountLabel(requireContext(), count != null ? count : 0),
                    R.drawable.ic_person,
                    false));
        }
        adapter.setRows(rows);

        boolean isEmpty = persons.isEmpty();
        binding.filesRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyState.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
