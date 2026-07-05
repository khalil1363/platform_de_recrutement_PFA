import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadFile, NzUploadXHRArgs } from 'ng-zorro-antd/upload';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { ApiResponse } from '../models/api-response.model';
import { UserProfile } from '../models/auth.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  host: { ngSkipHydration: 'true' }
})
export class ProfileComponent implements OnInit {
  currentUser: UserProfile | null = null;
  profileForm!: FormGroup;
  loading = true;
  saving = false;
  imageUploading = false;
  profileImagePreview: string | null = null;
  editMode = false;

  constructor(
    readonly authService: AuthService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.authService.loadCurrentUser().subscribe({
      next: (response: ApiResponse<UserProfile>) => {
        this.loading = false;
        if (response.success && response.data) {
          this.currentUser = response.data;
          this.profileImagePreview = this.authService.resolveImageUrl(response.data.profileImageUrl);
          this.patchForm(response.data);
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Impossible de charger le profil');
      }
    });
  }

  enableEdit(): void {
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
    if (this.currentUser) {
      this.patchForm(this.currentUser);
      this.profileImagePreview = this.authService.resolveImageUrl(this.currentUser.profileImageUrl);
    }
  }

  onSave(): void {
    if (this.profileForm.invalid) {
      Object.values(this.profileForm.controls).forEach((c) => {
        c.markAsDirty();
        c.updateValueAndValidity();
      });
      return;
    }

    const payload = { ...this.profileForm.value };
    if (!payload.password) {
      delete payload.password;
    }
    if (!payload.phoneNumber) {
      delete payload.phoneNumber;
    }
    if (!payload.address) {
      delete payload.address;
    }
    if (!payload.profileImageUrl) {
      delete payload.profileImageUrl;
    }

    this.saving = true;
    this.authService.updateProfile(payload).subscribe({
      next: (response: ApiResponse<UserProfile>) => {
        this.saving = false;
        if (response.success && response.data) {
          this.currentUser = response.data;
          this.editMode = false;
          this.message.success('Profil mis à jour avec succès');
        }
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        this.message.error(error.error?.message || 'Erreur lors de la mise à jour');
      }
    });
  }

  handleProfileUpload = (item: NzUploadXHRArgs): Subscription => {
    const file = item.file as unknown as File;
    this.imageUploading = true;

    return this.authService.uploadProfileImage(file).subscribe({
      next: (response: ApiResponse<{ profileImageUrl: string }>) => {
        this.imageUploading = false;
        if (response.success && response.data) {
          this.profileForm.patchValue({ profileImageUrl: response.data.profileImageUrl });
          this.profileImagePreview = this.authService.resolveImageUrl(response.data.profileImageUrl);
          this.message.success('Photo mise à jour');
          item.onSuccess?.(response, item.file, null);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.imageUploading = false;
        this.message.error(error.error?.message || 'Erreur upload');
        item.onError?.(error, item.file);
      }
    });
  };

  beforeProfileUpload = (file: NzUploadFile): boolean => {
    if (!file.type?.startsWith('image/')) {
      this.message.error('Image uniquement');
      return false;
    }
    if ((file.size || 0) / 1024 / 1024 >= 2) {
      this.message.error('Max 2 Mo');
      return false;
    }
    return true;
  };

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      ROLE_ADMIN: 'Administrateur',
      ROLE_RH: 'Ressources Humaines',
      ROLE_DEVELOPER: 'Développeur',
      ROLE_USER: 'Candidat'
    };
    return labels[role] || role;
  }

  private buildForm(): void {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: [''],
      address: [''],
      profileImageUrl: [''],
      password: ['', Validators.minLength(6)]
    });
  }

  private patchForm(user: UserProfile): void {
    this.profileForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phoneNumber: user.phoneNumber || '',
      address: user.address || '',
      profileImageUrl: user.profileImageUrl || '',
      password: ''
    });
  }
}
