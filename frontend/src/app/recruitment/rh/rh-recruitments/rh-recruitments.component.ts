import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { Recruitment } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-recruitments',
  templateUrl: './rh-recruitments.component.html',
  styleUrl: './rh-recruitments.component.css'
})
export class RhRecruitmentsComponent implements OnInit {
  recruitments: Recruitment[] = [];
  loading = false;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly router: Router,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadRecruitments();
  }

  loadRecruitments(): void {
    this.loading = true;
    this.recruitmentService.getRecruitments().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.recruitments = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des recrutements');
      }
    });
  }

  create(): void {
    this.router.navigate(['/rh/recruitments/new']);
  }

  edit(id: string): void {
    this.router.navigate(['/rh/recruitments', id, 'edit']);
  }

  statusColor(status: string): string {
    if (status === 'PUBLISHED') return 'green';
    if (status === 'CLOSED') return 'red';
    return 'default';
  }
}
