package com.document.immigrantvault.extraction;

import androidx.annotation.Nullable;

import java.util.Date;

/** Fields extracted for the I-94 form. All nullable for partial fills. */
public final class I94Extraction {

    @Nullable
    public String i94Number;
    @Nullable
    public String documentNumber;
    @Nullable
    public String countryOfCitizenship;
    @Nullable
    public Date arrivalDate;
    @Nullable
    public Date admitUntilDate;
    @Nullable
    public String portOfEntry;
    @Nullable
    public String classOfAdmission;

    public boolean hasAnyField() {
        return i94Number != null
                || documentNumber != null
                || countryOfCitizenship != null
                || arrivalDate != null
                || admitUntilDate != null
                || portOfEntry != null
                || classOfAdmission != null;
    }
}
