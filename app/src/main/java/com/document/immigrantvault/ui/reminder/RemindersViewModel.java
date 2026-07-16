package com.document.immigrantvault.ui.reminder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.repository.ReminderRepository;

import java.util.List;

public class RemindersViewModel extends ViewModel {

    private final ReminderRepository reminderRepository;

    public RemindersViewModel(ImmigrantVaultApplication application) {
        reminderRepository = application.getReminderRepository();
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminderRepository.getAllEnabled();
    }

    public void setEnabled(Reminder reminder, boolean enabled) {
        reminder.enabled = enabled;
        reminderRepository.update(reminder);
    }
}
