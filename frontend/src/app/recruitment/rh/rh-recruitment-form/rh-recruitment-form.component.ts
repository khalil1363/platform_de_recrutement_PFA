import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { Company, Qcm, RecruitmentRequest } from '../../models/recruitment.model';
import { JOB_TITLES } from '../../constants/job-titles';

@Component({
  selector: 'app-rh-recruitment-form',
  templateUrl: './rh-recruitment-form.component.html',
  styleUrl: './rh-recruitment-form.component.css'
})
export class RhRecruitmentFormComponent implements OnInit {
  form!: FormGroup;
  companies: Company[] = [];
  qcms: Qcm[] = [];
  jobTitles: string[] = [...JOB_TITLES];
  loading = false;
  saving = false;
  isEdit = false;
  recruitmentId = '';

  readonly statusOptions = [
    { label: 'Brouillon', value: 'DRAFT' },
    { label: 'Publié', value: 'PUBLISHED' },
    { label: 'Fermé', value: 'CLOSED' }
  ];

  readonly availabilityOptions = [
    'Plein temps',
    'Temps partiel'
  ];

  readonly educationLevelOptions = [
    'Bac',
    'Bac+2',
    'Bac+3',
    'Bac+4',
    'Bac+5',
    'Doctorat',
    'Non précisé'
  ];

  readonly experienceLevelOptions = [
    'Débutant / Junior',
    '1-2 ans',
    '3-5 ans',
    '5-10 ans',
    'Plus de 10 ans',
    'Confirmé / Senior',
    'Non précisé'
  ];

  readonly languageOptions = ['Arabe', 'Français', 'Anglais', 'Allemand', 'Italien', 'Espagnol'];

  readonly salaryPeriodOptions = ['Mois', 'An', 'Jour', 'Heure'];

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
    this.loadQcms();
    this.loadJobTitles();
    this.recruitmentId = this.route.snapshot.paramMap.get('id') || '';
    this.isEdit = !!this.recruitmentId && this.route.snapshot.url.some((s) => s.path === 'edit');
    if (this.isEdit) {
      this.loadRecruitment();
    }
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
      qcmId: [null],
      coworking: [false],
      coworkingMonth: [null]
    });

    this.form.get('coworking')?.valueChanges.subscribe((enabled: boolean) => {
      const monthControl = this.form.get('coworkingMonth');
      if (enabled) {
        monthControl?.setValidators([Validators.required]);
      } else {
        monthControl?.clearValidators();
        monthControl?.setValue(null);
      }
      monthControl?.updateValueAndValidity();
    });
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

  loadQcms(): void {
    this.recruitmentService.getQcms().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.qcms = response.data;
        }
      }
    });
  }

  loadJobTitles(): void {
    this.recruitmentService.getJobTitles().subscribe({
      next: (response) => {
        if (response.success && response.data?.length) {
          this.jobTitles = response.data;
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
          if (data.title && !this.jobTitles.includes(data.title)) {
            this.jobTitles = [data.title, ...this.jobTitles];
          }
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
            status: data.status,
            qcmId: data.qcmId || null,
            coworking: !!data.coworking,
            coworkingMonth: data.coworkingMonth ? new Date(data.coworkingMonth) : null
          });
          if (data.coworking) {
            this.form.get('coworkingMonth')?.setValidators([Validators.required]);
            this.form.get('coworkingMonth')?.updateValueAndValidity();
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
    const raw = { ...this.form.value };
    const payload = { ...raw } as RecruitmentRequest;
    if (!payload.qcmId) {
      payload.qcmId = null;
    }
    payload.coworking = !!raw.coworking;
    if (payload.coworking && raw.coworkingMonth) {
      const d = raw.coworkingMonth instanceof Date ? raw.coworkingMonth : new Date(raw.coworkingMonth);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      payload.coworkingMonth = `${y}-${m}-01`;
    } else {
      payload.coworkingMonth = null;
    }
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

  goToQcm(): void {
    this.router.navigate(['/rh/qcm/new']);
  }

  cancel(): void {
    this.router.navigate(['/rh/recruitments']);
  }
}
