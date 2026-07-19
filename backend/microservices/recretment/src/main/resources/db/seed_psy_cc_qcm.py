# -*- coding: utf-8 -*-
"""Seed PSY CC quiz into REC with proper UTF-8."""
import uuid
import mysql.connector

QCM_ID = "psy-cc-commercial-v1"

QUESTIONS = [
    ("APPROCHE_CLIENT",
     "Un prospect ne répond pas après un premier contact. Quelle est votre approche ?",
     "J'attends qu'il me rappelle",
     "Je laisse un message unique puis j'arrête",
     "Je relance de façon structurée sous quelques jours",
     "J'active plusieurs canaux (appel, visite, message) pour ouvrir le dialogue",
     "D", 1, 3, 7, 10),
    ("COMPREHENSION_BESOINS",
     "En entretien, avant de proposer une offre de crédit, vous privilégiez :",
     "Présenter immédiatement le produit phare",
     "Lister les avantages concurrentiels",
     "Poser quelques questions puis proposer",
     "Clarifier besoins, capacité et contraintes avant toute proposition",
     "D", 1, 3, 6, 10),
    ("ARGUMENTATION",
     "Le client objecte : « Votre taux est trop élevé ». Vous :",
     "Acceptez sans discuter pour conclure vite",
     "Baissez immédiatement le taux",
     "Répondez avec un argument générique",
     "Reformulez l'objection et argumentez valeur, risque et alternatives",
     "D", 1, 3, 5, 10),
    ("SATISFACTION_CLIENT",
     "Après signature d'un dossier, votre suivi client est plutôt :",
     "Aucun suivi sauf réclamation",
     "Un seul appel de confirmation",
     "Un suivi ponctuel si j'ai le temps",
     "Un accompagnement régulier pour fidéliser et détecter de nouveaux besoins",
     "D", 1, 3, 6, 10),
    ("RELATIONNEL",
     "Dans votre réseau professionnel de terrain, vous êtes plutôt :",
     "Peu d'échanges hors cadre strict",
     "Échanges limités au nécessaire",
     "Bonne entente avec un cercle proche",
     "Création facile de liens et d'opportunités relationnelles",
     "D", 2, 4, 7, 10),
    ("EXTRAVERSION",
     "En réunion client avec plusieurs interlocuteurs, vous êtes :",
     "Discret et observateur",
     "Participatif seulement si sollicité",
     "À l'aise pour prendre la parole",
     "Moteur de la discussion, énergique et expressif",
     "D", 2, 4, 7, 10),
    ("PRISE_DECISION",
     "Une décision commerciale urgente doit être prise sans toutes les infos. Vous :",
     "Reportez systématiquement",
     "Demandez toujours une validation hiérarchique avant d'agir",
     "Décidez avec prudence après une analyse courte",
     "Tranchez rapidement avec les infos disponibles et assumez",
     "D", 1, 4, 7, 10),
    ("PRISE_RECUL",
     "Un conflit apparaît entre urgences commerciales et risque dossier. Vous :",
     "Réagissez à chaud pour conclure",
     "Parfois vous prenez du recul",
     "Analysez avant d'agir",
     "Prenez du recul, cartographiez le problème, puis agissez",
     "D", 2, 4, 7, 10),
    ("GESTION_STRESS",
     "Sous forte pression d'objectifs mensuels, vous :",
     "Vous déstabilisez facilement",
     "Gardez le contrôle avec difficulté",
     "Restez globalement stable",
     "Restez calme, précis et efficace",
     "D", 1, 4, 7, 10),
    ("ORIENTATION_QUALITE",
     "Pour la qualité d'un dossier de crédit, vous :",
     "Livrez vite même avec des approximations",
     "Faites l'essentiel sans tout vérifier",
     "Contrôlez les points clés",
     "Êtes très rigoureux sur conformité et exhaustivité",
     "D", 1, 3, 6, 10),
    ("GESTION_RISQUES",
     "Un dossier présente des signaux de risque de remboursement. Vous :",
     "Ignorez le signal pour conclure",
     "Le signalez sans approfondir",
     "Analysez les risques principaux",
     "Évaluez finement le risque et proposez des garde-fous",
     "D", 1, 3, 6, 10),
    ("RIGUEUR_REGLES",
     "Face aux procédures internes et à la conformité, vous :",
     "Contournez souvent les règles",
     "Les suivez seulement si contrôlé",
     "Les respectez dans l'ensemble",
     "Adhérez strictement aux standards et à la conformité",
     "D", 1, 3, 7, 10),
    ("INITIATIVE",
     "Sans consigne précise pour une journée terrain, vous :",
     "Attendez les instructions",
     "Faites le minimum demandé",
     "Proposez quelques actions",
     "Prenez l'initiative et créez des opportunités",
     "D", 1, 3, 6, 10),
    ("INNOVATION",
     "Pour améliorer votre approche commerciale, vous :",
     "Reproduisez toujours la même méthode",
     "Changez rarement",
     "Testez parfois de nouvelles pratiques",
     "Innovez régulièrement dans l'approche client",
     "D", 1, 3, 6, 10),
    ("PLANIFICATION",
     "Votre organisation de tournée / pipeline commercial est :",
     "Surtout improvisée",
     "Partiellement planifiée",
     "Planifiée sur la semaine",
     "Anticipée, priorisée et suivie avec des indicateurs",
     "D", 2, 4, 7, 10),
    ("LEADERSHIP",
     "Pour faire avancer un partenaire ou un comité de crédit, vous :",
     "Laissez les autres décider",
     "Influences peu",
     "Convainquez sur certains points",
     "Pilotez la dynamique et influencez positivement",
     "D", 2, 4, 6, 10),
    ("MENTORAT",
     "Un collègue junior est en difficulté sur le terrain. Vous :",
     "N'intervenez pas",
     "Aidez seulement si on vous le demande",
     "Donnez quelques conseils ponctuels",
     "L'accompagnez activement pour le faire progresser",
     "D", 1, 4, 6, 10),
    ("IMPLICATION",
     "Votre implication au travail se traduit par :",
     "Un effort minimum",
     "Une implication variable",
     "Un engagement régulier",
     "Un fort investissement personnel et une constance élevée",
     "D", 2, 4, 7, 10),
]


def ensure_columns(cur):
    cur.execute("SHOW COLUMNS FROM qcm_questions")
    cols = {r[0] for r in cur.fetchall()}
    alters = []
    if "dimension_code" not in cols:
        alters.append("ADD COLUMN dimension_code VARCHAR(255) NULL")
    if "score_a" not in cols:
        alters.append("ADD COLUMN score_a DOUBLE NULL")
    if "score_b" not in cols:
        alters.append("ADD COLUMN score_b DOUBLE NULL")
    if "score_c" not in cols:
        alters.append("ADD COLUMN score_c DOUBLE NULL")
    if "score_d" not in cols:
        alters.append("ADD COLUMN score_d DOUBLE NULL")
    if alters:
        cur.execute("ALTER TABLE qcm_questions " + ", ".join(alters))


def main():
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="000000",
        database="REC",
        charset="utf8mb4",
        use_unicode=True,
    )
    cur = conn.cursor()
    ensure_columns(cur)

    cur.execute("SELECT created_by_rh_user_id FROM qcms ORDER BY id ASC LIMIT 1")
    row = cur.fetchone()
    rh_id = row[0] if row and row[0] else "system-seed"

    cur.execute("DELETE FROM qcm_questions WHERE qcm_id = %s", (QCM_ID,))
    cur.execute("DELETE FROM qcms WHERE qcm_id = %s", (QCM_ID,))

    cur.execute(
        """
        INSERT INTO qcms (qcm_id, title, description, created_by_rh_user_id, created_at, updated_at)
        VALUES (%s, %s, %s, %s, NOW(6), NOW(6))
        """,
        (
            QCM_ID,
            "Test PSY — Profil Commercial / Chargé de crédit",
            "Questionnaire psychométrique post-embauche (18 compétences). "
            "Barème Likert 0–10 pour rapports PDF Talent Insight et export Excel Suivi PSY CC.",
            rh_id,
        ),
    )

    insert_sql = """
        INSERT INTO qcm_questions
        (question_id, qcm_id, question_text, optiona, optionb, optionc, optiond,
         correct_option, order_index, dimension_code, score_a, score_b, score_c, score_d)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
    """
    for i, (dim, text, a, b, c, d, correct, sa, sb, sc, sd) in enumerate(QUESTIONS):
        cur.execute(
            insert_sql,
            (str(uuid.uuid4()), QCM_ID, text, a, b, c, d, correct, i, dim, sa, sb, sc, sd),
        )

    conn.commit()
    cur.execute(
        "SELECT title, (SELECT COUNT(*) FROM qcm_questions WHERE qcm_id=%s) FROM qcms WHERE qcm_id=%s",
        (QCM_ID, QCM_ID),
    )
    title, count = cur.fetchone()
    print(f"OK: {title} | {count} questions")
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
