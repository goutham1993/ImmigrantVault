package com.document.immigrantvault.extraction;

import androidx.annotation.Nullable;

import com.document.immigrantvault.data.db.entity.DocumentType;

import java.util.Date;

/** Fields extracted for the Documents form. All nullable for partial fills. */
public final class DocumentExtraction {

    @Nullable
    public DocumentType type;
    @Nullable
    public String documentNumber;
    @Nullable
    public String issuingCountry;
    @Nullable
    public String placeOfIssue;
    @Nullable
    public String nationality;
    @Nullable
    public Date issueDate;
    @Nullable
    public Date expiryDate;

    public boolean hasAnyField() {
        return documentNumber != null
                || issuingCountry != null
                || placeOfIssue != null
                || nationality != null
                || issueDate != null
                || expiryDate != null
                || type != null;
    }
}
