import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { authGuard } from '../core/guards/auth.guard';
import { candidateGuard } from '../core/guards/candidate.guard';
import { CandidateLayoutComponent } from '../layout/candidate-layout/candidate-layout.component';
import { JobListComponent } from './candidate/job-list/job-list.component';
import { JobDetailComponent } from './candidate/job-detail/job-detail.component';
import { JobApplyComponent } from './candidate/job-apply/job-apply.component';
import { MyApplicationsComponent } from './candidate/my-applications/my-applications.component';
import { MyEvaluationsComponent } from './candidate/my-evaluations/my-evaluations.component';
import { TakeEvaluationComponent } from './candidate/take-evaluation/take-evaluation.component';

const routes: Routes = [
  {
    path: '',
    component: CandidateLayoutComponent,
    children: [
      { path: '', component: JobListComponent },
      { path: 'applications', component: MyApplicationsComponent, canActivate: [authGuard, candidateGuard] },
      { path: 'evaluations', component: MyEvaluationsComponent, canActivate: [authGuard, candidateGuard] },
      { path: 'evaluations/:assignmentId', component: TakeEvaluationComponent, canActivate: [authGuard, candidateGuard] },
      { path: 'profile', loadChildren: () => import('../profile/profile.module').then((m) => m.ProfileModule), canActivate: [authGuard, candidateGuard] },
      { path: ':id/apply', component: JobApplyComponent, canActivate: [authGuard, candidateGuard] },
      { path: ':id', component: JobDetailComponent }
    ]
  }
];

@NgModule({
  declarations: [
    CandidateLayoutComponent,
    JobListComponent,
    JobDetailComponent,
    JobApplyComponent,
    MyApplicationsComponent,
    MyEvaluationsComponent,
    TakeEvaluationComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class RecruitmentModule {}
