import { Component, OnInit } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { DashboardStatusStat, SelectionDashboard } from '../../models/recruitment.model';

const EMPTY_DASHBOARD: SelectionDashboard = {
  totalCampaigns: 0,
  responsibleNames: [],
  totalCandidates: 0,
  retenus: 0,
  nonRetenus: 0,
  nonPresentes: 0,
  desistes: 0,
  selectionRate: 0,
  integresMonth: 0,
  heberges: 0,
  statusStats: [],
  bySource: [],
  byResponsible: [],
  topAgencies: [],
  topPosts: []
};

interface PipelineStep {
  key: string;
  label: string;
  count: number;
  percent: number;
  tone: 'blue' | 'green' | 'red' | 'orange' | 'purple';
}

@Component({
  selector: 'app-rh-dashboard',
  templateUrl: './rh-dashboard.component.html',
  styleUrl: './rh-dashboard.component.css'
})
export class RhDashboardComponent implements OnInit {
  loading = false;
  loadError = false;
  dashboard: SelectionDashboard = { ...EMPTY_DASHBOARD };
  pipelineSteps: PipelineStep[] = [];
  filterMonth: Date | null = null;

  readonly indicatorSlots = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];

  private readonly agencyColors = ['#1f6feb', '#2f9e44', '#7048e8', '#e67700', '#c92a2a', '#0ca678', '#364fc7'];
  private readonly postColors = ['#0c8599', '#2f9e44', '#d4a017', '#c92a2a', '#364fc7', '#7048e8'];

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.refreshPipelineSteps();
    this.loadDashboard();
  }

  get periodLabel(): string {
    if (this.filterMonth) {
      const y = this.filterMonth.getFullYear();
      return `${y} – ${y + 1}`;
    }
    const now = new Date();
    return `${now.getFullYear()} – ${now.getFullYear() + 1}`;
  }

  get subtitle(): string {
    const names = this.dashboard.responsibleNames ?? [];
    const responsables = names.length ? names.join(' · ') : '—';
    return `${this.dashboard.totalCandidates} candidats sur ${this.dashboard.totalCampaigns} campagnes coworking · Responsables : ${responsables}`;
  }

  get integresLabel(): string {
    const month = this.dashboard.filterMonthLabel || 'CE MOIS';
    return `INTÉGRÉS ${month}`;
  }

  loadDashboard(): void {
    this.loading = true;
    this.loadError = false;
    const year = this.filterMonth ? this.filterMonth.getFullYear() : null;
    const month = this.filterMonth ? this.filterMonth.getMonth() + 1 : null;

    this.recruitmentService.getSelectionDashboard(year, month).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.applyDashboard(response.data);
        } else {
          this.applyDashboard({ ...EMPTY_DASHBOARD });
          this.loadError = true;
        }
      },
      error: () => {
        this.loading = false;
        this.applyDashboard({ ...EMPTY_DASHBOARD });
        this.loadError = true;
        this.message.error('Impossible de charger le tableau de bord. Vérifiez que le service recrutement est démarré.');
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

  agencyColor(index: number): string {
    return this.agencyColors[index % this.agencyColors.length];
  }

  postColor(index: number): string {
    return this.postColors[index % this.postColors.length];
  }

  isIndicatorFilled(level: number | null | undefined, index: number): boolean {
    const safeLevel = Math.max(0, Math.min(10, Number(level ?? 0)));
    return index < safeLevel;
  }

  statusBarColor(stat: DashboardStatusStat): string {
    switch (stat.key) {
      case 'RETENU':
        return '#2f9e44';
      case 'NON_RETENU':
        return '#e03131';
      case 'NON_PRESENTE':
        return '#f08c00';
      case 'DESISTE':
        return '#7048e8';
      default:
        return '#364fc7';
    }
  }

  formatRate(value: number | null | undefined): string {
    const safe = Number(value ?? 0);
    if (!Number.isFinite(safe)) {
      return '0%';
    }
    return `${safe.toFixed(1).replace('.0', '')}%`;
  }

  trackPipelineStep(_index: number, step: PipelineStep): string {
    return step.key;
  }

  rankClass(index: number): string {
    if (index === 0) return 'rank-gold';
    if (index === 1) return 'rank-silver';
    if (index === 2) return 'rank-bronze';
    return 'rank-default';
  }

  rateTone(rate: number | null | undefined): string {
    const value = Number(rate ?? 0);
    if (value >= 30) return 'rate-high';
    if (value >= 15) return 'rate-mid';
    return 'rate-low';
  }

  private applyDashboard(data: SelectionDashboard): void {
    this.dashboard = {
      ...EMPTY_DASHBOARD,
      ...data,
      responsibleNames: data.responsibleNames ?? [],
      statusStats: data.statusStats ?? [],
      bySource: data.bySource ?? [],
      byResponsible: data.byResponsible ?? [],
      topAgencies: data.topAgencies ?? [],
      topPosts: data.topPosts ?? []
    };
    this.refreshPipelineSteps();
  }

  private refreshPipelineSteps(): void {
    const total = this.dashboard.totalCandidates || 1;
    this.pipelineSteps = [
      {
        key: 'recus',
        label: 'REÇUS',
        count: this.dashboard.totalCandidates,
        percent: 100,
        tone: 'blue'
      },
      {
        key: 'retenus',
        label: 'RETENUS',
        count: this.dashboard.retenus,
        percent: this.percent(this.dashboard.retenus, total),
        tone: 'green'
      },
      {
        key: 'nonRetenus',
        label: 'NON RETENUS',
        count: this.dashboard.nonRetenus,
        percent: this.percent(this.dashboard.nonRetenus, total),
        tone: 'red'
      },
      {
        key: 'nonPresentes',
        label: 'NON PRÉSENTÉS',
        count: this.dashboard.nonPresentes,
        percent: this.percent(this.dashboard.nonPresentes, total),
        tone: 'orange'
      },
      {
        key: 'desistes',
        label: 'DÉSISTÉS',
        count: this.dashboard.desistes,
        percent: this.percent(this.dashboard.desistes, total),
        tone: 'purple'
      }
    ];
  }

  private percent(count: number, total: number): number {
    if (!total) {
      return 0;
    }
    return Math.round((count / total) * 1000) / 10;
  }
}
