import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const profileRedirectGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  if (authService.isAdmin()) {
    return router.createUrlTree(['/admin/profile']);
  }
  if (authService.isRh()) {
    return router.createUrlTree(['/rh/profile']);
  }
  return router.createUrlTree(['/jobs/profile']);
};
