import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../core/services/auth.service';
import { RecruitmentService } from '../../recruitment/services/recruitment.service';
import { UserProfile } from '../../models/auth.model';
import { RhZoneAssignment, Zone } from '../../recruitment/models/recruitment.model';

interface RhAssignmentGroup {
  rhUserId: string;
  rhLabel: string;
  zones: string[];
}

@Component({
  selector: 'app-rh-assignments',
  templateUrl: './rh-assignments.component.html',
  styleUrl: './rh-assignments.component.css'
})
export class RhAssignmentsComponent implements OnInit {
  zones: Zone[] = [];
  rhUsers: UserProfile[] = [];
  assignments: RhZoneAssignment[] = [];
  groupedAssignments: RhAssignmentGroup[] = [];
  availableZones: Zone[] = [];
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
      zoneIds: [[], Validators.required]
    });
    this.assignForm.get('rhUserId')?.valueChanges.subscribe(() => this.updateAvailableZones());
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.recruitmentService.getZones().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.zones = response.data;
          this.updateAvailableZones();
        }
      },
      error: () => {
        this.message.error('Erreur de chargement des zones');
      }
    });
    this.recruitmentService.getRhZoneAssignments().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.assignments = response.data;
          this.groupAssignments();
          this.updateAvailableZones();
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des affectations RH');
      }
    });
    this.authService.getAllUsers().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.rhUsers = response.data.filter((u) => u.role === 'ROLE_RH');
          this.groupAssignments();
        }
      }
    });
  }

  private groupAssignments(): void {
    const grouped = new Map<string, RhAssignmentGroup>();
    for (const assignment of this.assignments) {
      if (!grouped.has(assignment.rhUserId)) {
        grouped.set(assignment.rhUserId, {
          rhUserId: assignment.rhUserId,
          rhLabel: this.getRhLabel(assignment.rhUserId),
          zones: []
        });
      }
      grouped.get(assignment.rhUserId)!.zones.push(assignment.zoneName);
    }
    this.groupedAssignments = Array.from(grouped.values()).sort((a, b) => a.rhLabel.localeCompare(b.rhLabel));
  }

  private updateAvailableZones(): void {
    const rhUserId = this.assignForm.get('rhUserId')?.value;
    if (!rhUserId) {
      this.availableZones = [];
      this.assignForm.patchValue({ zoneIds: [] }, { emitEvent: false });
      return;
    }

    const zonesAlreadyUsedByOthers = new Set(
      this.assignments
        .filter((assignment) => assignment.rhUserId !== rhUserId)
        .map((assignment) => assignment.zoneId)
    );
    const zonesAlreadyAssignedToRh = new Set(
      this.assignments
        .filter((assignment) => assignment.rhUserId === rhUserId)
        .map((assignment) => assignment.zoneId)
    );

    this.availableZones = this.zones.filter(
      (zone) => !zonesAlreadyUsedByOthers.has(zone.zoneId) && !zonesAlreadyAssignedToRh.has(zone.zoneId)
    );

    const selectedZoneIds: string[] = this.assignForm.get('zoneIds')?.value || [];
    this.assignForm.patchValue(
      { zoneIds: selectedZoneIds.filter((zoneId) => this.availableZones.some((zone) => zone.zoneId === zoneId)) },
      { emitEvent: false }
    );
  }

  getRhLabel(rhUserId: string): string {
    const rh = this.rhUsers.find((user) => user.userId === rhUserId);
    return rh ? `${rh.firstName} ${rh.lastName} (${rh.username})` : rhUserId;
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
          this.message.success('Zones affectées au RH');
          this.assignForm.patchValue({ zoneIds: [] });
          this.loadData();
        }
      },
      error: (err) => {
        this.modalLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de l\'assignation');
      }
    });
  }
}
