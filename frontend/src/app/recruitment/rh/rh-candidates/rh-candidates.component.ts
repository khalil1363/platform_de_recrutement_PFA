import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { ApplicationStatus, JobApplication } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-candidates',
  templateUrl: './rh-candidates.component.html',
  styleUrl: './rh-candidates.component.css'
})
export class RhCandidatesComponent implements OnInit, OnDestroy {
  applications: JobApplication[] = [];
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
      interviewAt: [null, Validators.required]
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

  updateStatus(app: JobApplication, status: ApplicationStatus): void {
    if (status === 'ACCEPTED') {
      this.pendingApplication = app;
      this.interviewForm.reset();
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

    const interviewAt: Date = this.interviewForm.value.interviewAt;
    this.actionLoading = true;
    this.recruitmentService.updateApplicationStatus(this.pendingApplication.applicationId, {
      status: 'ACCEPTED',
      interviewAt: this.formatLocalDateTime(interviewAt)
    }).subscribe({
      next: (response) => {
        this.actionLoading = false;
        if (response.success) {
          const meetLink = response.data?.googleMeetLink;
          this.message.success(
            meetLink
              ? `Entretien planifie le ${this.formatDisplayDate(interviewAt)}. Lien Meet cree.`
              : 'Candidature acceptee et entretien planifie'
          );
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
