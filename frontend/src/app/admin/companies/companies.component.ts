import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../recruitment/services/recruitment.service';
import { Company, Zone } from '../../recruitment/models/recruitment.model';

interface ZoneCompaniesGroup {
  zoneId: string;
  zoneName: string;
  companies: Company[];
}

@Component({
  selector: 'app-companies',
  templateUrl: './companies.component.html',
  styleUrl: './companies.component.css'
})
export class CompaniesComponent implements OnInit {
  companies: Company[] = [];
  groupedCompanies: ZoneCompaniesGroup[] = [];
  zones: Zone[] = [];
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
          this.groupCompanies();
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

  private groupCompanies(): void {
    const grouped = new Map<string, ZoneCompaniesGroup>();
    for (const company of this.companies) {
      if (!grouped.has(company.zoneId)) {
        grouped.set(company.zoneId, {
          zoneId: company.zoneId,
          zoneName: company.zoneName || 'Zone',
          companies: []
        });
      }
      grouped.get(company.zoneId)!.companies.push(company);
    }
    this.groupedCompanies = Array.from(grouped.values()).sort((a, b) => a.zoneName.localeCompare(b.zoneName));
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
