package com.daam.recruitment.enumeration;

public enum ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    /** Retenu (entretien / sélection) */
    ACCEPTED,
    /** Retenu et embauché */
    HIRED,
    /** Non retenu */
    REJECTED,
    /** Désisté */
    DESISTED
}
