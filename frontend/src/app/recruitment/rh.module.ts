import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { authGuard } from '../core/guards/auth.guard';
import { rhGuard } from './guards/rh.guard';
import { RhLayoutComponent } from './rh/rh-layout/rh-layout.component';
import { RhRecruitmentsComponent } from './rh/rh-recruitments/rh-recruitments.component';
import { RhRecruitmentFormComponent } from './rh/rh-recruitment-form/rh-recruitment-form.component';
import { RhCandidatesComponent } from './rh/rh-candidates/rh-candidates.component';
import { RhCalendarComponent } from './rh/rh-calendar/rh-calendar.component';
import { RhQcmListComponent } from './rh/rh-qcm-list/rh-qcm-list.component';
import { RhQcmFormComponent } from './rh/rh-qcm-form/rh-qcm-form.component';
import { RhDashboardComponent } from './rh/rh-dashboard/rh-dashboard.component';
import { RhCandidateUsersComponent } from './rh/rh-candidate-users/rh-candidate-users.component';
import { RhReclamationsComponent } from './rh/rh-reclamations/rh-reclamations.component';

const routes: Routes = [
  {
    path: '',
    component: RhLayoutComponent,
    canActivate: [authGuard, rhGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: RhDashboardComponent },
      { path: 'recruitments/new', component: RhRecruitmentFormComponent },
      { path: 'recruitments/:id/edit', component: RhRecruitmentFormComponent },
      { path: 'recruitments', component: RhRecruitmentsComponent },
      { path: 'qcm/new', component: RhQcmFormComponent },
      { path: 'qcm/:id/edit', component: RhQcmFormComponent },
      { path: 'qcm', component: RhQcmListComponent },
      { path: 'candidates', component: RhCandidatesComponent },
      { path: 'users', component: RhCandidateUsersComponent },
      { path: 'calendar', component: RhCalendarComponent },
      { path: 'reclamations', component: RhReclamationsComponent },
      { path: 'profile', loadChildren: () => import('../profile/profile.module').then((m) => m.ProfileModule) }
    ]
  }
];

@NgModule({
  declarations: [
    RhLayoutComponent,
    RhDashboardComponent,
    RhRecruitmentsComponent,
    RhRecruitmentFormComponent,
    RhQcmListComponent,
    RhQcmFormComponent,
    RhCandidatesComponent,
    RhCandidateUsersComponent,
    RhCalendarComponent,
    RhReclamationsComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class RhModule {}
