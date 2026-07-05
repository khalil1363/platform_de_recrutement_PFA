import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserProfile } from '../../../models/auth.model';

@Component({
  selector: 'app-rh-layout',
  templateUrl: './rh-layout.component.html',
  styleUrl: './rh-layout.component.css'
})
export class RhLayoutComponent {
  isCollapsed = false;
  currentUser: UserProfile | null = null;

  constructor(
    readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.authService.loadCurrentUser().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.currentUser = response.data;
        }
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
