import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { Recruitment } from '../../models/recruitment.model';

@Component({
  selector: 'app-job-list',
  templateUrl: './job-list.component.html',
  styleUrl: './job-list.component.css'
})
export class JobListComponent implements OnInit {
  recruitments: Recruitment[] = [];
  loading = false;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly router: Router,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.loading = true;
    this.recruitmentService.getPublishedRecruitments().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.recruitments = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Impossible de charger les offres');
      }
    });
  }

  viewJob(id: string): void {
    this.router.navigate(['/jobs', id]);
  }
}
