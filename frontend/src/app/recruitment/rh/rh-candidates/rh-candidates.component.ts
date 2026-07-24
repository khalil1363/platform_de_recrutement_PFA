import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { ApplicationStatus, ApplicationTrackingUpdateRequest, JobApplication } from '../../models/recruitment.model';

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
  trackingSaving = false;
  trackingForm!: FormGroup;

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
      hireContractType: [
        "Contrat à durée indéterminée (CDI), assorti d'une période d'essai de six (6) mois, renouvelable une seule fois, sous réserve d'éligibilité au contrat CIVP"
      ],
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
    this.trackingForm = this.fb.group({
      provenance: [''],
      diplomeEcole: [''],
      profilMetier: [''],
      affectation: [''],
      desistement: [''],
      dureeContrat: [''],
      composante: [''],
      dateDebutMission: [null],
      pretention: [''],
      observation: ['']
    });
    this.loadApplications();
  }

  get interviewModalTitle(): string {
    return this.pendingInterviewType === 'PHYSICAL'
      ? "Planifier l'entretien physique"
      : "Planifier l'entretien en ligne";
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
    this.patchTrackingForm(app);
    this.detailVisible = true;
  }

  exportMonthlyExcel(): void {
    this.exportLoading = true;
    this.recruitmentService.exportCandidatesMonthlyExcel().subscribe({
      next: (blob) => {
        this.exportLoading = false;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'daam-candidats-par-mois.xlsx';
        a.click();
        window.URL.revokeObjectURL(url);
        this.message.success('Export Excel téléchargé (une feuille par mois)');
      },
      error: () => {
        this.exportLoading = false;
        this.message.error('Erreur lors de l\'export Excel');
      }
    });
  }

  saveTracking(): void {
    if (!this.selectedApplication) {
      return;
    }
    const value = this.trackingForm.value;
    const request: ApplicationTrackingUpdateRequest = {
      provenance: value.provenance,
      diplomeEcole: value.diplomeEcole,
      profilMetier: value.profilMetier,
      affectation: value.affectation,
      desistement: value.desistement,
      dureeContrat: value.dureeContrat,
      composante: value.composante,
      dateDebutMission: this.formatLocalDateOrNull(value.dateDebutMission),
      pretention: value.pretention,
      observation: value.observation
    };
    this.trackingSaving = true;
    this.recruitmentService.updateApplicationTracking(this.selectedApplication.applicationId, request).subscribe({
      next: (response) => {
        this.trackingSaving = false;
        if (response.success && response.data) {
          this.selectedApplication = response.data;
          this.applications = this.applications.map((a) =>
            a.applicationId === response.data!.applicationId ? response.data! : a
          );
          this.rebuildGroups();
          this.message.success('Fiche suivi enregistrée — sera incluse dans l\'export Excel');
        }
      },
      error: (err) => {
        this.trackingSaving = false;
        this.message.error(err.error?.message || 'Erreur lors de l\'enregistrement');
      }
    });
  }

  private patchTrackingForm(app: JobApplication): void {
    this.trackingForm.reset({
      provenance: app.provenance || (app.keejobReference ? 'KEEJOB' : 'Plateforme DAAM'),
      diplomeEcole: app.diplomeEcole || '',
      profilMetier: app.profilMetier || app.recruitmentTitle || '',
      affectation: app.affectation || app.companyName || app.zoneName || '',
      desistement: app.desistement || '',
      dureeContrat: app.dureeContrat || app.hireContractType || app.formatMission || '',
      composante: app.composante || app.imf || app.companyName || '',
      dateDebutMission: app.dateDebutMission
        ? new Date(app.dateDebutMission)
        : app.hireStartDate
          ? new Date(app.hireStartDate)
          : null,
      pretention: app.pretention || app.disponibilite || app.salaireActuel || app.prixMois || '',
      observation: app.observation || app.commentairesRh || app.remarquesRh || ''
    });
  }

  private formatLocalDateOrNull(date: Date | null): string | null {
    if (!date) {
      return null;
    }
    return this.formatLocalDate(date);
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
          this.message.success(status === 'REJECTED' ? 'Candidature rejetee' : 'Statut mis a jour');
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
              this.message.success('Candidature acceptee et entretien planifie');
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
      hireNetSalary: ''
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
    if (status === 'HIRED') return 'purple';
    if (status === 'ACCEPTED') return 'green';
    if (status === 'REJECTED') return 'red';
    if (status === 'UNDER_REVIEW') return 'blue';
    return 'default';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      SUBMITTED: 'Soumise',
      UNDER_REVIEW: 'En cours',
      ACCEPTED: 'Acceptée',
      HIRED: 'Embauché',
      REJECTED: 'Rejetée'
    };
    return labels[status] || status;
  }

  hideCvPreview(): void {
    this.cvPreviewVisible = false;
    this.cvPreviewLoading = false;
    this.cvPreviewSrc = null;
    this.revokeCvObjectUrl();
  }

  private rebuildGroups(): void {
    const byRecruitment = new Map<string, RecruitmentApplicationsGroup>();

    for (const app of this.applications) {
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
