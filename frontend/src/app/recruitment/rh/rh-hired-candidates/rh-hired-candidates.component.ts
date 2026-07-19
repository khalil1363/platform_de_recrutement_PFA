import { Component, OnInit } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { HiredQcmAssignment, JobApplication, Qcm } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-hired-candidates',
  templateUrl: './rh-hired-candidates.component.html',
  styleUrl: './rh-hired-candidates.component.css'
})
export class RhHiredCandidatesComponent implements OnInit {
  applications: JobApplication[] = [];
  loading = false;
  selected: JobApplication | null = null;
  detailVisible = false;

  qcmModalVisible = false;
  qcms: Qcm[] = [];
  selectedQcmId: string | null = null;
  assignLoading = false;
  assignTarget: JobApplication | null = null;
  assignments: HiredQcmAssignment[] = [];
  assignmentsLoading = false;
  exportLoading = false;

  constructor(
    readonly authService: AuthService,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.load();
    this.recruitmentService.getQcms().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.qcms = response.data;
        }
      }
    });
  }

  exportExcel(): void {
    this.exportLoading = true;
    this.recruitmentService.exportHiredEvaluationsExcel().subscribe({
      next: (blob) => {
        this.exportLoading = false;
        this.downloadBlob(blob, 'suivi-test-psy-cc.xlsx');
        this.message.success('Export Excel téléchargé');
      },
      error: () => {
        this.exportLoading = false;
        this.message.error('Erreur lors de l\'export Excel');
      }
    });
  }

  downloadReport(assignment: HiredQcmAssignment): void {
    if (assignment.status === 'ASSIGNED') {
      this.message.warning('Le candidat n\'a pas encore terminé le QCM');
      return;
    }
    this.recruitmentService.downloadHiredQcmReportPdf(assignment.assignmentId).subscribe({
      next: (blob) => {
        const name = `rapport-talent-${assignment.candidate?.lastName || assignment.assignmentId}.pdf`;
        this.downloadBlob(blob, name);
      },
      error: () => this.message.error('Erreur lors du téléchargement du rapport PDF')
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  load(): void {
    this.loading = true;
    this.recruitmentService.getHiredApplications().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.applications = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des candidats admis');
      }
    });
  }

  openDetails(app: JobApplication): void {
    this.selected = app;
    this.detailVisible = true;
    this.loadAssignments(app.applicationId);
  }

  openAssignQcm(app: JobApplication): void {
    this.assignTarget = app;
    this.selectedQcmId = null;
    this.qcmModalVisible = true;
  }

  confirmAssignQcm(): void {
    if (!this.assignTarget || !this.selectedQcmId) {
      this.message.warning('Sélectionnez un QCM');
      return;
    }
    this.assignLoading = true;
    this.recruitmentService.assignHiredQcm(this.assignTarget.applicationId, this.selectedQcmId).subscribe({
      next: (response) => {
        this.assignLoading = false;
        if (response.success) {
          this.message.success('QCM assigné — le candidat le verra dans Mes évaluations');
          this.qcmModalVisible = false;
          if (this.selected?.applicationId === this.assignTarget?.applicationId) {
            this.loadAssignments(this.assignTarget!.applicationId);
          }
          this.assignTarget = null;
        }
      },
      error: (err) => {
        this.assignLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de l\'assignation du QCM');
      }
    });
  }

  assignmentStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      ASSIGNED: 'En attente',
      COMPLETED: 'Terminé',
      VIOLATED: 'Triche / score 0'
    };
    return labels[status] || status;
  }

  assignmentStatusColor(status: string): string {
    if (status === 'COMPLETED') return 'green';
    if (status === 'VIOLATED') return 'red';
    return 'blue';
  }

  benefitLines(value?: string): string[] {
    if (!value) return [];
    return value.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  }

  private loadAssignments(applicationId: string): void {
    this.assignmentsLoading = true;
    this.recruitmentService.getHiredQcmForApplication(applicationId).subscribe({
      next: (response) => {
        this.assignmentsLoading = false;
        this.assignments = response.success && response.data ? response.data : [];
      },
      error: () => {
        this.assignmentsLoading = false;
        this.assignments = [];
      }
    });
  }
}
