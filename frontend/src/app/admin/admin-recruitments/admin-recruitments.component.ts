import { Component, OnInit } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../recruitment/services/recruitment.service';
import { Recruitment } from '../../recruitment/models/recruitment.model';

export interface RhRecruitmentGroup {
  rhKey: string;
  rhName: string;
  items: Recruitment[];
}

@Component({
  selector: 'app-admin-recruitments',
  templateUrl: './admin-recruitments.component.html',
  styleUrl: './admin-recruitments.component.css'
})
export class AdminRecruitmentsComponent implements OnInit {
  loading = false;
  groups: RhRecruitmentGroup[] = [];
  search = '';
  totalCount = 0;
  private all: Recruitment[] = [];

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.recruitmentService.getRecruitments().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.all = response.data;
          this.applyFilter();
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des recrutements');
      }
    });
  }

  applyFilter(): void {
    const q = this.search.trim().toLowerCase();
    const filtered = !q
      ? this.all
      : this.all.filter((r) => {
          const hay = [
            r.title,
            r.companyName,
            r.zoneName,
            r.createdByRhName,
            r.responsibleName,
            r.status
          ]
            .filter(Boolean)
            .join(' ')
            .toLowerCase();
          return hay.includes(q);
        });
    this.groups = this.groupByRh(filtered);
    this.totalCount = filtered.length;
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
    if (status === 'PUBLISHED') return 'green';
    if (status === 'CLOSED') return 'red';
    return 'default';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      DRAFT: 'Brouillon',
      PUBLISHED: 'Publié',
      CLOSED: 'Clôturé'
    };
    return labels[status] || status;
  }

  private groupByRh(items: Recruitment[]): RhRecruitmentGroup[] {
    const map = new Map<string, RhRecruitmentGroup>();
    for (const item of items) {
      const rhKey = item.createdByRhUserId || 'unknown';
      const rhName = item.createdByRhName || item.responsibleName || 'RH non identifié';
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
