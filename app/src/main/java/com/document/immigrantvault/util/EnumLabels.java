package com.document.immigrantvault.util;

import com.document.immigrantvault.data.db.entity.DocumentType;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.PetitionType;
import com.document.immigrantvault.data.db.entity.Relationship;
import com.document.immigrantvault.data.db.entity.VisaType;

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
            case DRIVERS_LICENSE:
                return "Driver's License";
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

    public static String visaType(VisaType type) {
        if (type == null) {
            return "Visa";
        }
        switch (type) {
            case F1:
                return "F-1 (Student)";
            case F1_OPT:
                return "F-1 OPT";
            case STEM_OPT:
                return "STEM OPT";
            case H1B:
                return "H-1B";
            case H4:
                return "H-4";
            case H4_EAD:
                return "H-4 EAD";
            case L1:
                return "L-1";
            case L2:
                return "L-2";
            case B1_B2:
                return "B-1/B-2";
            case J1:
                return "J-1";
            case O1:
                return "O-1";
            case TN:
                return "TN";
            default:
                return "Other";
        }
    }
}
