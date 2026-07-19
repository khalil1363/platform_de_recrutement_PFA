package com.daam.recruitment.psychometric;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Competency catalog aligned with "Suivi Test PSY CC" Excel (Commercial / Chargé de crédit).
 */
public final class PsychometricDimensions {

    private PsychometricDimensions() {}

    public record DimensionDef(
            String code,
            String label,
            double expectedScore,
            String commentHigh,
            String commentMid,
            String commentLow
    ) {}

    private static final Map<String, DimensionDef> BY_CODE = new LinkedHashMap<>();

    static {
        add("APPROCHE_CLIENT", "Approche client", 0.85,
                "À l'aise pour initier le contact, spontané et relationnel",
                "Contact client correct, confiance encore à renforcer",
                "Difficulté à initier le contact, doit gagner en confiance");
        add("COMPREHENSION_BESOINS", "Compréhension des besoins", 0.80,
                "Bonne écoute, analyse pertinente des besoins clients",
                "Écoute partielle, analyse des besoins à consolider",
                "Écoute insuffisante, analyse des besoins à renforcer");
        add("ARGUMENTATION", "Argumentation", 0.85,
                "Capable de convaincre avec un discours structuré et impactant",
                "Capable de convaincre mais peut être plus incisif",
                "Faible capacité à convaincre, discours peu adapté aux objections");
        add("SATISFACTION_CLIENT", "Satisfaction client", 0.85,
                "Très orienté client, suivi et fidélisation solides",
                "Orientation client présente, fidélisation à améliorer",
                "Suivi client limité, peu orienté fidélisation");
        add("RELATIONNEL", "Relationnel", 0.80,
                "Excellent relationnel, crée facilement du lien",
                "Bon relationnel, à améliorer pour le réseautage",
                "Relation distante, difficulté à créer du lien");
        add("EXTRAVERSION", "Extraversion", 0.80,
                "Très sociable, à l'aise avec les clients",
                "Sociable, sait capter l'attention",
                "Réservé, moins à l'aise en interaction commerciale");
        add("PRISE_DECISION", "Prise de décision", 0.75,
                "Décide avec assurance dans les négociations",
                "Décisions rationnelles mais prudentes",
                "Hésitation sur décisions commerciales importantes");
        add("PRISE_RECUL", "Prise de recul", 0.70,
                "Bonne capacité d'analyse globale et stratégique",
                "Prise de recul correcte, vision à élargir",
                "Vision stratégique limitée");
        add("GESTION_STRESS", "Gestion du stress", 0.75,
                "Très bonne stabilité émotionnelle sous pression",
                "Bonne résistance, supporte la pression",
                "Sensibilité au stress, maîtrise émotionnelle à renforcer");
        add("ORIENTATION_QUALITE", "Orientation qualité", 0.70,
                "Très rigoureux, assure la qualité des dossiers",
                "Niveau de rigueur correct",
                "Manque de rigueur constante");
        add("GESTION_RISQUES", "Gestion des risques", 0.75,
                "Bon sens du contrôle, pertinent pour le crédit",
                "Prudence modérée, contrôle des risques à affiner",
                "Sens du risque insuffisant pour un rôle crédit");
        add("RIGUEUR_REGLES", "Rigueur / règles", 0.80,
                "Très structuré, conformité respectée",
                "Respecte les procédures, cadre structurant",
                "Respect des règles inégal, structure à renforcer");
        add("INITIATIVE", "Initiative", 0.80,
                "Très motivé et compétitif, aime relever les défis",
                "Initiative correcte, proactivité à développer",
                "Manque de proactivité, frein commercial");
        add("INNOVATION", "Innovation", 0.60,
                "Sait adapter son approche commerciale",
                "Créativité limitée, méthodes surtout classiques",
                "Peu créatif, préfère méthodes classiques");
        add("PLANIFICATION", "Planification", 0.70,
                "Bonne organisation et anticipation",
                "Organisation correcte, doit anticiper davantage",
                "Organisation moyenne, anticipation insuffisante");
        add("LEADERSHIP", "Leadership", 0.75,
                "Sait influencer et piloter la relation client",
                "Leadership en développement",
                "Capacité moyenne à influencer et piloter");
        add("MENTORAT", "Mentorat", 0.70,
                "Bon accompagnement, peut aider les collègues",
                "Peut accompagner, impact encore limité",
                "Accompagnement limité, peu de développement des autres");
        add("IMPLICATION", "Implication au travail", 0.80,
                "Très engagé, fort investissement personnel",
                "Bon engagement global",
                "Engagement correct mais pas constant");
    }

    private static void add(String code, String label, double expected,
                            String high, String mid, String low) {
        BY_CODE.put(code, new DimensionDef(code, label, expected, high, mid, low));
    }

    public static List<DimensionDef> all() {
        return List.copyOf(BY_CODE.values());
    }

    public static DimensionDef get(String code) {
        if (code == null) return null;
        return BY_CODE.get(code.trim().toUpperCase());
    }

    public static String commentFor(DimensionDef def, double score) {
        double expected = def.expectedScore();
        if (score >= expected) return def.commentHigh();
        if (score >= expected - 0.15) return def.commentMid();
        return def.commentLow();
    }

    public static String globalComment(double score, double expected) {
        if (score >= expected + 0.05) {
            return "Excellent fit global pour un commercial terrain / chargé de crédit";
        }
        if (score >= expected) {
            return "Bon potentiel global pour poste de chargé de crédit / vente B2B structurée";
        }
        if (score >= expected - 0.15) {
            return "Bon fit partiel pour un poste managérial structuré, profil à encadrer";
        }
        return "Potentiel commercial modéré, profil plus adapté à un rôle structuré qu'à un commercial pur";
    }
}
