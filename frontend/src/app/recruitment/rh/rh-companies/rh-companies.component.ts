import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../services/recruitment.service';
import { Company } from '../../models/recruitment.model';

@Component({
  selector: 'app-rh-companies',
  templateUrl: './rh-companies.component.html',
  styleUrl: './rh-companies.component.css'
})
export class RhCompaniesComponent implements OnInit {
  companies: Company[] = [];
  zones: { zoneId: string; name: string }[] = [];
  loading = false;
  modalVisible = false;
  modalLoading = false;
  companyForm!: FormGroup;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.companyForm = this.fb.group({
      name: ['', Validators.required],
      zoneId: ['', Validators.required],
      address: ['']
    });
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.recruitmentService.getCompanies().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.companies = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des entreprises');
      }
    });
    this.recruitmentService.getZones().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.zones = response.data;
        }
      }
    });
  }

  openModal(): void {
    this.companyForm.reset();
    this.modalVisible = true;
  }

  saveCompany(): void {
    if (this.companyForm.invalid) {
      return;
    }
    this.modalLoading = true;
    this.recruitmentService.createCompany(this.companyForm.value).subscribe({
      next: (response) => {
        this.modalLoading = false;
        if (response.success) {
          this.message.success('Entreprise créée');
          this.modalVisible = false;
          this.loadData();
        }
      },
      error: (err) => {
        this.modalLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de la création');
      }
    });
  }
}
