import { ApplicationTrackingUpdateRequest, JobApplication } from '../../models/recruitment.model';

export type ExportExcelType = 'monthly' | 'crm';

type ExportGapFieldKey = keyof ApplicationTrackingUpdateRequest;

export interface ExportFieldGap {
  header: string;
  label: string;
  fieldKey: ExportGapFieldKey;
  inputType: 'text' | 'select-poste' | 'select-affectation' | 'date' | 'datetime' | 'textarea';
  placeholder?: string;
}

function firstNonBlank(...values: (string | null | undefined)[]): string {
  for (const v of values) {
    if (v != null && String(v).trim()) {
      return String(v).trim();
    }
  }
  return '';
}

function formatDate(value?: string | null): string {
  if (!value?.trim()) {
    return '';
  }
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    return '';
  }
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

function formatTime(value?: string | null): string {
  if (!value?.trim()) {
    return '';
  }
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    return '';
  }
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Mirrors CandidatesMonthlyExcelService.toRow — monthly export column values. */
function monthlyImf(app: JobApplication): string {
  return firstNonBlank(app.imf, app.diplomeEcole);
}

function monthlyHebergement(app: JobApplication): string {
  return firstNonBlank(app.hebergement);
}

function monthlyCommentaire(app: JobApplication): string {
  return firstNonBlank(app.commentairesRh, app.remarquesRh, app.observation);
}

function monthlyDateDebutPotentielle(app: JobApplication): string {
  return app.dateDebutPotentielle ? formatDate(app.dateDebutPotentielle) : '';
}

function resolveAgenceName(app: JobApplication): string {
  return firstNonBlank(app.affectation, app.companyName, app.zoneName);
}

function resolvePosteMonthly(app: JobApplication): string {
  return firstNonBlank(app.recruitmentTitle, app.profilMetier);
}

function resolvePosteCrm(app: JobApplication): string {
  return firstNonBlank(app.profilMetier, app.recruitmentTitle);
}

function resolveContrat(app: JobApplication): string {
  return firstNonBlank(app.dureeContrat, app.hireContractType, app.formatMission);
}

function resolvePretention(app: JobApplication): string {
  return firstNonBlank(app.pretention, app.hireNetSalary, app.salaireActuel, app.prixMois);
}

function resolveDateIntegration(app: JobApplication): string {
  return firstNonBlank(
    app.dateDebutMission ? formatDate(app.dateDebutMission) : '',
    app.hireStartDate ? formatDate(app.hireStartDate) : ''
  );
}

function resolveResponsable(app: JobApplication): string {
  return firstNonBlank(app.responsibleName, app.contactName);
}

/** Mirrors CandidatesMonthlyExcelService.toCrmRow — CRM export column values. */
function crmReference(app: JobApplication): string {
  return firstNonBlank(app.keejobReference, app.internalReference, app.codeDossier);
}

function crmAncienEmployeur(app: JobApplication): string {
  return firstNonBlank(app.situationPerso, app.commercialName, app.imf);
}

function crmDateFormation(app: JobApplication): string {
  return firstNonBlank(
    app.dateDebutMission ? formatDate(app.dateDebutMission) : '',
    app.hireStartDate ? formatDate(app.hireStartDate) : ''
  );
}

function crmCommentaireRh(app: JobApplication): string {
  return firstNonBlank(app.commentairesRh);
}

function crmCommentaireResp(app: JobApplication): string {
  return firstNonBlank(app.remarquesRh, app.observation);
}

function crmEntretienRespAt(app: JobApplication): string {
  if (app.entretienRespAt) {
    return `${formatDate(app.entretienRespAt)} ${formatTime(app.entretienRespAt)}`;
  }
  if (app.hiredAt) {
    return `${formatDate(app.hiredAt)} ${formatTime(app.hiredAt)}`;
  }
  return '';
}

interface GapRule {
  header: string;
  label: string;
  fieldKey: ExportGapFieldKey;
  inputType: ExportFieldGap['inputType'];
  placeholder?: string;
  exports: ExportExcelType[];
  resolve: (app: JobApplication) => string;
}

const GAP_RULES: GapRule[] = [
  // —— Export par mois ——
  {
    header: 'IMF',
    label: 'IMF',
    fieldKey: 'imf',
    inputType: 'text',
    placeholder: 'IMF',
    exports: ['monthly'],
    resolve: monthlyImf
  },
  {
    header: 'INTITULE DU POSTE',
    label: 'Intitulé du poste',
    fieldKey: 'profilMetier',
    inputType: 'select-poste',
    exports: ['monthly'],
    resolve: resolvePosteMonthly
  },
  {
    header: 'AFFECTATION',
    label: 'Affectation',
    fieldKey: 'affectation',
    inputType: 'select-affectation',
    exports: ['monthly'],
    resolve: resolveAgenceName
  },
  {
    header: 'HEBERGEMENT',
    label: 'Hébergement',
    fieldKey: 'hebergement',
    inputType: 'text',
    placeholder: 'Ex: Oui / Non / détails',
    exports: ['monthly', 'crm'],
    resolve: monthlyHebergement
  },
  {
    header: 'CONTRAT',
    label: 'Contrat',
    fieldKey: 'dureeContrat',
    inputType: 'text',
    placeholder: 'Ex: CDI, CIVP, 6 mois',
    exports: ['monthly'],
    resolve: resolveContrat
  },
  {
    header: 'COMMENTAIRE',
    label: 'Commentaire',
    fieldKey: 'commentairesRh',
    inputType: 'textarea',
    exports: ['monthly'],
    resolve: monthlyCommentaire
  },
  {
    header: "DATE D'INTEGRATION",
    label: "Date d'intégration",
    fieldKey: 'dateDebutMission',
    inputType: 'date',
    exports: ['monthly'],
    resolve: resolveDateIntegration
  },
  {
    header: 'PRETENTION',
    label: 'Prétention',
    fieldKey: 'pretention',
    inputType: 'text',
    placeholder: 'Ex: 1200 DT',
    exports: ['monthly'],
    resolve: resolvePretention
  },
  {
    header: 'DATE DE DEBUT POTENTIELLE',
    label: 'Date de début potentielle',
    fieldKey: 'dateDebutPotentielle',
    inputType: 'date',
    exports: ['monthly'],
    resolve: monthlyDateDebutPotentielle
  },
  {
    header: 'RESPONSABLE DE RECRUTEMENT',
    label: 'Responsable de recrutement',
    fieldKey: 'contactName',
    inputType: 'text',
    placeholder: 'Nom du responsable',
    exports: ['monthly'],
    resolve: resolveResponsable
  },
  // —— Export CRM complet ——
  {
    header: 'Référence',
    label: 'Référence',
    fieldKey: 'codeDossier',
    inputType: 'text',
    placeholder: 'Code dossier / référence',
    exports: ['crm'],
    resolve: crmReference
  },
  {
    header: 'Ancien Employeur',
    label: 'Ancien employeur',
    fieldKey: 'situationPerso',
    inputType: 'text',
    exports: ['crm'],
    resolve: crmAncienEmployeur
  },
  {
    header: 'Exp (ans)',
    label: 'Expérience (ans)',
    fieldKey: 'experienceYears',
    inputType: 'text',
    placeholder: 'Ex: 3',
    exports: ['crm'],
    resolve: (app) => firstNonBlank(app.experienceYears)
  },
  {
    header: 'Poste Ciblé',
    label: 'Poste ciblé',
    fieldKey: 'profilMetier',
    inputType: 'select-poste',
    exports: ['crm'],
    resolve: resolvePosteCrm
  },
  {
    header: 'Affectation / Agence',
    label: 'Affectation / agence',
    fieldKey: 'affectation',
    inputType: 'select-affectation',
    exports: ['crm'],
    resolve: resolveAgenceName
  },
  {
    header: 'Type Contrat',
    label: 'Type contrat',
    fieldKey: 'dureeContrat',
    inputType: 'text',
    placeholder: 'Ex: CDI, CIVP, 6 mois',
    exports: ['crm'],
    resolve: resolveContrat
  },
  {
    header: 'Prétention (DT)',
    label: 'Prétention (DT)',
    fieldKey: 'pretention',
    inputType: 'text',
    placeholder: 'Ex: 1200 DT',
    exports: ['crm'],
    resolve: resolvePretention
  },
  {
    header: 'Date Formation',
    label: 'Date formation',
    fieldKey: 'dateDebutMission',
    inputType: 'date',
    exports: ['crm'],
    resolve: crmDateFormation
  },
  {
    header: 'Commentaire RH',
    label: 'Commentaire RH',
    fieldKey: 'commentairesRh',
    inputType: 'textarea',
    exports: ['crm'],
    resolve: crmCommentaireRh
  },
  {
    header: 'Date Entretien Resp.',
    label: 'Date et heure entretien responsable',
    fieldKey: 'entretienRespAt',
    inputType: 'datetime',
    exports: ['crm'],
    resolve: crmEntretienRespAt
  },
  {
    header: 'Commentaire Resp.',
    label: 'Commentaire responsable',
    fieldKey: 'remarquesRh',
    inputType: 'textarea',
    exports: ['crm'],
    resolve: crmCommentaireResp
  }
];

export function detectExportGaps(app: JobApplication, exportType: ExportExcelType): ExportFieldGap[] {
  const seenHeaders = new Set<string>();
  const gaps: ExportFieldGap[] = [];

  for (const rule of GAP_RULES) {
    if (!rule.exports.includes(exportType)) {
      continue;
    }
    if (seenHeaders.has(rule.header)) {
      continue;
    }
    if (rule.resolve(app)) {
      continue;
    }

    seenHeaders.add(rule.header);
    gaps.push({
      header: rule.header,
      label: rule.label,
      fieldKey: rule.fieldKey,
      inputType: rule.inputType,
      placeholder: rule.placeholder
    });
  }

  return gaps;
}

export function exportPrepTitle(exportType: ExportExcelType): string {
  return exportType === 'monthly'
    ? 'Compléter l’export Excel (par mois)'
    : 'Compléter l’export Excel (CRM complet)';
}
