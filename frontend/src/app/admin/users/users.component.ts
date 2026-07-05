import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadFile, NzUploadXHRArgs } from 'ng-zorro-antd/upload';
import { Subscription } from 'rxjs';
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
  isEditMode = false;
  modalLoading = false;
  imageUploading = false;
  profileImagePreview: string | null = null;
  selectedUser: UserProfile | null = null;
  userForm!: FormGroup;

  readonly roleOptions = [
    { label: 'RH', value: 'ROLE_RH' },
    { label: 'Développeur', value: 'ROLE_DEVELOPER' },
    { label: 'Candidat', value: 'ROLE_USER' },
    { label: 'Admin', value: 'ROLE_ADMIN' }
  ];

  constructor(
    readonly authService: AuthService,
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
    this.isEditMode = false;
    this.selectedUser = null;
    this.profileImagePreview = null;
    this.userForm.reset({ role: 'ROLE_RH' });
    this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
    this.userForm.get('password')?.updateValueAndValidity();
    this.isModalVisible = true;
  }

  openEditModal(user: UserProfile): void {
    this.isEditMode = true;
    this.selectedUser = user;
    this.profileImagePreview = this.authService.resolveImageUrl(user.profileImageUrl);
    this.userForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
      email: user.email,
      role: user.role,
      phoneNumber: user.phoneNumber || '',
      address: user.address || '',
      profileImageUrl: user.profileImageUrl || '',
      password: ''
    });
    this.userForm.get('password')?.setValidators([Validators.minLength(6)]);
    this.userForm.get('password')?.updateValueAndValidity();
    this.isModalVisible = true;
  }

  closeModal(): void {
    this.isModalVisible = false;
  }

  onSubmitUser(): void {
    if (this.userForm.invalid) {
      Object.values(this.userForm.controls).forEach((control) => {
        control.markAsDirty();
        control.updateValueAndValidity();
      });
      return;
    }

    const raw = { ...this.userForm.value };
    if (!raw.phoneNumber) delete raw.phoneNumber;
    if (!raw.address) delete raw.address;
    if (!raw.profileImageUrl) delete raw.profileImageUrl;
    if (!raw.password) delete raw.password;

    this.modalLoading = true;

    if (this.isEditMode && this.selectedUser) {
      const { username, ...updatePayload } = raw;
      this.authService.updateUser(this.selectedUser.userId, updatePayload).subscribe({
        next: (response) => {
          this.modalLoading = false;
          if (response.success) {
            this.message.success('Utilisateur mis à jour');
            this.isModalVisible = false;
            this.loadUsers();
          }
        },
        error: (error: HttpErrorResponse) => {
          this.modalLoading = false;
          this.message.error(error.error?.message || 'Erreur lors de la mise à jour');
        }
      });
      return;
    }

    this.authService.createUser(raw).subscribe({
      next: (response) => {
        this.modalLoading = false;
        if (response.success) {
          this.message.success('Utilisateur créé avec succès');
          this.isModalVisible = false;
          this.loadUsers();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.modalLoading = false;
        this.message.error(error.error?.message || 'Erreur lors de la création');
      }
    });
  }

  onDeleteUser(user: UserProfile): void {
    this.authService.deleteUser(user.userId).subscribe({
      next: (response) => {
        if (response.success) {
          this.message.success('Utilisateur supprimé');
          this.loadUsers();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.message.error(error.error?.message || 'Erreur lors de la suppression');
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
      error: (error: HttpErrorResponse) => {
        this.message.error(error.error?.message || 'Erreur lors de la mise à jour');
        this.loadUsers();
      }
    });
  }

  handleProfileUpload = (item: NzUploadXHRArgs): Subscription => {
    const file = item.file as unknown as File;
    this.imageUploading = true;

    return this.authService.uploadProfileImage(file).subscribe({
      next: (response) => {
        this.imageUploading = false;
        if (response.success && response.data) {
          this.userForm.patchValue({ profileImageUrl: response.data.profileImageUrl });
          this.profileImagePreview = this.authService.resolveImageUrl(response.data.profileImageUrl);
          item.onSuccess?.(response, item.file, null);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.imageUploading = false;
        item.onError?.(error, item.file);
      }
    });
  };

  beforeProfileUpload = (file: NzUploadFile): boolean => {
    return (file.size || 0) / 1024 / 1024 < 2;
  };

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
    this.userForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: ['ROLE_RH', [Validators.required]],
      phoneNumber: [''],
      address: [''],
      profileImageUrl: ['']
    });
  }
}
