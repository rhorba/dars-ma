import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'profile/tutor/me',
    loadComponent: () =>
      import('./features/profile/tutor-profile-form/tutor-profile-form.component').then(
        (m) => m.TutorProfileFormComponent
      ),
    canActivate: [roleGuard(['TUTOR'])]
  },
  {
    path: 'profile/tutor/:userId',
    loadComponent: () =>
      import('./features/profile/tutor-profile-view/tutor-profile-view.component').then(
        (m) => m.TutorProfileViewComponent
      )
  },
  {
    path: 'admin/verification',
    loadComponent: () =>
      import('./features/admin/verification-queue/verification-queue.component').then(
        (m) => m.VerificationQueueComponent
      ),
    canActivate: [roleGuard(['ADMIN'])]
  },
  {
    path: '',
    loadComponent: () => import('./shared/home.component').then((m) => m.HomeComponent),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '' }
];
