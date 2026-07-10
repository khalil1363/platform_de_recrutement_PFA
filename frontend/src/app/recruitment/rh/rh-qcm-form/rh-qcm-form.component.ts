import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { QcmRequest } from '../../models/recruitment.model';

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

  createQuestionGroup(): FormGroup {
    return this.fb.group({
      questionText: ['', Validators.required],
      optionA: ['', Validators.required],
      optionB: ['', Validators.required],
      optionC: [''],
      optionD: [''],
      correctOption: ['A', Validators.required]
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
            this.questions.push(this.fb.group({
              questionText: [q.questionText, Validators.required],
              optionA: [q.optionA, Validators.required],
              optionB: [q.optionB, Validators.required],
              optionC: [q.optionC || ''],
              optionD: [q.optionD || ''],
              correctOption: [q.correctOption || 'A', Validators.required]
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
    const payload = this.form.value as QcmRequest;
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
