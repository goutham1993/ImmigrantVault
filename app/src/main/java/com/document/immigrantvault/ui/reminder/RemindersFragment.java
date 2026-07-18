package com.document.immigrantvault.ui.reminder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.databinding.FragmentRemindersBinding;
import com.document.immigrantvault.databinding.ItemReminderBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.ui.ViewModelFactory;
import com.document.immigrantvault.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class RemindersFragment extends Fragment {

    private ReminderListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentRemindersBinding binding = FragmentRemindersBinding.inflate(inflater, container, false);

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        RemindersViewModel viewModel = new ViewModelProvider(this, new ViewModelFactory(app))
                .get(RemindersViewModel.class);

        adapter = new ReminderListAdapter(viewModel);
        binding.remindersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.remindersRecycler.setAdapter(adapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyIcon.setImageResource(R.drawable.ic_reminder);
        empty.emptyTitle.setText(R.string.empty_reminders);
        empty.emptySubtitle.setText(R.string.empty_reminders_subtitle);

        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.setItems(reminders);
            boolean isEmpty = reminders == null || reminders.isEmpty();
            binding.remindersRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            empty.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        return binding.getRoot();
    }

    static class ReminderListAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ReminderListAdapter.VH> {

        private final List<Reminder> items = new ArrayList<>();
        private final RemindersViewModel viewModel;

        ReminderListAdapter(RemindersViewModel viewModel) {
            this.viewModel = viewModel;
        }

        void setItems(List<Reminder> reminders) {
            items.clear();
            if (reminders != null) items.addAll(reminders);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemReminderBinding b = ItemReminderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final ItemReminderBinding binding;

            VH(ItemReminderBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Reminder reminder) {
                binding.reminderTitle.setText(reminder.title);
                binding.reminderBody.setText(reminder.body);
                String dateLabel = reminder.leadDays > 0
                        ? itemView.getContext().getString(
                                R.string.reminder_on_lead,
                                reminder.leadDays,
                                DateUtils.formatDate(reminder.triggerDate))
                        : itemView.getContext().getString(
                                R.string.reminder_on,
                                DateUtils.formatDate(reminder.triggerDate));
                binding.reminderDate.setText(dateLabel);
                binding.reminderSwitch.setOnCheckedChangeListener(null);
                binding.reminderSwitch.setChecked(reminder.enabled);
                binding.reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                        viewModel.setEnabled(reminder, isChecked));
            }
        }
    }
}
