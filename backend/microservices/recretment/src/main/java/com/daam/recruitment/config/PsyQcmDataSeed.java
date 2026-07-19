package com.daam.recruitment.config;

import com.daam.recruitment.entity.Qcm;
import com.daam.recruitment.entity.QcmQuestion;
import com.daam.recruitment.repository.QcmQuestionRepository;
import com.daam.recruitment.repository.QcmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the PSY Commercial / Chargé de crédit questionnaire once.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class PsyQcmDataSeed implements ApplicationRunner {

    public static final String PSY_QCM_ID = "psy-cc-commercial-v1";

    private final QcmRepository qcmRepository;
    private final QcmQuestionRepository qcmQuestionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (qcmRepository.findByQcmId(PSY_QCM_ID).isPresent()) {
            log.info("PSY CC QCM already present ({})", PSY_QCM_ID);
            return;
        }

        String rhUserId = qcmRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(Qcm::getCreatedByRhUserId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse("system-seed");

        Qcm qcm = qcmRepository.save(Qcm.builder()
                .qcmId(PSY_QCM_ID)
                .title("Test PSY — Profil Commercial / Chargé de crédit")
                .description("Questionnaire psychométrique post-embauche (18 compétences). "
                        + "Barème Likert 0–10 pour rapports PDF Talent Insight et export Excel Suivi PSY CC.")
                .createdByRhUserId(rhUserId)
                .build());

        List<QcmQuestion> questions = buildQuestions(qcm.getQcmId());
        qcmQuestionRepository.saveAll(questions);
        log.info("Seeded PSY CC QCM {} with {} questions", PSY_QCM_ID, questions.size());
    }

    private List<QcmQuestion> buildQuestions(String qcmId) {
        return List.of(
                q(qcmId, 0, "APPROCHE_CLIENT",
                        "Un prospect ne répond pas après un premier contact. Quelle est votre approche ?",
                        "J'attends qu'il me rappelle",
                        "Je laisse un message unique puis j'arrête",
                        "Je relance de façon structurée sous quelques jours",
                        "J'active plusieurs canaux (appel, visite, message) pour ouvrir le dialogue",
                        "D", 1, 3, 7, 10),
                q(qcmId, 1, "COMPREHENSION_BESOINS",
                        "En entretien, avant de proposer une offre de crédit, vous privilégiez :",
                        "Présenter immédiatement le produit phare",
                        "Lister les avantages concurrentiels",
                        "Poser quelques questions puis proposer",
                        "Clarifier besoins, capacité et contraintes avant toute proposition",
                        "D", 1, 3, 6, 10),
                q(qcmId, 2, "ARGUMENTATION",
                        "Le client objecte : « Votre taux est trop élevé ». Vous :",
                        "Acceptez sans discuter pour conclure vite",
                        "Baissez immédiatement le taux",
                        "Répondez avec un argument générique",
                        "Reformulez l'objection et argumentez valeur, risque et alternatives",
                        "D", 1, 3, 5, 10),
                q(qcmId, 3, "SATISFACTION_CLIENT",
                        "Après signature d'un dossier, votre suivi client est plutôt :",
                        "Aucun suivi sauf réclamation",
                        "Un seul appel de confirmation",
                        "Un suivi ponctuel si j'ai le temps",
                        "Un accompagnement régulier pour fidéliser et détecter de nouveaux besoins",
                        "D", 1, 3, 6, 10),
                q(qcmId, 4, "RELATIONNEL",
                        "Dans votre réseau professionnel de terrain, vous êtes plutôt :",
                        "Peu d'échanges hors cadre strict",
                        "Échanges limités au nécessaire",
                        "Bonne entente avec un cercle proche",
                        "Création facile de liens et d'opportunités relationnelles",
                        "D", 2, 4, 7, 10),
                q(qcmId, 5, "EXTRAVERSION",
                        "En réunion client avec plusieurs interlocuteurs, vous êtes :",
                        "Discret et observateur",
                        "Participatif seulement si sollicité",
                        "À l'aise pour prendre la parole",
                        "Moteur de la discussion, énergique et expressif",
                        "D", 2, 4, 7, 10),
                q(qcmId, 6, "PRISE_DECISION",
                        "Une décision commerciale urgente doit être prise sans toutes les infos. Vous :",
                        "Reportez systématiquement",
                        "Demandez toujours une validation hiérarchique avant d'agir",
                        "Décidez avec prudence après une analyse courte",
                        "Tranchez rapidement avec les infos disponibles et assumez",
                        "D", 1, 4, 7, 10),
                q(qcmId, 7, "PRISE_RECUL",
                        "Un conflit apparaît entre urgences commerciales et risque dossier. Vous :",
                        "Réagissez à chaud pour conclure",
                        "Parfois vous prenez du recul",
                        "Analysez avant d'agir",
                        "Prenez du recul, cartographiez le problème, puis agissez",
                        "D", 2, 4, 7, 10),
                q(qcmId, 8, "GESTION_STRESS",
                        "Sous forte pression d'objectifs mensuels, vous :",
                        "Vous déstabilisez facilement",
                        "Gardez le contrôle avec difficulté",
                        "Restez globalement stable",
                        "Restez calme, précis et efficace",
                        "D", 1, 4, 7, 10),
                q(qcmId, 9, "ORIENTATION_QUALITE",
                        "Pour la qualité d'un dossier de crédit, vous :",
                        "Livrez vite même avec des approximations",
                        "Faites l'essentiel sans tout vérifier",
                        "Contrôlez les points clés",
                        "Êtes très rigoureux sur conformité et exhaustivité",
                        "D", 1, 3, 6, 10),
                q(qcmId, 10, "GESTION_RISQUES",
                        "Un dossier présente des signaux de risque de remboursement. Vous :",
                        "Ignorez le signal pour conclure",
                        "Le signalez sans approfondir",
                        "Analysez les risques principaux",
                        "Évaluez finement le risque et proposez des garde-fous",
                        "D", 1, 3, 6, 10),
                q(qcmId, 11, "RIGUEUR_REGLES",
                        "Face aux procédures internes et à la conformité, vous :",
                        "Contournez souvent les règles",
                        "Les suivez seulement si contrôlé",
                        "Les respectez dans l'ensemble",
                        "Adhérez strictement aux standards et à la conformité",
                        "D", 1, 3, 7, 10),
                q(qcmId, 12, "INITIATIVE",
                        "Sans consigne précise pour une journée terrain, vous :",
                        "Attendez les instructions",
                        "Faites le minimum demandé",
                        "Proposez quelques actions",
                        "Prenez l'initiative et créez des opportunités",
                        "D", 1, 3, 6, 10),
                q(qcmId, 13, "INNOVATION",
                        "Pour améliorer votre approche commerciale, vous :",
                        "Reproduisez toujours la même méthode",
                        "Changez rarement",
                        "Testez parfois de nouvelles pratiques",
                        "Innovez régulièrement dans l'approche client",
                        "D", 1, 3, 6, 10),
                q(qcmId, 14, "PLANIFICATION",
                        "Votre organisation de tournée / pipeline commercial est :",
                        "Surtout improvisée",
                        "Partiellement planifiée",
                        "Planifiée sur la semaine",
                        "Anticipée, priorisée et suivie avec des indicateurs",
                        "D", 2, 4, 7, 10),
                q(qcmId, 15, "LEADERSHIP",
                        "Pour faire avancer un partenaire ou un comité de crédit, vous :",
                        "Laissez les autres décider",
                        "Influences peu",
                        "Convainquez sur certains points",
                        "Pilotez la dynamique et influencez positivement",
                        "D", 2, 4, 6, 10),
                q(qcmId, 16, "MENTORAT",
                        "Un collègue junior est en difficulté sur le terrain. Vous :",
                        "N'intervenez pas",
                        "Aidez seulement si on vous le demande",
                        "Donnez quelques conseils ponctuels",
                        "L'accompagnez activement pour le faire progresser",
                        "D", 1, 4, 6, 10),
                q(qcmId, 17, "IMPLICATION",
                        "Votre implication au travail se traduit par :",
                        "Un effort minimum",
                        "Une implication variable",
                        "Un engagement régulier",
                        "Un fort investissement personnel et une constance élevée",
                        "D", 2, 4, 7, 10)
        );
    }

    private static QcmQuestion q(
            String qcmId, int order, String dimension,
            String text, String a, String b, String c, String d,
            String correct, double sa, double sb, double sc, double sd) {
        return QcmQuestion.builder()
                .qcmId(qcmId)
                .orderIndex(order)
                .dimensionCode(dimension)
                .questionText(text)
                .optionA(a)
                .optionB(b)
                .optionC(c)
                .optionD(d)
                .correctOption(correct)
                .scoreA(sa)
                .scoreB(sb)
                .scoreC(sc)
                .scoreD(sd)
                .build();
    }
}
