import { Component, OnInit } from '@angular/core';
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
export class RhApplicationsComponent implements OnInit {
  applications: JobApplication[] = [];
  loading = false;
  recruitmentId = '';
  selectedApplication: JobApplication | null = null;
  detailVisible = false;

  constructor(
    readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.recruitmentId = this.route.snapshot.paramMap.get('id') || '';
    if (!this.recruitmentId) {
      this.router.navigate(['/rh/recruitments']);
      return;
    }
    this.loadApplications();
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
    this.detailVisible = true;
  }

  back(): void {
    this.router.navigate(['/rh/recruitments']);
  }

  cvUrl(path?: string): string | null {
    return this.recruitmentService.resolveFileUrl(path);
  }
}
