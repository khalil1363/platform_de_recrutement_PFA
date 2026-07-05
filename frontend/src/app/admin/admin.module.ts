import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { UsersComponent } from './users/users.component';
import { ZonesComponent } from './zones/zones.component';
import { RhAssignmentsComponent } from './rh-assignments/rh-assignments.component';
import { CompaniesComponent } from './companies/companies.component';
import { adminGuard } from '../core/guards/admin.guard';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'users', pathMatch: 'full' },
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
    UsersComponent,
    ZonesComponent,
    RhAssignmentsComponent,
    CompaniesComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
