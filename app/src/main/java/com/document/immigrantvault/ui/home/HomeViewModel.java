package com.document.immigrantvault.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.repository.PersonRepository;
import com.document.immigrantvault.data.repository.ReminderRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final PersonRepository personRepository;
    private final ReminderRepository reminderRepository;

    public HomeViewModel(ImmigrantVaultApplication application) {
        personRepository = application.getPersonRepository();
        reminderRepository = application.getReminderRepository();
    }

    public LiveData<List<Person>> getPersons() {
        return personRepository.getAll();
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminderRepository.getAllEnabled();
    }
}
