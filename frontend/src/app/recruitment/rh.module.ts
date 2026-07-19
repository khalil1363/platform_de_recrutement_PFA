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

import { RhHiredCandidatesComponent } from './rh/rh-hired-candidates/rh-hired-candidates.component';

const routes: Routes = [
  {
    path: '',
    component: RhLayoutComponent,
    canActivate: [authGuard, rhGuard],
    children: [
      { path: '', redirectTo: 'recruitments', pathMatch: 'full' },
      { path: 'recruitments', component: RhRecruitmentsComponent },
      { path: 'recruitments/new', component: RhRecruitmentFormComponent },
      { path: 'recruitments/:id/edit', component: RhRecruitmentFormComponent },
      { path: 'qcm', component: RhQcmListComponent },
      { path: 'qcm/new', component: RhQcmFormComponent },
      { path: 'qcm/:id/edit', component: RhQcmFormComponent },
      { path: 'candidates', component: RhCandidatesComponent },
      { path: 'hired', component: RhHiredCandidatesComponent },
      { path: 'calendar', component: RhCalendarComponent },
      { path: 'profile', loadChildren: () => import('../profile/profile.module').then((m) => m.ProfileModule) }
    ]
  }
];

@NgModule({
  declarations: [
    RhLayoutComponent,
    RhRecruitmentsComponent,
    RhRecruitmentFormComponent,
    RhQcmListComponent,
    RhQcmFormComponent,
    RhCandidatesComponent,
    RhHiredCandidatesComponent,
    RhCalendarComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class RhModule {}
