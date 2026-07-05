import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'home', redirectTo: 'jobs', pathMatch: 'full' },
  {
    path: 'login',
    loadChildren: () => import('./auth/auth.module').then((m) => m.AuthModule)
  },
  {
    path: 'register',
    loadChildren: () => import('./auth/auth.module').then((m) => m.AuthModule)
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then((m) => m.AdminModule)
  },
  {
    path: 'jobs',
    loadChildren: () => import('./recruitment/recruitment.module').then((m) => m.RecruitmentModule)
  },
  {
    path: 'rh',
    loadChildren: () => import('./recruitment/rh.module').then((m) => m.RhModule)
  },
  { path: '**', redirectTo: 'login' }
];
