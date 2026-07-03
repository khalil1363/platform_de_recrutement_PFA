import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../core/services/auth.service';
import { ApiResponse } from '../models/api-response.model';
import { AuthenticationResponse } from '../models/auth.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent implements OnInit {
  isRegisterMode = false;
  isFlipping = false;
  loginLoading = false;
  registerLoading = false;

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
    this.authService.register(this.registerForm.value).subscribe({
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
      address: ['']
    });
  }
}
