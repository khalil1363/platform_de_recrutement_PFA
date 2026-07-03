import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { AuthComponent } from './auth.component';
import { guestGuard } from '../core/guards/guest.guard';

const routes: Routes = [
  { path: '', component: AuthComponent, canActivate: [guestGuard] }
];

@NgModule({
  declarations: [AuthComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuthModule {}
