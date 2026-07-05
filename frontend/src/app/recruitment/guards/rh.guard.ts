import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

export const rhGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isRh()) {
    return true;
  }
  if (authService.isAdmin()) {
    router.navigate(['/admin/users']);
    return false;
  }
  router.navigate(['/jobs']);
  return false;
};
