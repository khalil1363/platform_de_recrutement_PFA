export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  cin: string;
  phoneNumber?: string;
  address?: string;
  profileImageUrl?: string;
}

export interface AuthenticationResponse {
  token: string;
  username: string;
  email: string;
  role: string;
}

export interface UserProfile {
  userId: string;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  phoneNumber?: string;
  cin?: string;
  address?: string;
  profileImageUrl?: string;
  meetingLink?: string;
  lastLoginDate?: string;
  joinDate?: string;
  role: string;
  authorities: string[];
  active: boolean;
  notLocked: boolean;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  email: string;
  cin?: string;
  phoneNumber?: string;
  address?: string;
  profileImageUrl?: string;
  meetingLink?: string;
  password?: string;
}

export interface AdminUpdateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  cin?: string;
  phoneNumber?: string;
  address?: string;
  profileImageUrl?: string;
  meetingLink?: string;
  password?: string;
}

export interface AdminCreateUserRequest {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  role: string;
  cin?: string;
  phoneNumber?: string;
  address?: string;
  profileImageUrl?: string;
  meetingLink?: string;
}

export interface FileUploadResponse {
  profileImageUrl: string;
}

export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_RH' | 'ROLE_DEVELOPER';
