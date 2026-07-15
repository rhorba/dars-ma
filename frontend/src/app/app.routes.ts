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
    path: 'gigs/new',
    loadComponent: () =>
      import('./features/gig/gig-create-form/gig-create-form.component').then((m) => m.GigCreateFormComponent),
    canActivate: [roleGuard(['STUDENT'])]
  },
  {
    path: 'gigs/:id',
    loadComponent: () =>
      import('./features/gig/gig-detail/gig-detail.component').then((m) => m.GigDetailComponent),
    canActivate: [authGuard]
  },
  {
    path: 'tutors',
    loadComponent: () =>
      import('./features/profile/tutor-browse/tutor-browse.component').then((m) => m.TutorBrowseComponent),
    canActivate: [authGuard]
  },
  {
    path: 'gigs/:gigId/book/:tutorUserId',
    loadComponent: () =>
      import('./features/booking/booking-confirm/booking-confirm.component').then((m) => m.BookingConfirmComponent),
    canActivate: [roleGuard(['STUDENT'])]
  },
  {
    path: 'bookings/:id',
    loadComponent: () =>
      import('./features/booking/booking-detail/booking-detail.component').then((m) => m.BookingDetailComponent),
    canActivate: [authGuard]
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
    path: 'admin/disputes',
    loadComponent: () =>
      import('./features/admin/dispute-queue/dispute-queue.component').then((m) => m.DisputeQueueComponent),
    canActivate: [roleGuard(['ADMIN'])]
  },
  {
    path: '',
    loadComponent: () => import('./shared/home.component').then((m) => m.HomeComponent),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '' }
];
