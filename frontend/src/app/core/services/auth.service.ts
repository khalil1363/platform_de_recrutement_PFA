import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../models/api-response.model';
import {
  AdminCreateUserRequest,
  AuthenticationResponse,
  FileUploadResponse,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
  AdminUpdateUserRequest,
  UserProfile
} from '../../models/auth.model';

const TOKEN_KEY = 'daam_auth_token';
const ROLE_KEY = 'daam_user_role';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;

  private readonly currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  login(request: LoginRequest): Observable<ApiResponse<AuthenticationResponse>> {
    return this.http
      .post<ApiResponse<AuthenticationResponse>>(`${this.apiUrl}/login`, request)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  register(request: RegisterRequest): Observable<ApiResponse<AuthenticationResponse>> {
    return this.http
      .post<ApiResponse<AuthenticationResponse>>(`${this.apiUrl}/register`, request)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  loadCurrentUser(): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.apiUrl}/me`).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.currentUserSubject.next(response.data);
          this.setRole(response.data.role);
        }
      })
    );
  }

  getAllUsers(): Observable<ApiResponse<UserProfile[]>> {
    return this.http.get<ApiResponse<UserProfile[]>>(`${this.apiUrl}/users`);
  }

  createUser(request: AdminCreateUserRequest): Observable<ApiResponse<UserProfile>> {
    return this.http.post<ApiResponse<UserProfile>>(`${this.apiUrl}/users`, request);
  }

  updateUserStatus(userId: string, active: boolean): Observable<ApiResponse<UserProfile>> {
    return this.http.patch<ApiResponse<UserProfile>>(`${this.apiUrl}/users/${userId}/status`, { active });
  }

  updateProfile(request: UpdateProfileRequest): Observable<ApiResponse<UserProfile>> {
    return this.http.put<ApiResponse<UserProfile>>(`${this.apiUrl}/me`, request).pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.currentUserSubject.next(response.data);
        }
      })
    );
  }

  updateUser(userId: string, request: AdminUpdateUserRequest): Observable<ApiResponse<UserProfile>> {
    return this.http.put<ApiResponse<UserProfile>>(`${this.apiUrl}/users/${userId}`, request);
  }

  deleteUser(userId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/users/${userId}`);
  }

  getCurrentUserValue(): UserProfile | null {
    return this.currentUserSubject.value;
  }

  uploadProfileImage(file: File): Observable<ApiResponse<FileUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<FileUploadResponse>>(`${this.apiUrl}/upload/profile-image`, formData);
  }

  resolveImageUrl(path?: string | null): string | null {
    if (!path) {
      return null;
    }
    if (path.startsWith('http')) {
      return path;
    }
    return `${environment.apiUrl}${path}`;
  }

  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    return localStorage.getItem(TOKEN_KEY);
  }

  getRole(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    return localStorage.getItem(ROLE_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    return this.getRole() === 'ROLE_ADMIN';
  }

  isRh(): boolean {
    return this.getRole() === 'ROLE_RH';
  }

  isCandidate(): boolean {
    return this.getRole() === 'ROLE_USER';
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(ROLE_KEY);
    }
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  redirectAfterLogin(role: string): void {
    if (role === 'ROLE_ADMIN') {
      this.router.navigate(['/admin/users']);
      return;
    }
    if (role === 'ROLE_RH') {
      this.router.navigate(['/rh/recruitments']);
      return;
    }
    this.router.navigate(['/jobs']);
  }

  getProfileRoute(): string {
    if (this.isAdmin()) {
      return '/admin/profile';
    }
    if (this.isRh()) {
      return '/rh/profile';
    }
    return '/jobs/profile';
  }

  private handleAuthSuccess(response: ApiResponse<AuthenticationResponse>): void {
    if (response.success && response.data) {
      this.setToken(response.data.token);
      this.setRole(response.data.role);
    }
  }

  private setToken(token: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(TOKEN_KEY, token);
    }
  }

  private setRole(role: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(ROLE_KEY, role);
    }
  }
}
