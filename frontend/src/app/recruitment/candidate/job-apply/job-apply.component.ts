import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadFile, NzUploadXHRArgs } from 'ng-zorro-antd/upload';
import { Subscription } from 'rxjs';
import { RecruitmentService } from '../../services/recruitment.service';
import { QcmAnswer, QcmQuestion, Recruitment } from '../../models/recruitment.model';

@Component({
  selector: 'app-job-apply',
  templateUrl: './job-apply.component.html',
  styleUrl: './job-apply.component.css'
})
export class JobApplyComponent implements OnInit, OnDestroy {
  recruitment: Recruitment | null = null;
  loading = false;
  submitting = false;
  currentStep = 0;
  currentQuestionIndex = 0;
  cvFileUrl = '';
  cvUploading = false;
  answers: Record<string, string> = {};
  qcmViolated = false;
  violationMessage = '';
  quizMonitoring = false;

  private quizBaselineWidth = 0;
  private quizBaselineHeight = 0;
  private readonly minSizeRatio = 0.85;
  private boundVisibilityHandler = (): void => this.onVisibilityChange();
  private boundResizeHandler = (): void => this.onWindowResize();

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

  ngOnDestroy(): void {
    this.stopQuizMonitoring();
  }

  get questions(): QcmQuestion[] {
    return this.recruitment?.questions || [];
  }

  get currentQuestion(): QcmQuestion | null {
    return this.questions[this.currentQuestionIndex] ?? null;
  }

  get totalQuestions(): number {
    return this.questions.length;
  }

  get answeredQuestionsCount(): number {
    return this.questions.filter((q) => q.questionId && this.answers[q.questionId]).length;
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
    if (this.qcmViolated || this.submitting) {
      return;
    }
    if (this.currentStep === 0) {
      if (!this.cvFileUrl) {
        this.message.warning('Veuillez télécharger votre CV');
        return;
      }
      this.currentStep = 1;
      this.currentQuestionIndex = 0;
      this.startQuizMonitoring();
      return;
    }

    if (this.currentStep === 1) {
      this.nextQuestion();
    }
  }

  prevStep(): void {
    if (this.qcmViolated || this.submitting) {
      return;
    }
    if (this.currentStep === 1) {
      if (this.currentQuestionIndex > 0) {
        this.currentQuestionIndex--;
        return;
      }
      this.stopQuizMonitoring();
      this.currentStep = 0;
      return;
    }
    if (this.currentStep === 2) {
      this.currentStep = 1;
      this.currentQuestionIndex = Math.max(0, this.totalQuestions - 1);
      this.startQuizMonitoring();
    }
  }

  nextQuestion(): void {
    if (this.qcmViolated || this.submitting) {
      return;
    }
    const question = this.currentQuestion;
    if (!question?.questionId || !this.answers[question.questionId]) {
      this.message.warning('Veuillez choisir une réponse avant de continuer');
      return;
    }
    if (this.currentQuestionIndex < this.totalQuestions - 1) {
      this.currentQuestionIndex++;
      return;
    }
    this.stopQuizMonitoring();
    this.currentStep = 2;
  }

  submit(): void {
    this.submitApplication(false);
  }

  blockClipboard(event: Event): void {
    if (this.currentStep === 1 && !this.qcmViolated && !this.submitting) {
      event.preventDefault();
      this.handleQuizViolation('Tentative de copier-coller détectée');
    }
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (this.currentStep !== 1 || this.qcmViolated || this.submitting) {
      return;
    }
    const key = event.key.toLowerCase();
    const ctrl = event.ctrlKey || event.metaKey;
    if (ctrl && (key === 'c' || key === 'v' || key === 'x' || key === 'a')) {
      event.preventDefault();
      this.handleQuizViolation('Tentative de copier-coller détectée');
    }
  }

  @HostListener('window:blur')
  onWindowBlur(): void {
    if (!this.quizMonitoring || this.qcmViolated || this.submitting) {
      return;
    }
    this.handleQuizViolation('Vous avez quitté la fenêtre du test');
  }

  private startQuizMonitoring(): void {
    if (this.quizMonitoring || this.totalQuestions === 0) {
      return;
    }
    this.quizMonitoring = true;
    this.quizBaselineWidth = window.innerWidth;
    this.quizBaselineHeight = window.innerHeight;
    document.addEventListener('visibilitychange', this.boundVisibilityHandler);
    window.addEventListener('resize', this.boundResizeHandler);
  }

  private stopQuizMonitoring(): void {
    if (!this.quizMonitoring) {
      return;
    }
    this.quizMonitoring = false;
    document.removeEventListener('visibilitychange', this.boundVisibilityHandler);
    window.removeEventListener('resize', this.boundResizeHandler);
  }

  private onVisibilityChange(): void {
    if (!this.quizMonitoring || this.qcmViolated || this.submitting) {
      return;
    }
    if (document.visibilityState === 'hidden') {
      this.handleQuizViolation('Changement d\'onglet ou de fenêtre détecté');
    }
  }

  private onWindowResize(): void {
    if (!this.quizMonitoring || this.qcmViolated || this.submitting) {
      return;
    }
    const tooSmall = window.innerWidth < this.quizBaselineWidth * this.minSizeRatio
      || window.innerHeight < this.quizBaselineHeight * this.minSizeRatio;
    if (tooSmall) {
      this.handleQuizViolation('Réduction de la fenêtre détectée');
    }
  }

  private handleQuizViolation(reason: string): void {
    if (this.qcmViolated || this.submitting) {
      return;
    }
    this.qcmViolated = true;
    this.violationMessage = reason;
    this.stopQuizMonitoring();
    this.submitApplication(true);
  }

  private submitApplication(violated: boolean): void {
    if (!this.recruitment || !this.cvFileUrl) {
      return;
    }
    if (this.submitting && !violated) {
      return;
    }

    const answerList: QcmAnswer[] = this.questions
      .filter((q) => q.questionId)
      .map((q) => ({
        questionId: q.questionId!,
        selectedOption: this.answers[q.questionId!] || '-'
      }));

    this.submitting = true;
    this.recruitmentService.apply({
      recruitmentId: this.recruitment.recruitmentId,
      cvFileUrl: this.cvFileUrl,
      answers: answerList,
      qcmViolated: violated
    }).subscribe({
      next: (response) => {
        this.submitting = false;
        if (response.success) {
          if (violated) {
            setTimeout(() => this.router.navigate(['/jobs/applications']), 2500);
          } else {
            this.message.success('Candidature envoyée avec succès');
            this.router.navigate(['/jobs/applications']);
          }
        }
      },
      error: (err) => {
        this.submitting = false;
        this.qcmViolated = false;
        this.violationMessage = '';
        this.message.error(err.error?.message || 'Échec de l\'envoi de la candidature');
      }
    });
  }

  back(): void {
    if (this.qcmViolated || this.submitting) {
      return;
    }
    if (this.recruitment) {
      this.stopQuizMonitoring();
      this.router.navigate(['/jobs', this.recruitment.recruitmentId]);
    }
  }
}
