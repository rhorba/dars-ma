import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './auth.models';

export const roleGuard = (allowed: Role[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const role = authService.role();
  if (authService.isAuthenticated() && role !== null && allowed.includes(role)) {
    return true;
  }
  return router.createUrlTree(['/login']);
};
