import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const candidateGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isCandidate()) {
    return true;
  }
  if (authService.isAdmin()) {
    router.navigate(['/admin/users']);
    return false;
  }
  if (authService.isRh()) {
    router.navigate(['/rh/recruitments']);
    return false;
  }
  router.navigate(['/login']);
  return false;
};
