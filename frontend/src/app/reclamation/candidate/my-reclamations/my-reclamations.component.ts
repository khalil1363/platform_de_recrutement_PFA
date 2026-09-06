import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { ReclamationService } from '../../../reclamation/services/reclamation.service';
import { Reclamation, ReclamationStatus } from '../../../reclamation/models/reclamation.model';

@Component({
  selector: 'app-my-reclamations',
  templateUrl: './my-reclamations.component.html',
  styleUrl: './my-reclamations.component.css'
})
export class MyReclamationsComponent implements OnInit {
  reclamations: Reclamation[] = [];
  loading = false;
  modalVisible = false;
  modalLoading = false;
  form!: FormGroup;

  constructor(
    private readonly reclamationService: ReclamationService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.maxLength(4000)]]
    });
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.reclamationService.getMine().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.reclamations = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Impossible de charger vos réclamations');
      }
    });
  }

  openCreate(): void {
    this.form.reset();
    this.modalVisible = true;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.modalLoading = true;
    this.reclamationService.create(this.form.value).subscribe({
      next: (response) => {
        this.modalLoading = false;
        if (response.success) {
          this.message.success('Réclamation envoyée');
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

  delete(item: Reclamation): void {
    this.reclamationService.delete(item.reclamationId).subscribe({
      next: (response) => {
        if (response.success) {
          this.message.success('Réclamation supprimée');
          this.loadData();
        }
      },
      error: (err) => {
        this.message.error(err.error?.message || 'Suppression impossible');
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
