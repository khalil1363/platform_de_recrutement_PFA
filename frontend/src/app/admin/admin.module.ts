import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { UsersComponent } from './users/users.component';
import { ZonesComponent } from './zones/zones.component';
import { RhAssignmentsComponent } from './rh-assignments/rh-assignments.component';
import { CompaniesComponent } from './companies/companies.component';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { AdminRecruitmentsComponent } from './admin-recruitments/admin-recruitments.component';
import { AdminApplicationsComponent } from './admin-applications/admin-applications.component';
import { AdminHiredComponent } from './admin-hired/admin-hired.component';
import { AdminReclamationsComponent } from './admin-reclamations/admin-reclamations.component';
import { adminGuard } from '../core/guards/admin.guard';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'recruitments', component: AdminRecruitmentsComponent },
      { path: 'applications', component: AdminApplicationsComponent },
      { path: 'hired', component: AdminHiredComponent },
      { path: 'reclamations', component: AdminReclamationsComponent },
      { path: 'users', component: UsersComponent },
      { path: 'zones', component: ZonesComponent },
      { path: 'rh-assignments', component: RhAssignmentsComponent },
      { path: 'companies', component: CompaniesComponent },
      { path: 'profile', loadChildren: () => import('../profile/profile.module').then((m) => m.ProfileModule) }
    ]
  }
];

@NgModule({
  declarations: [
    AdminLayoutComponent,
    AdminDashboardComponent,
    AdminRecruitmentsComponent,
    AdminApplicationsComponent,
    AdminHiredComponent,
    AdminReclamationsComponent,
    UsersComponent,
    ZonesComponent,
    RhAssignmentsComponent,
    CompaniesComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
