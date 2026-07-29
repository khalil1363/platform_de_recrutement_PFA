import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { ApplicationStatus, ApplicationTrackingUpdateRequest, JobApplication } from '../../models/recruitment.model';
import { AFFECTATIONS } from '../../constants/affectations';
import { JOB_TITLES } from '../../constants/job-titles';
import {
  detectExportGaps,
  ExportExcelType,
  ExportFieldGap,
  exportPrepTitle
} from './export-excel-gaps.util';

interface ExportPrepFormEntry {
  applicationId: string;
  candidateName: string;
  recruitmentTitle: string;
  gaps: ExportFieldGap[];
  values: Record<string, string | Date | null>;
  saving: boolean;
}

interface RecruitmentApplicationsGroup {
  recruitmentId: string;
  recruitmentTitle: string;
  zoneName?: string;
  region?: string;
  applications: JobApplication[];
}

@Component({
  selector: 'app-rh-candidates',
  templateUrl: './rh-candidates.component.html',
  styleUrl: './rh-candidates.component.css'
})
export class RhCandidatesComponent implements OnInit, OnDestroy {
  applications: JobApplication[] = [];
  groups: RecruitmentApplicationsGroup[] = [];
  loading = false;
  selectedApplication: JobApplication | null = null;
  detailVisible = false;
  interviewModalVisible = false;
  hireModalVisible = false;
  actionLoading = false;
  interviewForm!: FormGroup;
  hireForm!: FormGroup;
  pendingApplication: JobApplication | null = null;
  pendingInterviewType: 'ONLINE' | 'PHYSICAL' = 'ONLINE';
  cvPreviewVisible = false;
  cvPreviewLoading = false;
  cvPreviewSrc: SafeResourceUrl | null = null;
  cvObjectUrl: string | null = null;
  analyzeLoading = false;
  exportLoading = false;
  exportFullLoading = false;
  affectations: string[] = [...AFFECTATIONS];
  jobTitles: string[] = [...JOB_TITLES];
  hireContractTypes: string[] = ['CVP', 'CDI'];

  filterFirstName = '';
  filterLastName = '';
  filterCin = '';

  historyVisible = false;
  historyLoading = false;
  historyApplications: JobApplication[] = [];
  historyCandidate: JobApplication | null = null;
  historyExpandedId: string | null = null;

  exportPrepVisible = false;
  exportPrepType: ExportExcelType = 'monthly';
  exportPrepEntry: ExportPrepFormEntry | null = null;
  qcmDetailsExpanded = false;

  constructor(
    readonly authService: AuthService,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService,
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly sanitizer: DomSanitizer,
    private readonly http: HttpClient
  ) {}

  ngOnInit(): void {
    this.interviewForm = this.fb.group({
      interviewDate: [null, Validators.required],
      startTime: [null, Validators.required],
      endTime: [null, Validators.required],
      interviewLocation: ['']
    });
    this.hireForm = this.fb.group({
      hireStartDate: [null, Validators.required],
      hireNetSalary: ['', Validators.required],
      hireContractType: [null, Validators.required],
      hireWorkingHours: [
        '08 heures par jour, du lundi au vendredi de 8h à 17h30, avec permanence le samedi de fin de mois de 08h00 à 12h00'
      ],
      hireBenefits: [
        `Prime de performance selon les résultats réalisés ;
Une allocation de 105 DT par mois, à partir de trois (3) dossiers déboursés minimum et jusqu’à 100 000 DT d’encours ;
Prime de portefeuille mensuelle calculée selon l’évolution du portefeuille, conformément aux dix (10) paliers définis ;
Tickets restaurant d’une valeur mensuelle de 170 DT ;
Assurance groupe avec un plafond annuel de remboursement fixé à 6 500 DT.`
      ]
    });
    this.loadReferentials();
    this.loadApplications();
  }

  loadReferentials(): void {
    this.recruitmentService.getAffectations().subscribe({
      next: (response) => {
        if (response.success && response.data?.length) {
          this.affectations = response.data;
        }
      }
    });
    this.recruitmentService.getJobTitles().subscribe({
      next: (response) => {
        if (response.success && response.data?.length) {
          this.jobTitles = response.data;
        }
      }
    });
  }

  get interviewModalTitle(): string {
    return this.pendingInterviewType === 'PHYSICAL'
      ? "Planifier l'entretien physique"
      : "Planifier l'entretien en ligne";
  }

  get historyModalTitle(): string {
    const c = this.historyCandidate?.candidate;
    if (!c) return 'Historique des candidatures';
    return `Historique — ${c.firstName} ${c.lastName}`;
  }

  ngOnDestroy(): void {
    this.revokeCvObjectUrl();
  }

  loadApplications(): void {
    this.loading = true;
    this.recruitmentService.getRhApplications().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.applications = response.data;
          this.rebuildGroups();
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des candidatures');
      }
    });
  }

  openDetails(app: JobApplication): void {
    this.selectedApplication = app;
    this.hideCvPreview();
    this.qcmDetailsExpanded = false;
    this.ensureReferentialOptions(app);
    this.detailVisible = true;
  }

  toggleQcmDetails(): void {
    this.qcmDetailsExpanded = !this.qcmDetailsExpanded;
  }

  qcmScoreLabel(app: JobApplication): string {
    const score = app.qcmScore ?? 0;
    const total = app.qcmTotalQuestions ?? 0;
    if (!total) {
      return 'Aucune réponse';
    }
    const pct = Math.round((score / total) * 100);
    return `${score}/${total} (${pct}%)`;
  }

  qcmScoreColor(app: JobApplication): string {
    const score = app.qcmScore ?? 0;
    const total = app.qcmTotalQuestions ?? 1;
    const pct = (score / total) * 100;
    if (pct >= 70) return 'success';
    if (pct >= 50) return 'processing';
    return 'error';
  }

  private ensureReferentialOptions(app: JobApplication): void {
    const affectation = app.affectation || app.companyName || app.zoneName || '';
    if (affectation && !this.affectations.includes(affectation)) {
      this.affectations = [affectation, ...this.affectations];
    }
    const poste = app.profilMetier || app.recruitmentTitle || '';
    if (poste && !this.jobTitles.includes(poste)) {
      this.jobTitles = [poste, ...this.jobTitles];
    }
  }

  openHistory(app: JobApplication): void {
    this.historyCandidate = app;
    this.historyVisible = true;
    this.historyLoading = true;
    this.historyApplications = [];
    this.historyExpandedId = null;

    this.recruitmentService.getCandidateApplicationHistory(app.candidateUserId).subscribe({
      next: (response) => {
        this.historyLoading = false;
        if (response.success && response.data) {
          this.historyApplications = response.data;
          const others = response.data.filter((a) => a.applicationId !== app.applicationId);
          if (others.length > 0) {
            this.historyExpandedId = others[0].applicationId;
          } else if (response.data.length === 1) {
            this.historyExpandedId = response.data[0].applicationId;
          }
        }
      },
      error: () => {
        this.historyLoading = false;
        this.message.error("Impossible de charger l'historique du candidat");
      }
    });
  }

  closeHistory(): void {
    this.historyVisible = false;
    this.historyCandidate = null;
    this.historyApplications = [];
    this.historyExpandedId = null;
  }

  toggleHistoryPanel(applicationId: string): void {
    this.historyExpandedId = this.historyExpandedId === applicationId ? null : applicationId;
  }

  previousApplicationsCount(): number {
    if (!this.historyCandidate) {
      return 0;
    }
    return this.historyApplications.filter(
      (a) => a.applicationId !== this.historyCandidate!.applicationId
    ).length;
  }

  interviewTypeLabel(app: JobApplication): string {
    if (app.meetingProvider === 'PHYSICAL') return 'Entretien physique';
    if (app.googleMeetLink || app.meetingProvider) return 'Entretien en ligne';
    return '—';
  }

  exportMonthlyExcel(): void {
    this.runExportDownload('monthly');
  }

  exportFullExcel(): void {
    this.runExportDownload('crm');
  }

  detailGapCount(type: ExportExcelType): number {
    if (!this.selectedApplication) {
      return 0;
    }
    return detectExportGaps(this.selectedApplication, type).length;
  }

  get exportPrepModalTitle(): string {
    return exportPrepTitle(this.exportPrepType);
  }

  openDetailExportPrep(type: ExportExcelType): void {
    if (!this.selectedApplication) {
      return;
    }
    const gaps = detectExportGaps(this.selectedApplication, type);
    if (gaps.length === 0) {
      this.message.success('Toutes les colonnes Excel sont déjà remplies pour cet export.');
      return;
    }
    const c = this.selectedApplication.candidate;
    const candidateName = c
      ? `${c.firstName || ''} ${c.lastName || ''}`.trim() || 'Candidat'
      : 'Candidat';

    this.exportPrepType = type;
    this.exportPrepEntry = {
      applicationId: this.selectedApplication.applicationId,
      candidateName,
      recruitmentTitle: this.selectedApplication.recruitmentTitle || '—',
      gaps,
      values: this.emptyValuesForGaps(gaps),
      saving: false
    };
    this.exportPrepVisible = true;
  }

  closeExportPrep(): void {
    this.exportPrepVisible = false;
    this.exportPrepEntry = null;
  }

  exportPrepFilledCount(): number {
    if (!this.exportPrepEntry) {
      return 0;
    }
    return this.exportPrepEntry.gaps.filter((g) =>
      this.hasExportPrepValue(this.exportPrepEntry!, g.fieldKey)
    ).length;
  }

  hasExportPrepValue(entry: ExportPrepFormEntry, fieldKey: string): boolean {
    const v = entry.values[fieldKey];
    if (v == null) {
      return false;
    }
    if (v instanceof Date) {
      return !Number.isNaN(v.getTime());
    }
    return String(v).trim().length > 0;
  }

  saveDetailExportFiche(): void {
    if (!this.exportPrepEntry) {
      return;
    }
    const entry = this.exportPrepEntry;
    const request = this.buildTrackingRequestFromPrep(entry);
    if (Object.keys(request).length === 0) {
      this.message.warning('Renseignez au moins un champ avant d’enregistrer');
      return;
    }
    entry.saving = true;
    this.recruitmentService.updateApplicationTracking(entry.applicationId, request).subscribe({
      next: (response) => {
        entry.saving = false;
        if (response.success && response.data) {
          this.applications = this.applications.map((a) =>
            a.applicationId === response.data!.applicationId ? response.data! : a
          );
          this.rebuildGroups();
          if (this.selectedApplication?.applicationId === response.data.applicationId) {
            this.selectedApplication = response.data;
          }
          this.message.success('Fiche enregistrée — incluse dans l’export Excel');
          this.closeExportPrep();
        }
      },
      error: (err) => {
        entry.saving = false;
        this.message.error(err.error?.message || 'Erreur lors de l’enregistrement');
      }
    });
  }

  private runExportDownload(type: ExportExcelType): void {
    if (type === 'monthly') {
      this.exportLoading = true;
      this.recruitmentService.exportCandidatesMonthlyExcel().subscribe({
        next: (blob) => {
          this.exportLoading = false;
          this.downloadBlob(blob, 'daam-candidats-par-mois.xlsx');
          this.message.success('Export Excel téléchargé (une feuille par mois)');
        },
        error: () => {
          this.exportLoading = false;
          this.message.error('Erreur lors de l\'export Excel');
        }
      });
      return;
    }
    this.exportFullLoading = true;
    this.recruitmentService.exportCandidatesFullExcel().subscribe({
      next: (blob) => {
        this.exportFullLoading = false;
        this.downloadBlob(blob, 'daam-candidats-complet.xlsx');
        this.message.success('Export Excel complet téléchargé (Suivi CRM + Recrutements Agences)');
      },
      error: () => {
        this.exportFullLoading = false;
        this.message.error('Erreur lors de l\'export Excel complet');
      }
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  private emptyValuesForGaps(gaps: ExportFieldGap[]): Record<string, string | Date | null> {
    const values: Record<string, string | Date | null> = {};
    for (const gap of gaps) {
      values[gap.fieldKey] =
        gap.inputType === 'date' || gap.inputType === 'datetime' ? null : '';
    }
    return values;
  }

  private buildTrackingRequestFromPrep(entry: ExportPrepFormEntry): ApplicationTrackingUpdateRequest {
    const request: ApplicationTrackingUpdateRequest = {};
    for (const gap of entry.gaps) {
      const raw = entry.values[gap.fieldKey];
      if (raw == null || (typeof raw === 'string' && !raw.trim())) {
        continue;
      }
      if (gap.inputType === 'date') {
        const date = raw instanceof Date ? raw : new Date(String(raw));
        if (!Number.isNaN(date.getTime())) {
          (request as Record<string, string | null>)[gap.fieldKey] = this.formatLocalDate(date);
        }
      } else if (gap.inputType === 'datetime') {
        const date = raw instanceof Date ? raw : new Date(String(raw));
        if (!Number.isNaN(date.getTime())) {
          request.entretienRespAt = this.formatLocalDateTime(date);
        }
      } else {
        (request as Record<string, string | null>)[gap.fieldKey] = String(raw).trim();
      }
    }
    return request;
  }

  toggleCvPreview(): void {
    if (this.cvPreviewVisible) {
      this.hideCvPreview();
      return;
    }
    this.loadCvPreview();
  }

  isPdfCv(path?: string): boolean {
    const url = this.cvUrl(path);
    return !!url && /\.pdf($|\?)/i.test(url);
  }

  isImageCv(path?: string): boolean {
    const url = this.cvUrl(path);
    return !!url && /\.(png|jpe?g|gif|webp)($|\?)/i.test(url);
  }

  matchColor(score?: number | null): string {
    if (score == null) return 'default';
    if (score >= 75) return 'green';
    if (score >= 50) return 'blue';
    if (score >= 30) return 'orange';
    return 'red';
  }

  skillList(value?: string): string[] {
    if (!value) return [];
    return value.split(',').map((s) => s.trim()).filter(Boolean);
  }

  analyzeCv(app: JobApplication): void {
    this.analyzeLoading = true;
    this.recruitmentService.analyzeApplicationCv(app.applicationId).subscribe({
      next: (response) => {
        this.analyzeLoading = false;
        if (response.success && response.data) {
          this.selectedApplication = response.data;
          this.applications = this.applications.map((a) =>
            a.applicationId === response.data!.applicationId ? response.data! : a
          );
          this.rebuildGroups();
          this.message.success('Analyse CV terminée');
        }
      },
      error: (err) => {
        this.analyzeLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de l\'analyse du CV');
      }
    });
  }

  updateStatus(app: JobApplication, status: ApplicationStatus, interviewType: 'ONLINE' | 'PHYSICAL' = 'ONLINE'): void {
    if (status === 'ACCEPTED') {
      this.pendingApplication = app;
      this.pendingInterviewType = interviewType;
      const defaultStart = new Date();
      defaultStart.setHours(10, 0, 0, 0);
      const defaultEnd = new Date();
      defaultEnd.setHours(11, 0, 0, 0);
      this.interviewForm.reset({
        interviewDate: null,
        startTime: defaultStart,
        endTime: defaultEnd,
        interviewLocation: ''
      });
      this.interviewModalVisible = true;
      return;
    }

    this.actionLoading = true;
    this.recruitmentService.updateApplicationStatus(app.applicationId, { status }).subscribe({
      next: (response) => {
        this.actionLoading = false;
        if (response.success) {
          const msg =
            status === 'REJECTED'
              ? 'Candidature non retenue'
              : status === 'DESISTED'
                ? 'Candidature marquée comme désistée'
                : 'Statut mis a jour';
          this.message.success(msg);
          this.detailVisible = false;
          this.loadApplications();
        }
      },
      error: (err) => {
        this.actionLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de la mise à jour');
      }
    });
  }

  confirmInterview(): void {
    if (!this.pendingApplication || this.interviewForm.invalid) {
      this.interviewForm.markAllAsTouched();
      return;
    }

    const { interviewDate, startTime, endTime, interviewLocation } = this.interviewForm.value;
    const interviewAt = this.combineDateAndTime(interviewDate, startTime);
    const interviewEndAt = this.combineDateAndTime(interviewDate, endTime);

    if (!interviewAt || !interviewEndAt) {
      this.message.error('Date et horaires invalides');
      return;
    }
    if (interviewEndAt <= interviewAt) {
      this.message.error('L\'heure de fin doit etre apres l\'heure de debut');
      return;
    }

    const isPhysical = this.pendingInterviewType === 'PHYSICAL';
    this.actionLoading = true;
    this.recruitmentService.updateApplicationStatus(this.pendingApplication.applicationId, {
      status: 'ACCEPTED',
      interviewAt: this.formatLocalDateTime(interviewAt),
      interviewEndAt: this.formatLocalDateTime(interviewEndAt),
      interviewType: this.pendingInterviewType,
      interviewLocation: isPhysical && interviewLocation?.trim() ? interviewLocation.trim() : null
    }).subscribe({
      next: (response) => {
        this.actionLoading = false;
        if (response.success) {
          if (isPhysical) {
            this.message.success(
              `Entretien physique planifie le ${this.formatDisplayDate(interviewAt)}. Convocation envoyee au candidat.`
            );
          } else {
            const meetLink = response.data?.googleMeetLink;
            const warning = response.data?.meetingWarning;

            if (warning) {
              this.message.warning(warning, { nzDuration: 8000 });
            } else if (meetLink) {
              this.message.success(
                `Entretien planifie le ${this.formatDisplayDate(interviewAt)}. Lien de reunion envoye au candidat.`
              );
            } else {
              this.message.success('Candidature retenue et entretien planifié');
            }
          }

          this.interviewModalVisible = false;
          this.detailVisible = false;
          this.pendingApplication = null;
          this.loadApplications();
          this.router.navigate(['/rh/calendar'], {
            state: { selectedDate: interviewAt.toISOString() }
          });
        }
      },
      error: (err) => {
        this.actionLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de la planification');
      }
    });
  }

  openHireModal(app: JobApplication): void {
    this.pendingApplication = app;
    this.hireForm.patchValue({
      hireStartDate: null,
      hireNetSalary: '',
      hireContractType: null
    });
    this.hireModalVisible = true;
  }

  confirmHire(): void {
    if (!this.pendingApplication || this.hireForm.invalid) {
      this.hireForm.markAllAsTouched();
      return;
    }
    const value = this.hireForm.value;
    const startDate: Date = value.hireStartDate;
    const hireStartDate = this.formatLocalDate(startDate);

    // Address + GPS always come from the linked agency (company) in DB.
    this.actionLoading = true;
    this.recruitmentService.updateApplicationStatus(this.pendingApplication.applicationId, {
      status: 'HIRED',
      hireStartDate,
      hireNetSalary: value.hireNetSalary?.trim(),
      hireContractType: value.hireContractType?.trim() || null,
      hireWorkingHours: value.hireWorkingHours?.trim() || null,
      hireBenefits: value.hireBenefits?.trim() || null,
      hireIntegrationAddress: this.pendingApplication.companyAddress || null,
      hireIntegrationGpsUrl: this.pendingApplication.companyGoogleMapsUrl || null
    }).subscribe({
      next: (response) => {
        this.actionLoading = false;
        if (response.success) {
          this.message.success("Confirmation d'embauche envoyée au candidat");
          this.hireModalVisible = false;
          this.detailVisible = false;
          this.pendingApplication = null;
          this.loadApplications();
          this.router.navigate(['/rh/hired']);
        }
      },
      error: (err) => {
        this.actionLoading = false;
        this.message.error(err.error?.message || "Erreur lors de la confirmation d'embauche");
      }
    });
  }

  cvUrl(path?: string): string | null {
    return this.recruitmentService.resolveFileUrl(path);
  }

  statusColor(status: string): string {
    if (status === 'HIRED' || status === 'ACCEPTED') return 'green';
    if (status === 'REJECTED') return 'red';
    if (status === 'DESISTED') return 'orange';
    if (status === 'UNDER_REVIEW') return 'blue';
    return 'default';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      SUBMITTED: 'En attente',
      UNDER_REVIEW: 'En attente',
      ACCEPTED: 'Retenu',
      HIRED: 'Retenu',
      REJECTED: 'Non retenu',
      DESISTED: 'Désisté'
    };
    return labels[status] || status;
  }

  isFinalStatus(status: string): boolean {
    return status === 'REJECTED' || status === 'DESISTED' || status === 'HIRED';
  }

  hideCvPreview(): void {
    this.cvPreviewVisible = false;
    this.cvPreviewLoading = false;
    this.cvPreviewSrc = null;
    this.revokeCvObjectUrl();
  }

  private rebuildGroups(): void {
    const byRecruitment = new Map<string, RecruitmentApplicationsGroup>();
    const filtered = this.getFilteredApplications();

    for (const app of filtered) {
      const key = app.recruitmentId;
      let group = byRecruitment.get(key);
      if (!group) {
        group = {
          recruitmentId: app.recruitmentId,
          recruitmentTitle: app.recruitmentTitle || 'Offre sans titre',
          zoneName: app.zoneName,
          region: app.region,
          applications: []
        };
        byRecruitment.set(key, group);
      }
      group.applications.push(app);
    }

    this.groups = Array.from(byRecruitment.values())
      .map((group) => ({
        ...group,
        applications: [...group.applications].sort((a, b) => {
          const scoreDiff = (b.cvMatchScore ?? -1) - (a.cvMatchScore ?? -1);
          if (scoreDiff !== 0) return scoreDiff;
          return (b.qcmScore ?? -1) - (a.qcmScore ?? -1);
        })
      }))
      .sort((a, b) => a.recruitmentTitle.localeCompare(b.recruitmentTitle, 'fr'));
  }

  getFilteredApplications(): JobApplication[] {
    const first = this.filterFirstName.trim().toLowerCase();
    const last = this.filterLastName.trim().toLowerCase();
    const cin = this.filterCin.trim().toLowerCase();

    return this.applications.filter((app) => {
      const c = app.candidate;
      if (!c) {
        return !first && !last && !cin;
      }
      const matchFirst = !first || (c.firstName || '').toLowerCase().includes(first);
      const matchLast = !last || (c.lastName || '').toLowerCase().includes(last);
      const matchCin = !cin || (c.cin || '').toLowerCase().includes(cin);
      return matchFirst && matchLast && matchCin;
    });
  }

  applyFilters(): void {
    this.rebuildGroups();
  }

  clearFilters(): void {
    this.filterFirstName = '';
    this.filterLastName = '';
    this.filterCin = '';
    this.rebuildGroups();
  }

  private loadCvPreview(): void {
    const path = this.selectedApplication?.cvFileUrl;
    const url = this.cvUrl(path);
    if (!url) {
      return;
    }

    if (!this.isPdfCv(path) && !this.isImageCv(path)) {
      this.cvPreviewVisible = true;
      return;
    }

    this.cvPreviewLoading = true;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.revokeCvObjectUrl();
        this.cvObjectUrl = URL.createObjectURL(blob);
        this.cvPreviewSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.cvObjectUrl);
        this.cvPreviewVisible = true;
        this.cvPreviewLoading = false;
      },
      error: () => {
        this.cvPreviewLoading = false;
        this.message.error('Impossible de charger le CV');
      }
    });
  }

  private revokeCvObjectUrl(): void {
    if (this.cvObjectUrl) {
      URL.revokeObjectURL(this.cvObjectUrl);
      this.cvObjectUrl = null;
    }
  }

  private formatLocalDateTime(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
  }

  private formatLocalDate(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  private combineDateAndTime(dateValue: Date, timeValue: Date): Date | null {
    if (!dateValue || !timeValue) {
      return null;
    }
    const combined = new Date(dateValue);
    combined.setHours(timeValue.getHours(), timeValue.getMinutes(), 0, 0);
    return combined;
  }

  private formatDisplayDate(date: Date): string {
    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
