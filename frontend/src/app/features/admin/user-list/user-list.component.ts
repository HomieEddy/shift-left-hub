import { Component, inject, OnInit, signal } from '@angular/core';
import { NgFor, NgIf, NgClass } from '@angular/common';
import { $localize } from '@angular/localize/init';
import { AuthService } from '../../../core/auth/auth.service';
import { UserDto } from '../../../core/auth/auth.models';

type SortField = 'displayName' | 'email' | 'role' | 'enabled' | 'createdAt';
type SortDir = 'asc' | 'desc';

/** Admin user management: list, sort, edit roles, and toggle status. */
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

  /** Fetch all users from the API. */
  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.getUsers().subscribe({
      next: (data) => {
        this.users.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set($localize`:@@error.load-users:Failed to load users. Make sure you have admin access.`);
        this.isLoading.set(false);
      },
    });
  }

  /** Users sorted by the current field and direction. */
  get sortedUsers(): UserDto[] {
    const sorted = [...this.users()].sort((a, b) => {
      let aVal = a[this.sortField];
      let bVal = b[this.sortField];
      if (typeof aVal === 'string') aVal = aVal.toLowerCase();
      if (typeof bVal === 'string') bVal = bVal.toLowerCase();
      if (aVal < bVal) return this.sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDir === 'asc' ? 1 : -1;
      return 0;
    });
    return sorted;
  }

  /** Set or toggle a sort field. */
  setSort(field: SortField): void {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDir = 'asc';
    }
  }

  /** Return sort arrow indicator for the given field. */
  sortIndicator(field: SortField): string {
    if (this.sortField !== field) return '';
    return this.sortDir === 'asc' ? ' ▲' : ' ▼';
  }

  /** Open the role-editing dialog for a user. */
  openRoleDialog(user: UserDto): void {
    this.editingUser.set(user);
    this.showRoleDialog.set(true);
  }

  /** Close the role-editing dialog. */
  closeRoleDialog(): void {
    this.showRoleDialog.set(false);
    this.editingUser.set(null);
  }

  /** Update the selected user's role via the API. */
  updateRole(newRole: string): void {
    const user = this.editingUser();
    if (!user) return;

    this.authService.updateUserRole(user.id, newRole).subscribe({
      next: (updated) => {
        this.users.update(users => users.map(u => u.id === updated.id ? updated : u));
        this.closeRoleDialog();
      },
      error: () => {
        this.errorMessage.set($localize`:@@error.update-role:Failed to update role. Please try again.`);
        this.closeRoleDialog();
      },
    });
  }

  /** Toggle a user's enabled/disabled status via the API. */
  toggleStatus(user: UserDto): void {
    this.authService.toggleUserStatus(user.id).subscribe({
      next: (updated) => {
        this.users.update(users => users.map(u => u.id === updated.id ? updated : u));
      },
      error: () => {
        this.errorMessage.set($localize`:@@error.toggle-status:Failed to toggle user status.`);
      },
    });
  }

  /** Translatable labels */
  roleLabels: Record<string, string> = {
    'ROLE_ADMIN': $localize`:@@admin.users.role.admin:Admin`,
    'ROLE_AGENT': $localize`:@@admin.users.role.agent:Agent`,
    'ROLE_USER': $localize`:@@admin.users.role.user:User`,
  };

  statusLabels: Record<string, string> = {
    'active': $localize`:@@admin.users.status.active:Active`,
    'disabled': $localize`:@@admin.users.status.disabled:Disabled`,
  };

  actionEditRole: string = $localize`:@@admin.users.edit-role:Edit Role`;
  actionDisable: string = $localize`:@@admin.users.disable:Disable`;
  actionEnable: string = $localize`:@@admin.users.enable:Enable`;

  dialogTitle: string = $localize`:@@admin.users.dialog.title:Edit Role`;
  dialogCancel: string = $localize`:@@admin.users.dialog.cancel:Cancel`;
  dialogChangeRoleFor: string = $localize`:@@admin.users.dialog.changeRole:Change role for`;

  dialogDescAdmin: string = $localize`:@@admin.users.role.admin.desc:Full access to all features`;
  dialogDescAgent: string = $localize`:@@admin.users.role.agent.desc:Ticket queue and resolution access`;
  dialogDescUser: string = $localize`:@@admin.users.role.user.desc:Standard user access`;

  /** Format an ISO date string for display. */
  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString();
  }
}
