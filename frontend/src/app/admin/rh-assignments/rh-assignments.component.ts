import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../core/services/auth.service';
import { RecruitmentService } from '../../recruitment/services/recruitment.service';
import { UserProfile } from '../../models/auth.model';
import { Zone } from '../../recruitment/models/recruitment.model';

@Component({
  selector: 'app-rh-assignments',
  templateUrl: './rh-assignments.component.html',
  styleUrl: './rh-assignments.component.css'
})
export class RhAssignmentsComponent implements OnInit {
  zones: Zone[] = [];
  rhUsers: UserProfile[] = [];
  loading = false;
  modalLoading = false;
  assignForm!: FormGroup;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly authService: AuthService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.assignForm = this.fb.group({
      rhUserId: ['', Validators.required],
      zoneId: ['', Validators.required]
    });
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.recruitmentService.getZones().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.zones = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des zones');
      }
    });
    this.authService.getAllUsers().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.rhUsers = response.data.filter((u) => u.role === 'ROLE_RH');
        }
      }
    });
  }

  assignRh(): void {
    if (this.assignForm.invalid) {
      this.assignForm.markAllAsTouched();
      return;
    }
    this.modalLoading = true;
    this.recruitmentService.assignRhToZone(this.assignForm.value).subscribe({
      next: (response) => {
        this.modalLoading = false;
        if (response.success) {
          this.message.success('RH assigné à la zone');
          this.assignForm.reset();
        }
      },
      error: (err) => {
        this.modalLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de l\'assignation');
      }
    });
  }
}
