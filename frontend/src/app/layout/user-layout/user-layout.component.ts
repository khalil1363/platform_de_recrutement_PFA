import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../models/auth.model';

@Component({
  selector: 'app-user-layout',
  templateUrl: './user-layout.component.html',
  styleUrl: './user-layout.component.css'
})
export class UserLayoutComponent implements OnInit {
  isCollapsed = false;
  currentUser: UserProfile | null = null;

  constructor(
    readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
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

  goToAdmin(): void {
    this.router.navigate(['/admin/users']);
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      ROLE_ADMIN: 'Administrateur',
      ROLE_RH: 'Ressources Humaines',
      ROLE_DEVELOPER: 'Développeur',
      ROLE_USER: 'Candidat'
    };
    return labels[role] || role;
  }
}
