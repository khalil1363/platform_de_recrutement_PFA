import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { Company, RecruitmentRequest } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-recruitment-form',
  templateUrl: './rh-recruitment-form.component.html',
  styleUrl: './rh-recruitment-form.component.css'
})
export class RhRecruitmentFormComponent implements OnInit {
  form!: FormGroup;
  companies: Company[] = [];
  loading = false;
  saving = false;
  isEdit = false;
  recruitmentId = '';

  readonly statusOptions = [
    { label: 'Brouillon', value: 'DRAFT' },
    { label: 'Publié', value: 'PUBLISHED' },
    { label: 'Fermé', value: 'CLOSED' }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recruitmentService: RecruitmentService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadCompanies();
    this.recruitmentId = this.route.snapshot.paramMap.get('id') || '';
    this.isEdit = !!this.recruitmentId && this.route.snapshot.url.some((s) => s.path === 'edit');
    if (this.isEdit) {
      this.loadRecruitment();
    }
  }

  get questions(): FormArray {
    return this.form.get('questions') as FormArray;
  }

  buildForm(): void {
    this.form = this.fb.group({
      title: ['', Validators.required],
      companyId: ['', Validators.required],
      description: [''],
      responsibilities: [''],
      technicalSkills: [''],
      personalSkills: [''],
      educationRequirements: [''],
      experienceRequirements: [''],
      jobType: [''],
      availability: ['Plein temps'],
      salaryMin: [null],
      salaryMax: [null],
      salaryPeriod: ['Mois'],
      educationLevel: [''],
      experienceLevel: [''],
      country: ['Tunisie'],
      region: [''],
      city: [''],
      languages: [[]],
      drivingLicenseRequired: [false],
      localTravel: [false],
      internationalTravel: [false],
      anonymousMode: [false],
      responsibleName: [''],
      internalReference: [''],
      keejobReference: [''],
      status: ['DRAFT'],
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

  loadCompanies(): void {
    this.recruitmentService.getCompanies().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.companies = response.data;
        }
      }
    });
  }

  loadRecruitment(): void {
    this.loading = true;
    this.recruitmentService.getRecruitment(this.recruitmentId).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          const data = response.data;
          this.form.patchValue({
            title: data.title,
            companyId: data.companyId,
            description: data.description,
            responsibilities: data.responsibilities,
            technicalSkills: data.technicalSkills,
            personalSkills: data.personalSkills,
            educationRequirements: data.educationRequirements,
            experienceRequirements: data.experienceRequirements,
            jobType: data.jobType,
            availability: data.availability,
            salaryMin: data.salaryMin,
            salaryMax: data.salaryMax,
            salaryPeriod: data.salaryPeriod,
            educationLevel: data.educationLevel,
            experienceLevel: data.experienceLevel,
            country: data.country,
            region: data.region,
            city: data.city,
            languages: data.languages || [],
            drivingLicenseRequired: data.drivingLicenseRequired,
            localTravel: data.localTravel,
            internationalTravel: data.internationalTravel,
            anonymousMode: data.anonymousMode,
            responsibleName: data.responsibleName,
            internalReference: data.internalReference,
            keejobReference: data.keejobReference,
            status: data.status
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
        this.message.error('Recrutement introuvable');
        this.router.navigate(['/rh/recruitments']);
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.form.value as RecruitmentRequest;
    this.saving = true;
    const request$ = this.isEdit
      ? this.recruitmentService.updateRecruitment(this.recruitmentId, payload)
      : this.recruitmentService.createRecruitment(payload);

    request$.subscribe({
      next: (response) => {
        this.saving = false;
        if (response.success) {
          this.message.success(this.isEdit ? 'Recrutement mis à jour' : 'Recrutement créé');
          this.router.navigate(['/rh/recruitments']);
        }
      },
      error: (err) => {
        this.saving = false;
        this.message.error(err.error?.message || 'Erreur lors de l\'enregistrement');
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/rh/recruitments']);
  }
}
