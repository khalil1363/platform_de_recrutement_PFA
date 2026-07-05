import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { Recruitment } from '../../models/recruitment.model';

@Component({
  selector: 'app-job-detail',
  templateUrl: './job-detail.component.html',
  styleUrl: './job-detail.component.css'
})
export class JobDetailComponent implements OnInit {
  recruitment: Recruitment | null = null;
  loading = false;

  constructor(
    readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/jobs']);
      return;
    }
    this.loadJob(id);
  }

  loadJob(id: string): void {
    this.loading = true;
    this.recruitmentService.getPublishedRecruitment(id).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.recruitment = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Offre introuvable');
        this.router.navigate(['/jobs']);
      }
    });
  }

  apply(): void {
    if (!this.recruitment) {
      return;
    }
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }
    this.router.navigate(['/jobs', this.recruitment.recruitmentId, 'apply']);
  }

  back(): void {
    this.router.navigate(['/jobs']);
  }
}
