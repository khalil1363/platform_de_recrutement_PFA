import { Component, OnInit } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../core/services/auth.service';
import { RecruitmentService } from '../../recruitment/services/recruitment.service';
import { JobApplication } from '../../recruitment/models/recruitment.model';

export interface RhApplicationGroup {
  rhKey: string;
  rhName: string;
  items: JobApplication[];
}

@Component({
  selector: 'app-admin-applications',
  templateUrl: './admin-applications.component.html',
  styleUrl: './admin-applications.component.css'
})
export class AdminApplicationsComponent implements OnInit {
  loading = false;
  groups: RhApplicationGroup[] = [];
  filterFirstName = '';
  filterLastName = '';
  filterCin = '';
  statusFilter: string | null = null;
  totalCount = 0;
  private all: JobApplication[] = [];

  historyVisible = false;
  historyLoading = false;
  historyItems: JobApplication[] = [];
  historyCandidateLabel = '';

  readonly statusOptions = [
    { value: 'PENDING', label: 'En attente' },
    { value: 'ACCEPTED', label: 'Retenu' },
    { value: 'HIRED', label: 'Admis' },
    { value: 'REJECTED', label: 'Non retenu' },
    { value: 'DESISTED', label: 'Désisté' }
  ];

  constructor(
    readonly authService: AuthService,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.recruitmentService.getRhApplications().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.all = response.data;
          this.applyFilter();
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des candidatures');
      }
    });
  }

  applyFilter(): void {
    const first = this.filterFirstName.trim().toLowerCase();
    const last = this.filterLastName.trim().toLowerCase();
    const cin = this.filterCin.trim().toLowerCase();

    const filtered = this.all.filter((app) => {
      if (this.statusFilter) {
        if (this.statusFilter === 'PENDING') {
          if (app.status !== 'SUBMITTED' && app.status !== 'UNDER_REVIEW') {
            return false;
          }
        } else if (app.status !== this.statusFilter) {
          return false;
        }
      }
      if (first && !(app.candidate?.firstName || '').toLowerCase().includes(first)) {
        return false;
      }
      if (last && !(app.candidate?.lastName || '').toLowerCase().includes(last)) {
        return false;
      }
      if (cin && !(app.candidate?.cin || '').toLowerCase().includes(cin)) {
        return false;
      }
      return true;
    });
    this.groups = this.groupByRh(filtered);
    this.totalCount = filtered.length;
  }

  clearFilters(): void {
    this.filterFirstName = '';
    this.filterLastName = '';
    this.filterCin = '';
    this.statusFilter = null;
    this.applyFilter();
  }

  initials(name: string): string {
    const parts = (name || '')
      .trim()
      .split(/\s+/)
      .filter(Boolean);
    if (!parts.length) {
      return 'RH';
    }
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  }

  statusColor(status: string): string {
    if (status === 'ACCEPTED' || status === 'HIRED') return 'green';
    if (status === 'REJECTED') return 'red';
    if (status === 'DESISTED') return 'orange';
    if (status === 'UNDER_REVIEW') return 'blue';
    return 'default';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      SUBMITTED: 'En attente',
      UNDER_REVIEW: 'En attente',
      ACCEPTED: 'Retenu',
      HIRED: 'Admis',
      REJECTED: 'Non retenu',
      DESISTED: 'Désisté'
    };
    return labels[status] || status;
  }

  openHistory(app: JobApplication): void {
    this.historyCandidateLabel = `${app.candidate?.firstName || ''} ${app.candidate?.lastName || ''}`.trim()
      || app.candidate?.email
      || 'Candidat';
    this.historyVisible = true;
    this.historyLoading = true;
    this.historyItems = [];
    this.recruitmentService.getCandidateApplicationHistory(app.candidateUserId).subscribe({
      next: (response) => {
        this.historyLoading = false;
        if (response.success && response.data) {
          this.historyItems = response.data;
        }
      },
      error: () => {
        this.historyLoading = false;
        this.message.error('Impossible de charger l\'historique');
      }
    });
  }

  closeHistory(): void {
    this.historyVisible = false;
    this.historyItems = [];
  }

  private groupByRh(items: JobApplication[]): RhApplicationGroup[] {
    const map = new Map<string, RhApplicationGroup>();
    for (const item of items) {
      const rhKey = item.createdByRhUserId || 'unknown';
      const rhName = item.createdByRhName || 'RH non identifié';
      let group = map.get(rhKey);
      if (!group) {
        group = { rhKey, rhName, items: [] };
        map.set(rhKey, group);
      }
      group.items.push(item);
    }
    return Array.from(map.values()).sort((a, b) => a.rhName.localeCompare(b.rhName, 'fr'));
  }
}
