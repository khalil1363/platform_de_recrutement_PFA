import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { ShellBase } from '../../core/layout/shell-base';
import { UserProfile } from '../../models/auth.model';

@Component({
  selector: 'app-candidate-layout',
  templateUrl: './candidate-layout.component.html',
  styleUrl: './candidate-layout.component.css'
})
export class CandidateLayoutComponent extends ShellBase implements OnInit, OnDestroy {
  currentUser: UserProfile | null = null;

  constructor(readonly authService: AuthService) {
    super();
  }

  override ngOnInit(): void {
    super.ngOnInit();
    if (this.authService.isAuthenticated()) {
      this.authService.loadCurrentUser().subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.currentUser = response.data;
          }
        }
      });
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
