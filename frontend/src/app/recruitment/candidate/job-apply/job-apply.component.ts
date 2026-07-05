import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadFile, NzUploadXHRArgs } from 'ng-zorro-antd/upload';
import { Subscription } from 'rxjs';
import { RecruitmentService } from '../../services/recruitment.service';
import { QcmAnswer, Recruitment } from '../../models/recruitment.model';

@Component({
  selector: 'app-job-apply',
  templateUrl: './job-apply.component.html',
  styleUrl: './job-apply.component.css'
})
export class JobApplyComponent implements OnInit {
  recruitment: Recruitment | null = null;
  loading = false;
  submitting = false;
  currentStep = 0;
  cvFileUrl = '';
  cvUploading = false;
  answers: Record<string, string> = {};

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/jobs']);
      return;
    }
    this.loadJob(id);
  }

  loadJob(id: string): void {
    this.loading = true;
    this.recruitmentService.getPublishedRecruitment(id).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.recruitment = response.data;
          if (!this.recruitment.questions?.length) {
            this.message.warning('Cette offre ne contient pas encore de QCM');
          }
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Offre introuvable');
        this.router.navigate(['/jobs']);
      }
    });
  }

  customCvUpload = (item: NzUploadXHRArgs): Subscription => {
    this.cvUploading = true;
    return this.recruitmentService.uploadCv(item.file as unknown as File).subscribe({
      next: (response) => {
        this.cvUploading = false;
        if (response.success && response.data?.cvFileUrl) {
          this.cvFileUrl = response.data.cvFileUrl;
          item.onSuccess?.(response, item.file, null);
          this.message.success('CV téléchargé avec succès');
        }
      },
      error: (err) => {
        this.cvUploading = false;
        item.onError?.(err, item.file);
        this.message.error('Échec du téléchargement du CV');
      }
    });
  };

  beforeCvUpload = (file: NzUploadFile): boolean => {
    const isAllowed = file.type === 'application/pdf'
      || file.type === 'application/msword'
      || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
    if (!isAllowed) {
      this.message.error('Seuls les fichiers PDF et Word sont acceptés');
    }
    return isAllowed;
  };

  nextStep(): void {
    if (this.currentStep === 0 && !this.cvFileUrl) {
      this.message.warning('Veuillez télécharger votre CV');
      return;
    }
    if (this.currentStep === 1) {
      const questions = this.recruitment?.questions || [];
      for (const q of questions) {
        if (!q.questionId || !this.answers[q.questionId]) {
          this.message.warning('Veuillez répondre à toutes les questions');
          return;
        }
      }
    }
    this.currentStep++;
  }

  prevStep(): void {
    this.currentStep--;
  }

  submit(): void {
    if (!this.recruitment || !this.cvFileUrl) {
      return;
    }
    const questions = this.recruitment.questions || [];
    const answerList: QcmAnswer[] = questions
      .filter((q) => q.questionId)
      .map((q) => ({
        questionId: q.questionId!,
        selectedOption: this.answers[q.questionId!]
      }));

    this.submitting = true;
    this.recruitmentService.apply({
      recruitmentId: this.recruitment.recruitmentId,
      cvFileUrl: this.cvFileUrl,
      answers: answerList
    }).subscribe({
      next: (response) => {
        this.submitting = false;
        if (response.success) {
          this.message.success('Candidature envoyée avec succès');
          this.router.navigate(['/jobs/applications']);
        }
      },
      error: (err) => {
        this.submitting = false;
        this.message.error(err.error?.message || 'Échec de l\'envoi de la candidature');
      }
    });
  }

  back(): void {
    if (this.recruitment) {
      this.router.navigate(['/jobs', this.recruitment.recruitmentId]);
    }
  }
}
