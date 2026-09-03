package com.document.immigrantvault.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.databinding.FragmentHomeBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.ViewModelFactory;
import com.document.immigrantvault.ui.person.PersonFormBottomSheet;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private PersonCardAdapter personAdapter;
    private DeadlineAdapter deadlineAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(app)).get(HomeViewModel.class);

        personAdapter = new PersonCardAdapter();
        personAdapter.setOnPersonClickListener(person -> {
            Bundle args = new Bundle();
            args.putLong("personId", person.id);
            Navigation.findNavController(view).navigate(R.id.action_home_to_personDetail, args);
        });
        personAdapter.setOnPersonLongClickListener(this::confirmDeletePerson);

        deadlineAdapter = new DeadlineAdapter();
        binding.deadlinesRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.deadlinesRecycler.setAdapter(deadlineAdapter);

        int spanCount = getResources().getBoolean(R.bool.is_wide_screen) ? 2 : 1;
        binding.personsRecycler.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        binding.personsRecycler.setAdapter(personAdapter);

        binding.fabAddPerson.setOnClickListener(v -> showPersonForm(null));

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyIcon.setImageResource(R.drawable.ic_person);
        empty.emptyTitle.setText(R.string.home_empty_title);
        empty.emptySubtitle.setText(R.string.home_empty_subtitle);
        empty.emptyAction.setVisibility(View.VISIBLE);
        empty.emptyAction.setText(R.string.home_edit_profile);
        empty.emptyAction.setOnClickListener(v -> showPersonForm(null));

        viewModel.getPersons().observe(getViewLifecycleOwner(), persons -> {
            personAdapter.setItems(persons);
            boolean emptyList = persons == null || persons.isEmpty();
            binding.personsRecycler.setVisibility(emptyList ? View.GONE : View.VISIBLE);
            binding.emptyState.getRoot().setVisibility(emptyList ? View.VISIBLE : View.GONE);
            binding.fabAddPerson.setVisibility(emptyList ? View.GONE : View.VISIBLE);
        });

        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            deadlineAdapter.setItems(reminders);
            boolean hasDeadlines = reminders != null && !reminders.isEmpty();
            binding.deadlinesTitle.setVisibility(hasDeadlines ? View.VISIBLE : View.GONE);
            binding.deadlinesRecycler.setVisibility(hasDeadlines ? View.VISIBLE : View.GONE);
        });
    }

    private void confirmDeletePerson(Person person) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_person_confirm, person.getDisplayName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.deletePerson(person, () -> {
                            if (!isAdded()) {
                                return;
                            }
                            Toast.makeText(requireContext(), R.string.files_deleted,
                                    Toast.LENGTH_SHORT).show();
                        }))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showPersonForm(Long personId) {
        PersonFormBottomSheet sheet = PersonFormBottomSheet.newInstance(personId);
        sheet.show(getParentFragmentManager(), "person_form");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
