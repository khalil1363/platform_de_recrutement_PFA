import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { HiredQcmAssignment, QcmAnswer, QcmQuestion } from '../../models/recruitment.model';

@Component({
  selector: 'app-take-evaluation',
  templateUrl: './take-evaluation.component.html',
  styleUrl: './take-evaluation.component.css'
})
export class TakeEvaluationComponent implements OnInit, OnDestroy {
  assignment: HiredQcmAssignment | null = null;
  loading = false;
  submitting = false;
  currentStep = 0;
  currentQuestionIndex = 0;
  answers: Record<string, string> = {};
  qcmViolated = false;
  violationMessage = '';
  quizMonitoring = false;
  result: HiredQcmAssignment | null = null;

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
    const id = this.route.snapshot.paramMap.get('assignmentId');
    if (!id) {
      this.router.navigate(['/jobs/evaluations']);
      return;
    }
    this.load(id);
  }

  ngOnDestroy(): void {
    this.stopQuizMonitoring();
  }

  get questions(): QcmQuestion[] {
    return this.assignment?.questions || [];
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

  load(id: string): void {
    this.loading = true;
    this.recruitmentService.getHiredQcmAssignment(id).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.assignment = response.data;
          if (this.assignment.status !== 'ASSIGNED') {
            this.result = this.assignment;
            this.message.info('Cette évaluation est déjà terminée');
            return;
          }
          if (!this.questions.length) {
            this.message.error('Ce QCM ne contient aucune question');
            this.router.navigate(['/jobs/evaluations']);
          }
        }
      },
      error: (err) => {
        this.loading = false;
        this.message.error(err.error?.message || 'Évaluation introuvable');
        this.router.navigate(['/jobs/evaluations']);
      }
    });
  }

  startQuiz(): void {
    if (this.qcmViolated || this.submitting || !this.questions.length) {
      return;
    }
    this.currentStep = 1;
    this.currentQuestionIndex = 0;
    this.startQuizMonitoring();
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

  prevQuestion(): void {
    if (this.qcmViolated || this.submitting) {
      return;
    }
    if (this.currentQuestionIndex > 0) {
      this.currentQuestionIndex--;
    }
  }

  submit(): void {
    this.submitAnswers(false);
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

  back(): void {
    if (this.qcmViolated || this.submitting) {
      return;
    }
    this.stopQuizMonitoring();
    this.router.navigate(['/jobs/evaluations']);
  }

  goToEvaluations(): void {
    this.router.navigate(['/jobs/evaluations']);
  }

  resumeQuizFromReview(): void {
    this.currentStep = 1;
    this.currentQuestionIndex = Math.max(0, this.totalQuestions - 1);
    this.startQuizMonitoring();
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
    this.submitAnswers(true);
  }

  private submitAnswers(violated: boolean): void {
    if (!this.assignment) {
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
    this.recruitmentService.submitHiredQcm(this.assignment.assignmentId, {
      answers: answerList,
      qcmViolated: violated
    }).subscribe({
      next: (response) => {
        this.submitting = false;
        if (response.success && response.data) {
          this.result = response.data;
          this.currentStep = 3;
          if (!violated) {
            this.message.success('Évaluation envoyée');
          }
        }
      },
      error: (err) => {
        this.submitting = false;
        this.qcmViolated = false;
        this.violationMessage = '';
        this.message.error(err.error?.message || 'Échec de l\'envoi de l\'évaluation');
      }
    });
  }
}
