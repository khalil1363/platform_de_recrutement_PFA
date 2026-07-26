package com.daam.recruitment.enumeration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Fixed DAAM agency affectation list (référentiel AFFECTATION).
 */
public enum AgencyAffectation {
    AGENCE_ARIANA("AGENCE ARIANA"),
    AGENCE_BEJA("AGENCE BÉJA"),
    AGENCE_BEN_AROUS("AGENCE BEN AROUS"),
    AGENCE_BIZERTE("AGENCE BIZERTE"),
    AGENCE_GABES("AGENCE GABES"),
    AGENCE_GAFSA("AGENCE GAFSA"),
    AGENCE_JENDOUBA("AGENCE JENDOUBA"),
    AGENCE_KAIRQUAN("AGENCE KAIRQUAN"),
    AGENCE_KASSERINE("AGENCE KASSERINE"),
    AGENCE_KRAM("AGENCE KRAM"),
    AGENCE_MAHDIA("AGENCE MAHDIA"),
    AGENCE_MANNOUBA("AGENCE MANNOUBA"),
    AGENCE_MEDENINE("AGENCE MEDENINE"),
    AGENCE_MENZEL_TEMIM("AGENCE MENZEL TEMIM"),
    AGENCE_MONASTIR("AGENCE MONASTIR"),
    AGENCE_MONCEF_BEY("AGENCE MONCEF BEY"),
    AGENCE_NABEUL("AGENCE NABEUL"),
    AGENCE_SFAX_NORD("AGENCE SFAX NORD"),
    AGENCE_SFAX_SUD("AGENCE SFAX SUD"),
    AGENCE_SIDI_BOUZID("AGENCE SIDI BOUZID"),
    AGENCE_SOUSSE("AGENCE SOUSSE"),
    AGENCE_ZAGHOUAN("AGENCE ZAGHOUAN"),
    AGENCE_DJERBA("AGENCE DJERBA"),
    SIEGE_SOCIAL("SIÈGE SOCIAL"),
    AGENCE_LE_KEF("AGENCE LE KEF"),
    AGENCE_SILIANA("AGENCE SILIANA"),
    AGENCE_AKOUDA("AGENCE AKOUDA"),
    AGENCE_MOKNINE("AGENCE MOKNINE"),
    AGENCE_SOUSSE_2("AGENCE SOUSSE 2"),
    AGENCE_SFAX_3("AGENCE SFAX 3"),
    AGENCE_KEBILI("AGENCE KEBILI"),
    AGENCE_MNLHLA("AGENCE MNLHLA");

    private final String label;

    AgencyAffectation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(AgencyAffectation::getLabel).toList();
    }

    public static Optional<AgencyAffectation> fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        String normalized = label.trim();
        return Arrays.stream(values())
                .filter(a -> a.label.equalsIgnoreCase(normalized))
                .findFirst();
    }
}
