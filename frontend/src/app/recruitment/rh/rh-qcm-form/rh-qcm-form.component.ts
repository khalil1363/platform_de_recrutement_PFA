import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { QcmRequest } from '../../models/recruitment.model';
import { PSY_CC_DIMENSION_OPTIONS, PSY_CC_SAMPLE_QUESTIONS } from '../../data/psy-cc-sample-qcm';

@Component({
  selector: 'app-rh-qcm-form',
  templateUrl: './rh-qcm-form.component.html',
  styleUrl: './rh-qcm-form.component.css'
})
export class RhQcmFormComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  saving = false;
  isEdit = false;
  qcmId = '';
  readonly dimensionOptions = PSY_CC_DIMENSION_OPTIONS;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.qcmId = this.route.snapshot.paramMap.get('id') || '';
    this.isEdit = !!this.qcmId && this.route.snapshot.url.some((s) => s.path === 'edit');
    if (this.isEdit) {
      this.loadQcm();
    }
  }

  get questions(): FormArray {
    return this.form.get('questions') as FormArray;
  }

  buildForm(): void {
    this.form = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      questions: this.fb.array([this.createQuestionGroup()])
    });
  }

  createQuestionGroup(data?: Partial<{
    questionText: string;
    optionA: string;
    optionB: string;
    optionC: string;
    optionD: string;
    correctOption: string;
    dimensionCode: string | null;
    scoreA: number | null;
    scoreB: number | null;
    scoreC: number | null;
    scoreD: number | null;
  }>): FormGroup {
    return this.fb.group({
      questionText: [data?.questionText || '', Validators.required],
      optionA: [data?.optionA || '', Validators.required],
      optionB: [data?.optionB || '', Validators.required],
      optionC: [data?.optionC || ''],
      optionD: [data?.optionD || ''],
      correctOption: [data?.correctOption || 'A', Validators.required],
      dimensionCode: [data?.dimensionCode || null],
      scoreA: [data?.scoreA ?? null],
      scoreB: [data?.scoreB ?? null],
      scoreC: [data?.scoreC ?? null],
      scoreD: [data?.scoreD ?? null]
    });
  }

  addQuestion(): void {
    this.questions.push(this.createQuestionGroup());
  }

  removeQuestion(index: number): void {
    if (this.questions.length > 1) {
      this.questions.removeAt(index);
    }
  }

  loadPsySample(): void {
    this.form.patchValue({
      title: 'Test PSY — Profil Commercial / Chargé de crédit',
      description:
        'Questionnaire psychométrique post-embauche (18 compétences). Réponses Likert scorées pour rapports PDF / Excel.'
    });
    this.questions.clear();
    PSY_CC_SAMPLE_QUESTIONS.forEach((q) => {
      this.questions.push(this.createQuestionGroup({ ...q }));
    });
    this.message.success('Modèle PSY CC chargé — enregistrez puis assignez-le au candidat admis');
  }

  loadQcm(): void {
    this.loading = true;
    this.recruitmentService.getQcm(this.qcmId).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          const data = response.data;
          this.form.patchValue({
            title: data.title,
            description: data.description || ''
          });
          this.questions.clear();
          (data.questions || []).forEach((q) => {
            this.questions.push(this.createQuestionGroup({
              questionText: q.questionText,
              optionA: q.optionA,
              optionB: q.optionB,
              optionC: q.optionC || '',
              optionD: q.optionD || '',
              correctOption: q.correctOption || 'A',
              dimensionCode: q.dimensionCode || null,
              scoreA: q.scoreA ?? null,
              scoreB: q.scoreB ?? null,
              scoreC: q.scoreC ?? null,
              scoreD: q.scoreD ?? null
            }));
          });
          if (!this.questions.length) {
            this.addQuestion();
          }
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('QCM introuvable');
        this.router.navigate(['/rh/qcm']);
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.value;
    const payload: QcmRequest = {
      title: raw.title,
      description: raw.description,
      questions: (raw.questions || []).map((q: Record<string, unknown>, index: number) => ({
        ...q,
        orderIndex: index,
        scoreA: q['scoreA'] === null || q['scoreA'] === '' ? undefined : Number(q['scoreA']),
        scoreB: q['scoreB'] === null || q['scoreB'] === '' ? undefined : Number(q['scoreB']),
        scoreC: q['scoreC'] === null || q['scoreC'] === '' ? undefined : Number(q['scoreC']),
        scoreD: q['scoreD'] === null || q['scoreD'] === '' ? undefined : Number(q['scoreD']),
        dimensionCode: q['dimensionCode'] || undefined
      }))
    };
    this.saving = true;
    const request$ = this.isEdit
      ? this.recruitmentService.updateQcm(this.qcmId, payload)
      : this.recruitmentService.createQcm(payload);

    request$.subscribe({
      next: (response) => {
        this.saving = false;
        if (response.success) {
          this.message.success(this.isEdit ? 'QCM mis à jour' : 'QCM créé');
          this.router.navigate(['/rh/qcm']);
        }
      },
      error: (err) => {
        this.saving = false;
        this.message.error(err.error?.message || 'Erreur lors de l\'enregistrement');
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/rh/qcm']);
  }
}
