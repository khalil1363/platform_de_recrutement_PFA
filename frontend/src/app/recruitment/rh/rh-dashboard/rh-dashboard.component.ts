import { Component, OnInit } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { CoworkingDashboard } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-dashboard',
  templateUrl: './rh-dashboard.component.html',
  styleUrl: './rh-dashboard.component.css'
})
export class RhDashboardComponent implements OnInit {
  loading = false;
  dashboard: CoworkingDashboard | null = null;
  filterMonth: Date | null = null;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  get totalHired(): number {
    return (this.dashboard?.agencyOutcomes || []).reduce((sum, row) => sum + (row.hiredCount || 0), 0);
  }

  get totalRejected(): number {
    return (this.dashboard?.agencyOutcomes || []).reduce((sum, row) => sum + (row.rejectedCount || 0), 0);
  }

  loadDashboard(): void {
    this.loading = true;
    const year = this.filterMonth ? this.filterMonth.getFullYear() : null;
    const month = this.filterMonth ? this.filterMonth.getMonth() + 1 : null;

    this.recruitmentService.getCoworkingDashboard(year, month).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.dashboard = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Impossible de charger le tableau de bord coworking');
      }
    });
  }

  onMonthChange(value: Date | null): void {
    this.filterMonth = value;
    this.loadDashboard();
  }

  clearMonthFilter(): void {
    this.filterMonth = null;
    this.loadDashboard();
  }

  maxAgencyCount(): number {
    if (!this.dashboard?.byAgency?.length) {
      return 1;
    }
    return Math.max(...this.dashboard.byAgency.map((a) => a.coworkingCount), 1);
  }

  maxPostCount(): number {
    if (!this.dashboard?.byPost?.length) {
      return 1;
    }
    return Math.max(...this.dashboard.byPost.map((p) => p.coworkingCount), 1);
  }

  barWidth(count: number, max: number): string {
    return `${Math.max(8, Math.round((count / max) * 100))}%`;
  }
}
