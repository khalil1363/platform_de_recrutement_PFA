import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { HiredQcmAssignment } from '../../models/recruitment.model';

@Component({
  selector: 'app-my-evaluations',
  templateUrl: './my-evaluations.component.html',
  styleUrl: './my-evaluations.component.css'
})
export class MyEvaluationsComponent implements OnInit {
  assignments: HiredQcmAssignment[] = [];
  loading = false;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly router: Router,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.recruitmentService.getMyHiredQcm().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.assignments = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Impossible de charger vos évaluations');
      }
    });
  }

  open(assignment: HiredQcmAssignment): void {
    if (assignment.status === 'ASSIGNED') {
      this.router.navigate(['/jobs/evaluations', assignment.assignmentId]);
    }
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      ASSIGNED: 'À passer',
      COMPLETED: 'Terminé',
      VIOLATED: 'Triche / score 0'
    };
    return labels[status] || status;
  }

  statusColor(status: string): string {
    if (status === 'COMPLETED') return 'green';
    if (status === 'VIOLATED') return 'red';
    return 'blue';
  }
}
