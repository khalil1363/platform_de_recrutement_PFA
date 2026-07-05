import { Component, OnInit } from '@angular/core';
import { AuthService } from '../core/services/auth.service';
import { UserProfile } from '../models/auth.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  currentUser: UserProfile | null = null;

  constructor(readonly authService: AuthService) {}

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
}
