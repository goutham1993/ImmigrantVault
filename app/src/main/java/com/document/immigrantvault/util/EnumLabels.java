package com.document.immigrantvault.util;

import com.document.immigrantvault.data.db.entity.DocumentType;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.PetitionType;
import com.document.immigrantvault.data.db.entity.Relationship;

public final class EnumLabels {

    private EnumLabels() {
    }

    public static String relationship(Relationship relationship) {
        if (relationship == null) {
            return "Other";
        }
        switch (relationship) {
            case SELF:
                return "You";
            case SPOUSE:
                return "Spouse";
            case CHILD:
                return "Child";
            default:
                return "Other";
        }
    }

    public static String documentType(DocumentType type) {
        if (type == null) {
            return "Document";
        }
        switch (type) {
            case PASSPORT:
                return "Passport";
            case VISA_STAMP:
                return "Visa Stamp";
            case I797:
                return "I-797";
            case EAD:
                return "EAD";
            case I20:
                return "I-20";
            case I94:
                return "I-94";
            case GREEN_CARD:
                return "Green Card";
            default:
                return "Other";
        }
    }

    public static String petitionType(PetitionType type) {
        if (type == null) {
            return "Petition";
        }
        switch (type) {
            case H1B:
                return "H-1B";
            case H4:
                return "H-4";
            case L1:
                return "L-1";
            case I140:
                return "I-140";
            case I485:
                return "I-485";
            case I765:
                return "I-765";
            case I131:
                return "I-131";
            case N400:
                return "N-400";
            default:
                return "Other";
        }
    }

    public static String petitionStatus(PetitionStatus status) {
        if (status == null) {
            return "Unknown";
        }
        switch (status) {
            case PENDING:
                return "Pending";
            case APPROVED:
                return "Approved";
            case DENIED:
                return "Denied";
            case RFE:
                return "RFE";
            default:
                return "Other";
        }
    }
}
