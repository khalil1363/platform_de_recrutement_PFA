import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { JobApplication } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-applications',
  templateUrl: './rh-applications.component.html',
  styleUrl: './rh-applications.component.css'
})
export class RhApplicationsComponent implements OnInit, OnDestroy {
  applications: JobApplication[] = [];
  loading = false;
  recruitmentId = '';
  selectedApplication: JobApplication | null = null;
  detailVisible = false;
  cvPreviewVisible = false;
  cvPreviewLoading = false;
  cvPreviewSrc: SafeResourceUrl | null = null;
  cvObjectUrl: string | null = null;

  constructor(
    readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService,
    private readonly sanitizer: DomSanitizer,
    private readonly http: HttpClient
  ) {}

  ngOnInit(): void {
    this.recruitmentId = this.route.snapshot.paramMap.get('id') || '';
    if (!this.recruitmentId) {
      this.router.navigate(['/rh/recruitments']);
      return;
    }
    this.loadApplications();
  }

  ngOnDestroy(): void {
    this.revokeCvObjectUrl();
  }

  loadApplications(): void {
    this.loading = true;
    this.recruitmentService.getApplications(this.recruitmentId).subscribe({
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

  hideCvPreview(): void {
    this.cvPreviewVisible = false;
    this.cvPreviewLoading = false;
    this.cvPreviewSrc = null;
    this.revokeCvObjectUrl();
  }

  isPdfCv(path?: string): boolean {
    const url = this.cvUrl(path);
    return !!url && /\.pdf($|\?)/i.test(url);
  }

  isImageCv(path?: string): boolean {
    const url = this.cvUrl(path);
    return !!url && /\.(png|jpe?g|gif|webp)($|\?)/i.test(url);
  }

  back(): void {
    this.router.navigate(['/rh/recruitments']);
  }

  cvUrl(path?: string): string | null {
    return this.recruitmentService.resolveFileUrl(path);
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
}
