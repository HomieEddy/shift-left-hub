import { Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { UserDto } from '../../../core/auth/auth.models';
import { TranslationService } from '../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../shared/ui/confirmation-dialog/confirmation-dialog.service';

type SortField = 'displayName' | 'email' | 'role' | 'enabled' | 'createdAt';
type SortDir = 'asc' | 'desc';

/** Admin user management: list, sort, edit roles, and toggle status. */
@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [NgClass],
  templateUrl: './user-list.component.html',
})
export class UserListComponent implements OnInit {
  private authService = inject(AuthService);
  private destroyRef = inject(DestroyRef);
  private confirmationDialog = inject(ConfirmationDialogService);
  protected translationService = inject(TranslationService);
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

    this.authService
      .getUsers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.users.set(data);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set(this.translationService.translate('error.load-users'));
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
    if (user == null) return;

    this.authService
      .updateUserRole(user.id, newRole)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.users.update((users) => users.map((u) => (u.id === updated.id ? updated : u)));
          this.closeRoleDialog();
        },
        error: () => {
          this.errorMessage.set(this.translationService.translate('error.update-role'));
          this.closeRoleDialog();
        },
      });
  }

  toggleStatus(user: UserDto): void {
    const action = user.enabled ? 'disable' : 'enable';
    this.confirmationDialog
      .confirm({
        title: this.translationService.translate('admin.users.' + action + '-title'),
        message: this.translationService.translate('admin.users.' + action + '-message', {
          VAR_NAME: user.displayName,
        }),
        confirmLabel: this.translationService.translate('admin.users.' + action + '-confirm'),
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.authService
          .toggleUserStatus(user.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (updated) => {
              this.users.update((users) => users.map((u) => (u.id === updated.id ? updated : u)));
            },
            error: () => {
              this.errorMessage.set(this.translationService.translate('error.toggle-status'));
            },
          });
      });
  }

  /** Translatable labels */
  roleLabels = computed<Record<string, string>>(() => ({
    ROLE_ADMIN: this.translationService.translate('admin.users.role.admin'),
    ROLE_AGENT: this.translationService.translate('admin.users.role.agent'),
    ROLE_USER: this.translationService.translate('admin.users.role.user'),
  }));

  statusLabels = computed<Record<string, string>>(() => ({
    active: this.translationService.translate('admin.users.status.active'),
    disabled: this.translationService.translate('admin.users.status.disabled'),
  }));

  actionDisable = computed(() => this.translationService.translate('admin.users.disable'));
  actionEnable = computed(() => this.translationService.translate('admin.users.enable'));

  /** Format an ISO date string for display. */
  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString();
  }
}
