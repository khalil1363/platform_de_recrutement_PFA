import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadFile, NzUploadXHRArgs } from 'ng-zorro-antd/upload';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { ApiResponse } from '../models/api-response.model';
import { AuthenticationResponse } from '../models/auth.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css',
  host: { ngSkipHydration: 'true' }
})
export class AuthComponent implements OnInit {
  isRegisterMode = false;
  isFlipping = false;
  loginLoading = false;
  registerLoading = false;
  imageUploading = false;
  profileImagePreview: string | null = null;

  loginForm!: FormGroup;
  registerForm!: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.buildForms();
    this.isRegisterMode = this.router.url.includes('/register');
  }

  toggleMode(): void {
    if (this.isFlipping) {
      return;
    }

    this.isFlipping = true;
    setTimeout(() => {
      this.isRegisterMode = !this.isRegisterMode;
      this.isFlipping = false;
      const target = this.isRegisterMode ? '/register' : '/login';
      this.router.navigate([target], { replaceUrl: true });
    }, 350);
  }

  onLogin(): void {
    if (this.loginForm.invalid) {
      Object.values(this.loginForm.controls).forEach((control) => {
        control.markAsDirty();
        control.updateValueAndValidity();
      });
      return;
    }

    this.loginLoading = true;
    this.authService.login(this.loginForm.value).subscribe({
      next: (response: ApiResponse<AuthenticationResponse>) => {
        this.loginLoading = false;
        if (response.success && response.data) {
          this.message.success('Connexion réussie');
          this.authService.redirectAfterLogin(response.data.role);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loginLoading = false;
        this.message.error(error.error?.message || 'Identifiants invalides');
      }
    });
  }

  onRegister(): void {
    if (this.registerForm.invalid) {
      Object.values(this.registerForm.controls).forEach((control) => {
        control.markAsDirty();
        control.updateValueAndValidity();
      });
      return;
    }

    this.registerLoading = true;
    const payload = { ...this.registerForm.value };
    if (!payload.phoneNumber) {
      delete payload.phoneNumber;
    }
    if (!payload.address) {
      delete payload.address;
    }
    if (!payload.profileImageUrl) {
      delete payload.profileImageUrl;
    }

    this.authService.register(payload).subscribe({
      next: (response: ApiResponse<AuthenticationResponse>) => {
        this.registerLoading = false;
        if (response.success && response.data) {
          this.message.success('Compte créé avec succès');
          this.authService.redirectAfterLogin(response.data.role);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.registerLoading = false;
        this.message.error(error.error?.message || 'Erreur lors de l\'inscription');
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
          this.registerForm.patchValue({ profileImageUrl: response.data.profileImageUrl });
          this.profileImagePreview = this.authService.resolveImageUrl(response.data.profileImageUrl);
          this.message.success('Photo de profil ajoutée');
          item.onSuccess?.(response, item.file, null);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.imageUploading = false;
        this.message.error(error.error?.message || 'Erreur lors du téléchargement de l\'image');
        item.onError?.(error, item.file);
      }
    });
  };

  beforeProfileUpload = (file: NzUploadFile): boolean => {
    const isImage = file.type?.startsWith('image/');
    if (!isImage) {
      this.message.error('Veuillez sélectionner une image');
      return false;
    }

    const isLt2M = (file.size || 0) / 1024 / 1024 < 2;
    if (!isLt2M) {
      this.message.error('L\'image doit faire moins de 2 Mo');
      return false;
    }

    return true;
  };

  private buildForms(): void {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      phoneNumber: [''],
      address: [''],
      profileImageUrl: ['']
    });
  }
}
