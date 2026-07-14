import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { ApplicationStatus, JobApplication } from '../../models/recruitment.model';

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
  actionLoading = false;
  interviewForm!: FormGroup;
  pendingApplication: JobApplication | null = null;
  cvPreviewVisible = false;
  cvPreviewLoading = false;
  cvPreviewSrc: SafeResourceUrl | null = null;
  cvObjectUrl: string | null = null;
  analyzeLoading = false;

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
      endTime: [null, Validators.required]
    });
    this.loadApplications();
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
    this.detailVisible = true;
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

  updateStatus(app: JobApplication, status: ApplicationStatus): void {
    if (status === 'ACCEPTED') {
      this.pendingApplication = app;
      const defaultStart = new Date();
      defaultStart.setHours(10, 0, 0, 0);
      const defaultEnd = new Date();
      defaultEnd.setHours(11, 0, 0, 0);
      this.interviewForm.reset({
        interviewDate: null,
        startTime: defaultStart,
        endTime: defaultEnd
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

    const { interviewDate, startTime, endTime } = this.interviewForm.value;
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

    this.actionLoading = true;
    this.recruitmentService.updateApplicationStatus(this.pendingApplication.applicationId, {
      status: 'ACCEPTED',
      interviewAt: this.formatLocalDateTime(interviewAt),
      interviewEndAt: this.formatLocalDateTime(interviewEndAt)
    }).subscribe({
      next: (response) => {
        this.actionLoading = false;
        if (response.success) {
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

  cvUrl(path?: string): string | null {
    return this.recruitmentService.resolveFileUrl(path);
  }

  statusColor(status: string): string {
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
