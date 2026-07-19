USE REC;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE qcm_questions ADD COLUMN dimension_code VARCHAR(255) NULL',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'REC' AND TABLE_NAME = 'qcm_questions' AND COLUMN_NAME = 'dimension_code'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE qcm_questions ADD COLUMN score_a DOUBLE NULL',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'REC' AND TABLE_NAME = 'qcm_questions' AND COLUMN_NAME = 'score_a'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE qcm_questions ADD COLUMN score_b DOUBLE NULL',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'REC' AND TABLE_NAME = 'qcm_questions' AND COLUMN_NAME = 'score_b'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE qcm_questions ADD COLUMN score_c DOUBLE NULL',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'REC' AND TABLE_NAME = 'qcm_questions' AND COLUMN_NAME = 'score_c'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE qcm_questions ADD COLUMN score_d DOUBLE NULL',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'REC' AND TABLE_NAME = 'qcm_questions' AND COLUMN_NAME = 'score_d'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @qcm_id := 'psy-cc-commercial-v1';
SET @rh_id := (SELECT created_by_rh_user_id FROM qcms ORDER BY id ASC LIMIT 1);
SET @rh_id := IFNULL(@rh_id, 'system-seed');

DELETE FROM qcm_questions WHERE qcm_id = @qcm_id;
DELETE FROM qcms WHERE qcm_id = @qcm_id;

INSERT INTO qcms (qcm_id, title, description, created_by_rh_user_id, created_at, updated_at)
VALUES (
  @qcm_id,
  'Test PSY — Profil Commercial / Chargé de crédit',
  'Questionnaire psychométrique post-embauche (18 compétences). Barème Likert 0–10 pour rapports PDF et export Excel.',
  @rh_id,
  NOW(6),
  NOW(6)
);

INSERT INTO qcm_questions
(question_id, qcm_id, question_text, optiona, optionb, optionc, optiond, correct_option, order_index, dimension_code, score_a, score_b, score_c, score_d)
VALUES
(UUID(), @qcm_id, 'Un prospect ne répond pas après un premier contact. Quelle est votre approche ?',
 'J''attends qu''il me rappelle', 'Je laisse un message unique puis j''arrête',
 'Je relance de façon structurée sous quelques jours',
 'J''active plusieurs canaux (appel, visite, message) pour ouvrir le dialogue',
 'D', 0, 'APPROCHE_CLIENT', 1, 3, 7, 10),
(UUID(), @qcm_id, 'En entretien, avant de proposer une offre de crédit, vous privilégiez :',
 'Présenter immédiatement le produit phare', 'Lister les avantages concurrentiels',
 'Poser quelques questions puis proposer',
 'Clarifier besoins, capacité et contraintes avant toute proposition',
 'D', 1, 'COMPREHENSION_BESOINS', 1, 3, 6, 10),
(UUID(), @qcm_id, 'Le client objecte : Votre taux est trop élevé. Vous :',
 'Acceptez sans discuter pour conclure vite', 'Baissez immédiatement le taux',
 'Répondez avec un argument générique',
 'Reformulez l''objection et argumentez valeur, risque et alternatives',
 'D', 2, 'ARGUMENTATION', 1, 3, 5, 10),
(UUID(), @qcm_id, 'Après signature d''un dossier, votre suivi client est plutôt :',
 'Aucun suivi sauf réclamation', 'Un seul appel de confirmation',
 'Un suivi ponctuel si j''ai le temps',
 'Un accompagnement régulier pour fidéliser et détecter de nouveaux besoins',
 'D', 3, 'SATISFACTION_CLIENT', 1, 3, 6, 10),
(UUID(), @qcm_id, 'Dans votre réseau professionnel de terrain, vous êtes plutôt :',
 'Peu d''échanges hors cadre strict', 'Échanges limités au nécessaire',
 'Bonne entente avec un cercle proche',
 'Création facile de liens et d''opportunités relationnelles',
 'D', 4, 'RELATIONNEL', 2, 4, 7, 10),
(UUID(), @qcm_id, 'En réunion client avec plusieurs interlocuteurs, vous êtes :',
 'Discret et observateur', 'Participatif seulement si sollicité',
 'À l''aise pour prendre la parole',
 'Moteur de la discussion, énergique et expressif',
 'D', 5, 'EXTRAVERSION', 2, 4, 7, 10),
(UUID(), @qcm_id, 'Une décision commerciale urgente doit être prise sans toutes les infos. Vous :',
 'Reportez systématiquement', 'Demandez toujours une validation hiérarchique avant d''agir',
 'Décidez avec prudence après une analyse courte',
 'Tranchez rapidement avec les infos disponibles et assumez',
 'D', 6, 'PRISE_DECISION', 1, 4, 7, 10),
(UUID(), @qcm_id, 'Un conflit apparaît entre urgences commerciales et risque dossier. Vous :',
 'Réagissez à chaud pour conclure', 'Parfois vous prenez du recul',
 'Analysez avant d''agir',
 'Prenez du recul, cartographiez le problème, puis agissez',
 'D', 7, 'PRISE_RECUL', 2, 4, 7, 10),
(UUID(), @qcm_id, 'Sous forte pression d''objectifs mensuels, vous :',
 'Vous déstabilisez facilement', 'Gardez le contrôle avec difficulté',
 'Restez globalement stable',
 'Restez calme, précis et efficace',
 'D', 8, 'GESTION_STRESS', 1, 4, 7, 10),
(UUID(), @qcm_id, 'Pour la qualité d''un dossier de crédit, vous :',
 'Livrez vite même avec des approximations', 'Faites l''essentiel sans tout vérifier',
 'Contrôlez les points clés',
 'Êtes très rigoureux sur conformité et exhaustivité',
 'D', 9, 'ORIENTATION_QUALITE', 1, 3, 6, 10),
(UUID(), @qcm_id, 'Un dossier présente des signaux de risque de remboursement. Vous :',
 'Ignorez le signal pour conclure', 'Le signalez sans approfondir',
 'Analysez les risques principaux',
 'Évaluez finement le risque et proposez des garde-fous',
 'D', 10, 'GESTION_RISQUES', 1, 3, 6, 10),
(UUID(), @qcm_id, 'Face aux procédures internes et à la conformité, vous :',
 'Contournez souvent les règles', 'Les suivez seulement si contrôlé',
 'Les respectez dans l''ensemble',
 'Adhérez strictement aux standards et à la conformité',
 'D', 11, 'RIGUEUR_REGLES', 1, 3, 7, 10),
(UUID(), @qcm_id, 'Sans consigne précise pour une journée terrain, vous :',
 'Attendez les instructions', 'Faites le minimum demandé',
 'Proposez quelques actions',
 'Prenez l''initiative et créez des opportunités',
 'D', 12, 'INITIATIVE', 1, 3, 6, 10),
(UUID(), @qcm_id, 'Pour améliorer votre approche commerciale, vous :',
 'Reproduisez toujours la même méthode', 'Changez rarement',
 'Testez parfois de nouvelles pratiques',
 'Innovez régulièrement dans l''approche client',
 'D', 13, 'INNOVATION', 1, 3, 6, 10),
(UUID(), @qcm_id, 'Votre organisation de tournée / pipeline commercial est :',
 'Surtout improvisée', 'Partiellement planifiée',
 'Planifiée sur la semaine',
 'Anticipée, priorisée et suivie avec des indicateurs',
 'D', 14, 'PLANIFICATION', 2, 4, 7, 10),
(UUID(), @qcm_id, 'Pour faire avancer un partenaire ou un comité de crédit, vous :',
 'Laissez les autres décider', 'Influences peu',
 'Convainquez sur certains points',
 'Pilotez la dynamique et influencez positivement',
 'D', 15, 'LEADERSHIP', 2, 4, 6, 10),
(UUID(), @qcm_id, 'Un collègue junior est en difficulté sur le terrain. Vous :',
 'N''intervenez pas', 'Aidez seulement si on vous le demande',
 'Donnez quelques conseils ponctuels',
 'L''accompagnez activement pour le faire progresser',
 'D', 16, 'MENTORAT', 1, 4, 6, 10),
(UUID(), @qcm_id, 'Votre implication au travail se traduit par :',
 'Un effort minimum', 'Une implication variable',
 'Un engagement régulier',
 'Un fort investissement personnel et une constance élevée',
 'D', 17, 'IMPLICATION', 2, 4, 7, 10);

SELECT q.qcm_id, q.title, COUNT(qq.id) AS questions
FROM qcms q
LEFT JOIN qcm_questions qq ON qq.qcm_id = q.qcm_id
WHERE q.qcm_id = 'psy-cc-commercial-v1'
GROUP BY q.qcm_id, q.title;
