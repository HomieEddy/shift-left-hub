import { Component, inject, OnInit, signal } from '@angular/core';
import { NgFor, NgIf, NgClass } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { UserDto } from '../../../core/auth/auth.models';

type SortField = 'displayName' | 'email' | 'role' | 'enabled' | 'createdAt';
type SortDir = 'asc' | 'desc';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [NgFor, NgIf, NgClass],
  templateUrl: './user-list.component.html',
})
export class UserListComponent implements OnInit {
  private authService = inject(AuthService);
  protected users = signal<UserDto[]>([]);
  protected isLoading = signal(true);
  protected errorMessage = signal('');

  protected sortField: SortField = 'createdAt';
  protected sortDir: SortDir = 'desc';
  protected editingUser = signal<UserDto | null>(null);
  protected showRoleDialog = signal(false);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.getUsers().subscribe({
      next: (data) => {
        this.users.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load users. Make sure you have admin access.');
        this.isLoading.set(false);
      },
    });
  }

  get sortedUsers(): UserDto[] {
    const sorted = [...this.users()].sort((a, b) => {
      let aVal: any = (a as any)[this.sortField];
      let bVal: any = (b as any)[this.sortField];
      if (typeof aVal === 'string') aVal = aVal.toLowerCase();
      if (typeof bVal === 'string') bVal = bVal.toLowerCase();
      if (aVal < bVal) return this.sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDir === 'asc' ? 1 : -1;
      return 0;
    });
    return sorted;
  }

  setSort(field: SortField): void {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDir = 'asc';
    }
  }

  sortIndicator(field: SortField): string {
    if (this.sortField !== field) return '';
    return this.sortDir === 'asc' ? ' ▲' : ' ▼';
  }

  openRoleDialog(user: UserDto): void {
    this.editingUser.set(user);
    this.showRoleDialog.set(true);
  }

  closeRoleDialog(): void {
    this.showRoleDialog.set(false);
    this.editingUser.set(null);
  }

  updateRole(newRole: string): void {
    const user = this.editingUser();
    if (!user) return;

    this.authService.updateUserRole(user.id, newRole).subscribe({
      next: (updated) => {
        this.users.update(users => users.map(u => u.id === updated.id ? updated : u));
        this.closeRoleDialog();
      },
      error: () => {
        this.errorMessage.set('Failed to update role. Please try again.');
        this.closeRoleDialog();
      },
    });
  }

  toggleStatus(user: UserDto): void {
    this.authService.toggleUserStatus(user.id).subscribe({
      next: (updated) => {
        this.users.update(users => users.map(u => u.id === updated.id ? updated : u));
      },
      error: () => {
        this.errorMessage.set('Failed to toggle user status.');
      },
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString();
  }
}
