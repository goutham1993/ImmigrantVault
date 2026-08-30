package com.document.immigrantvault.extraction;

import androidx.annotation.Nullable;

/** Fields extracted for the W-2 form. All nullable for partial fills. */
public final class W2Extraction {

    @Nullable
    public Integer taxYear;
    @Nullable
    public String employerName;
    @Nullable
    public String ein;
    @Nullable
    public Double wages;
    @Nullable
    public Double federalIncomeTax;
    @Nullable
    public Double socialSecurityWages;
    @Nullable
    public Double socialSecurityTax;
    @Nullable
    public Double medicareWages;
    @Nullable
    public Double medicareTax;
    @Nullable
    public String box12aCode;
    @Nullable
    public Double box12aAmount;
    @Nullable
    public String box12bCode;
    @Nullable
    public Double box12bAmount;
    @Nullable
    public String box12cCode;
    @Nullable
    public Double box12cAmount;
    @Nullable
    public String box12dCode;
    @Nullable
    public Double box12dAmount;
    @Nullable
    public String box14;
    @Nullable
    public String state;
    @Nullable
    public Double stateWages;
    @Nullable
    public Double stateIncomeTax;

    public boolean hasAnyField() {
        return taxYear != null
                || employerName != null
                || ein != null
                || wages != null
                || federalIncomeTax != null
                || socialSecurityWages != null
                || socialSecurityTax != null
                || medicareWages != null
                || medicareTax != null
                || box12aCode != null
                || box12aAmount != null
                || box12bCode != null
                || box12bAmount != null
                || box12cCode != null
                || box12cAmount != null
                || box12dCode != null
                || box12dAmount != null
                || box14 != null
                || state != null
                || stateWages != null
                || stateIncomeTax != null;
    }
}
