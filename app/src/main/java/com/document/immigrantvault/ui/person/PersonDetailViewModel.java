package com.document.immigrantvault.ui.person;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.repository.PersonRepository;

public class PersonDetailViewModel extends ViewModel {

    private final PersonRepository personRepository;

    public PersonDetailViewModel(ImmigrantVaultApplication application) {
        personRepository = application.getPersonRepository();
    }

    public LiveData<Person> getPerson(long personId) {
        return personRepository.getById(personId);
    }
}
