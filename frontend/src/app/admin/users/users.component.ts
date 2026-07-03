import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../models/auth.model';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrl: './users.component.css'
})
export class UsersComponent implements OnInit {
  users: UserProfile[] = [];
  loading = false;
  isModalVisible = false;
  createLoading = false;
  createForm!: FormGroup;

  readonly roleOptions = [
    { label: 'RH', value: 'ROLE_RH' },
    { label: 'Développeur', value: 'ROLE_DEVELOPER' },
    { label: 'Utilisateur', value: 'ROLE_USER' },
    { label: 'Admin', value: 'ROLE_ADMIN' }
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.authService.getAllUsers().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.users = response.data;
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur lors du chargement des utilisateurs');
      }
    });
  }

  openCreateModal(): void {
    this.createForm.reset({ role: 'ROLE_RH' });
    this.isModalVisible = true;
  }

  closeCreateModal(): void {
    this.isModalVisible = false;
  }

  onCreateUser(): void {
    if (this.createForm.invalid) {
      Object.values(this.createForm.controls).forEach((control) => {
        control.markAsDirty();
        control.updateValueAndValidity();
      });
      return;
    }

    this.createLoading = true;
    this.authService.createUser(this.createForm.value).subscribe({
      next: (response) => {
        this.createLoading = false;
        if (response.success) {
          this.message.success('Utilisateur créé avec succès');
          this.isModalVisible = false;
          this.loadUsers();
        }
      },
      error: (error) => {
        this.createLoading = false;
        this.message.error(error.error?.message || 'Erreur lors de la création');
      }
    });
  }

  onStatusChange(user: UserProfile, active: boolean): void {
    this.authService.updateUserStatus(user.userId, active).subscribe({
      next: (response) => {
        if (response.success) {
          user.active = active;
          this.message.success(active ? 'Compte activé' : 'Compte désactivé');
        }
      },
      error: (error) => {
        this.message.error(error.error?.message || 'Erreur lors de la mise à jour');
        this.loadUsers();
      }
    });
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      ROLE_ADMIN: 'Admin',
      ROLE_RH: 'RH',
      ROLE_DEVELOPER: 'Développeur',
      ROLE_USER: 'Candidat'
    };
    return labels[role] || role;
  }

  getRoleColor(role: string): string {
    const colors: Record<string, string> = {
      ROLE_ADMIN: 'red',
      ROLE_RH: 'green',
      ROLE_DEVELOPER: 'blue',
      ROLE_USER: 'default'
    };
    return colors[role] || 'default';
  }

  private buildForm(): void {
    this.createForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: ['ROLE_RH', [Validators.required]],
      phoneNumber: [''],
      address: ['']
    });
  }
}
