import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { ReclamationService } from '../../../reclamation/services/reclamation.service';
import { Reclamation, ReclamationStatus } from '../../../reclamation/models/reclamation.model';

@Component({
  selector: 'app-rh-reclamations',
  templateUrl: './rh-reclamations.component.html',
  styleUrl: './rh-reclamations.component.css'
})
export class RhReclamationsComponent implements OnInit {
  reclamations: Reclamation[] = [];
  loading = false;
  modalVisible = false;
  modalLoading = false;
  selected: Reclamation | null = null;
  form!: FormGroup;
  readonly statuses: ReclamationStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  constructor(
    private readonly reclamationService: ReclamationService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      status: [null as ReclamationStatus | null, Validators.required],
      rhResponse: ['']
    });
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reclamationService.getAll().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.reclamations = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Impossible de charger les réclamations');
      }
    });
  }

  openTreat(item: Reclamation): void {
    this.selected = item;
    this.form.reset({
      status: item.status,
      rhResponse: item.rhResponse || ''
    });
    this.modalVisible = true;
  }

  save(): void {
    if (!this.selected || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.modalLoading = true;
    this.reclamationService
      .updateStatus(this.selected.reclamationId, {
        status: this.form.value.status,
        rhResponse: this.form.value.rhResponse || undefined
      })
      .subscribe({
        next: (response) => {
          this.modalLoading = false;
          if (response.success) {
            this.message.success('Réclamation mise à jour');
            this.modalVisible = false;
            this.loadData();
          }
        },
        error: (err) => {
          this.modalLoading = false;
          this.message.error(err.error?.message || 'Erreur de mise à jour');
        }
      });
  }

  statusLabel(status: ReclamationStatus): string {
    const labels: Record<ReclamationStatus, string> = {
      OPEN: 'Ouverte',
      IN_PROGRESS: 'En cours',
      RESOLVED: 'Résolue',
      CLOSED: 'Clôturée'
    };
    return labels[status] || status;
  }

  statusColor(status: ReclamationStatus): string {
    if (status === 'OPEN') return 'blue';
    if (status === 'IN_PROGRESS') return 'orange';
    if (status === 'RESOLVED') return 'green';
    return 'default';
  }
}
