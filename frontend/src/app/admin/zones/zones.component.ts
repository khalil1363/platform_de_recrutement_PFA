import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../recruitment/services/recruitment.service';
import { Zone } from '../../recruitment/models/recruitment.model';

@Component({
  selector: 'app-zones',
  templateUrl: './zones.component.html',
  styleUrl: './zones.component.css'
})
export class ZonesComponent implements OnInit {
  zones: Zone[] = [];
  loading = false;
  zoneModalVisible = false;
  modalLoading = false;
  zoneForm!: FormGroup;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.zoneForm = this.fb.group({
      name: ['', Validators.required],
      description: ['']
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
  }

  openZoneModal(): void {
    this.zoneForm.reset();
    this.zoneModalVisible = true;
  }

  saveZone(): void {
    if (this.zoneForm.invalid) {
      return;
    }
    this.modalLoading = true;
    this.recruitmentService.createZone(this.zoneForm.value).subscribe({
      next: (response) => {
        this.modalLoading = false;
        if (response.success) {
          this.message.success('Zone créée');
          this.zoneModalVisible = false;
          this.loadData();
        }
      },
      error: (err) => {
        this.modalLoading = false;
        if (err.status === 503) {
          this.message.error('Service recrutement indisponible. Vérifiez que RECRUITMENT (port 8090) est démarré.');
          return;
        }
        this.message.error(err.error?.message || 'Erreur lors de la création');
      }
    });
  }
}
