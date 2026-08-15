package com.document.immigrantvault;

import com.document.immigrantvault.data.db.entity.Person;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PersonNameTest {

    @Test
    public void splitLegacyName_preservesCommonNameParts() {
        assertArrayEquals(
                new String[]{"John", "Michael", "Doe"},
                Person.splitLegacyName("  John   Michael   Doe  ")
        );
    }

    @Test
    public void splitLegacyName_handlesSingleAndTwoPartNames() {
        assertArrayEquals(
                new String[]{"Madonna", "", ""},
                Person.splitLegacyName("Madonna")
        );
        assertArrayEquals(
                new String[]{"Jane", "", "Doe"},
                Person.splitLegacyName("Jane Doe")
        );
    }

    @Test
    public void displayName_usesStructuredFieldsAndLegacyFallback() {
        Person person = new Person();
        person.setNameParts("Jane", "Mary", "Doe");
        assertEquals("Jane Mary Doe", person.getDisplayName());

        Person legacyPerson = new Person();
        legacyPerson.name = "Legacy Name";
        assertEquals("Legacy Name", legacyPerson.getDisplayName());
    }
}
