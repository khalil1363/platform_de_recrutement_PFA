import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../../core/services/auth.service';
import { UserProfile } from '../../../models/auth.model';

@Component({
  selector: 'app-rh-candidate-users',
  templateUrl: './rh-candidate-users.component.html',
  styleUrl: './rh-candidate-users.component.css'
})
export class RhCandidateUsersComponent implements OnInit {
  users: UserProfile[] = [];
  filteredUsers: UserProfile[] = [];
  loading = false;

  filterFirstName = '';
  filterLastName = '';
  filterCin = '';

  constructor(
    readonly authService: AuthService,
    private readonly message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.authService.getCandidateUsers().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.users = response.data;
          this.applyFilters();
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur lors du chargement des candidats');
      }
    });
  }

  applyFilters(): void {
    const first = this.filterFirstName.trim().toLowerCase();
    const last = this.filterLastName.trim().toLowerCase();
    const cin = this.filterCin.trim().toLowerCase();

    this.filteredUsers = this.users.filter((user) => {
      const matchFirst = !first || (user.firstName || '').toLowerCase().includes(first);
      const matchLast = !last || (user.lastName || '').toLowerCase().includes(last);
      const matchCin = !cin || (user.cin || '').toLowerCase().includes(cin);
      return matchFirst && matchLast && matchCin;
    });
  }

  clearFilters(): void {
    this.filterFirstName = '';
    this.filterLastName = '';
    this.filterCin = '';
    this.applyFilters();
  }

  onStatusChange(user: UserProfile, active: boolean): void {
    this.authService.updateCandidateStatus(user.userId, active).subscribe({
      next: (response) => {
        if (response.success) {
          user.active = active;
          this.message.success(active ? 'Compte candidat activé' : 'Compte candidat désactivé');
        }
      },
      error: (error: HttpErrorResponse) => {
        this.message.error(error.error?.message || 'Erreur lors de la mise à jour');
        this.loadUsers();
      }
    });
  }
}
