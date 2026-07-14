import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { JobApplication } from '../../models/recruitment.model';
import { RecruitmentService } from '../../services/recruitment.service';

@Component({
  selector: 'app-rh-calendar',
  templateUrl: './rh-calendar.component.html',
  styleUrl: './rh-calendar.component.css'
})
export class RhCalendarComponent implements OnInit {
  applications: JobApplication[] = [];
  loading = false;
  selectedDate = new Date();

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly router: Router,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    const state = this.router.getCurrentNavigation()?.extras.state as { selectedDate?: string } | undefined;
    if (state?.selectedDate) {
      this.selectedDate = new Date(state.selectedDate);
    }
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    this.recruitmentService.getRhApplications().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.applications = response.data.filter((app) => !!app.interviewAt);
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement du calendrier');
      }
    });
  }

  get selectedDayApplications(): JobApplication[] {
    return this.applications
      .filter((app) => app.interviewAt && this.isSameDay(new Date(app.interviewAt), this.selectedDate))
      .sort((a, b) => new Date(a.interviewAt!).getTime() - new Date(b.interviewAt!).getTime());
  }

  getDayCount(date: Date): number {
    return this.applications.filter((app) => app.interviewAt && this.isSameDay(new Date(app.interviewAt), date)).length;
  }

  private isSameDay(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear()
      && a.getMonth() === b.getMonth()
      && a.getDate() === b.getDate();
  }

  meetingJoinLabel(): string {
    return 'Rejoindre la reunion';
  }

  formatInterviewTime(app: JobApplication): string {
    if (!app.interviewAt) {
      return '';
    }
    const start = new Date(app.interviewAt);
    if (app.interviewEndAt) {
      const end = new Date(app.interviewEndAt);
      const startStr = start.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
      const endStr = end.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
      return `${startStr} — ${endStr}`;
    }
    return start.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
