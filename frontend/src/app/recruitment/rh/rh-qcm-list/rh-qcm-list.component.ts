import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { Qcm } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-qcm-list',
  templateUrl: './rh-qcm-list.component.html',
  styleUrl: './rh-qcm-list.component.css'
})
export class RhQcmListComponent implements OnInit {
  qcms: Qcm[] = [];
  loading = false;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly router: Router,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadQcms();
  }

  loadQcms(): void {
    this.loading = true;
    this.recruitmentService.getQcms().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.qcms = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des QCM');
      }
    });
  }

  create(): void {
    this.router.navigate(['/rh/qcm/new']);
  }

  edit(id: string): void {
    this.router.navigate(['/rh/qcm', id, 'edit']);
  }

  delete(qcm: Qcm): void {
    this.recruitmentService.deleteQcm(qcm.qcmId).subscribe({
      next: (response) => {
        if (response.success) {
          this.message.success('QCM supprimé');
          this.loadQcms();
        }
      },
      error: (err) => {
        this.message.error(err.error?.message || 'Impossible de supprimer ce QCM');
      }
    });
  }
}
