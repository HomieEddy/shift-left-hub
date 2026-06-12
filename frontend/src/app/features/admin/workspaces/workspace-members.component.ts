import { Component, DestroyRef, inject, Input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkspaceService } from './workspace.service';
import { WorkspaceMemberDto, InvitationDto, CreateInvitationRequest, ChangeRoleRequest } from './workspace.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-workspace-members',
  standalone: true,
  imports: [NgClass, DatePipe, FormsModule],
  template: `
    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-semibold text-text-primary">{{ translationService.translate('admin.workspaces.members.invite') }}</h3>
        @if (!showInviteForm()) {
          <button (click)="openInviteForm()"
            class="px-3 py-1.5 text-sm bg-accent-info text-white rounded-lg hover:opacity-90">
            + {{ translationService.translate('admin.workspaces.members.invite') }}
          </button>
        }
      </div>

      @if (showInviteForm()) {
        <div class="rounded-xl border border-border-default bg-surface-secondary p-4 space-y-3">
          <select [ngModel]="selectedUserId()" (ngModelChange)="selectedUserId.set($event)"
            class="w-full rounded-lg border border-border-default px-3 py-2 text-sm bg-surface-primary text-text-primary">
            <option value="">{{ translationService.translate('admin.workspaces.select-user') }}</option>
            @for (u of availableUsers(); track u.userId) {
              <option [value]="u.userId">{{ u.displayName }} ({{ u.email }})</option>
            }
          </select>
          <select [ngModel]="selectedRole()" (ngModelChange)="selectedRole.set($event)"
            class="w-full rounded-lg border border-border-default px-3 py-2 text-sm bg-surface-primary text-text-primary">
            <option value="MEMBER">{{ translationService.translate('workspace.role.member') }}</option>
            <option value="ADMIN">{{ translationService.translate('workspace.role.admin') }}</option>
            <option value="READ_ONLY">{{ translationService.translate('workspace.role.read-only') }}</option>
          </select>
          <div class="flex gap-2">
            <button (click)="sendInvitation()" [disabled]="isSendingInvite() || !selectedUserId()"
              class="px-4 py-2 text-sm rounded-lg bg-accent-info text-white font-medium hover:opacity-90 disabled:opacity-50">
              {{ translationService.translate('admin.workspaces.members.invite') }}
            </button>
            <button (click)="cancelInvite()"
              class="px-4 py-2 text-sm rounded-lg border border-border-default text-text-secondary hover:bg-surface-tertiary">
              {{ translationService.translate('admin.workspaces.cancel') }}
            </button>
          </div>
        </div>
      }

      <div class="rounded-xl border border-border-default overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-surface-secondary border-b border-border-default">
            <tr>
              <th class="text-left px-4 py-3 font-medium text-text-secondary">{{ translationService.translate('admin.users.col.name') }}</th>
              <th class="text-left px-4 py-3 font-medium text-text-secondary">{{ translationService.translate('admin.users.col.email') }}</th>
              <th class="text-left px-4 py-3 font-medium text-text-secondary">{{ translationService.translate('admin.users.col.role') }}</th>
              <th class="text-left px-4 py-3 font-medium text-text-secondary">{{ translationService.translate('admin.workspaces.col.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-light">
            @for (m of members(); track m.userId) {
              <tr class="hover:bg-surface-secondary transition-colors">
                <td class="px-4 py-3 font-medium text-text-primary">{{ m.displayName }}</td>
                <td class="px-4 py-3 text-text-secondary">{{ m.email }}</td>
                <td class="px-4 py-3">
                  <select [ngModel]="m.role" (ngModelChange)="changeRole(m, $event)"
                    class="text-xs rounded border border-border-default px-2 py-1 bg-surface-primary text-text-primary">
                    <option value="ADMIN">{{ translationService.translate('workspace.role.admin') }}</option>
                    <option value="MEMBER">{{ translationService.translate('workspace.role.member') }}</option>
                    <option value="READ_ONLY">{{ translationService.translate('workspace.role.read-only') }}</option>
                  </select>
                </td>
                <td class="px-4 py-3">
                  <button (click)="confirmRemoveMember(m)" [disabled]="isOnlyAdmin(m)"
                    class="text-xs px-2 py-1 rounded border border-border-default text-accent-danger hover:bg-accent-danger-muted disabled:opacity-50 disabled:cursor-not-allowed">
                    {{ translationService.translate('admin.workspaces.members.remove') }}
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      @if (invitations().length > 0) {
        <div class="rounded-xl border border-border-default p-4">
          <h4 class="text-sm font-semibold text-text-secondary mb-2">{{ translationService.translate('workspace.switcher.invitations') }}</h4>
          @for (inv of invitations(); track inv.id) {
            <div class="flex items-center justify-between py-2 text-sm">
              <span>{{ inv.invitedUserEmail }}</span>
              <span class="text-xs rounded-full bg-surface-secondary px-2 py-0.5">{{ inv.role }}</span>
              <button (click)="revokeInvitation(inv)" class="text-xs text-accent-danger hover:underline">
                {{ translationService.translate('admin.workspaces.members.remove') }}
              </button>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class WorkspaceMembersComponent implements OnInit {
  @Input({ required: true }) workspaceId!: string;

  private workspaceService = inject(WorkspaceService);
  protected translationService = inject(TranslationService);
  private destroyRef = inject(DestroyRef);

  members = signal<WorkspaceMemberDto[]>([]);
  invitations = signal<InvitationDto[]>([]);
  availableUsers = signal<WorkspaceMemberDto[]>([]);
  isLoading = signal(true);
  showInviteForm = signal(false);
  selectedUserId = signal('');
  selectedRole = signal<'ADMIN' | 'MEMBER' | 'READ_ONLY'>('MEMBER');
  isSendingInvite = signal(false);
  errorMessage = signal('');

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading.set(true);
    this.workspaceService.getMembers(this.workspaceId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(m => { this.members.set(m); this.isLoading.set(false); });
    this.workspaceService.getInvitations(this.workspaceId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(invs => this.invitations.set(invs));
    this.workspaceService.getAvailableUsers(this.workspaceId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(users => this.availableUsers.set(users));
  }

  openInviteForm() { this.showInviteForm.set(true); }

  cancelInvite() {
    this.showInviteForm.set(false);
    this.selectedUserId.set('');
  }

  sendInvitation() {
    this.isSendingInvite.set(true);
    const request: CreateInvitationRequest = {
      userId: this.selectedUserId(),
      role: this.selectedRole(),
    };
    this.workspaceService.sendInvitation(this.workspaceId, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSendingInvite.set(false);
          this.cancelInvite();
          this.loadData();
        },
        error: () => this.isSendingInvite.set(false),
      });
  }

  changeRole(member: WorkspaceMemberDto, newRole: string) {
    const req: ChangeRoleRequest = { role: newRole as 'ADMIN' | 'MEMBER' | 'READ_ONLY' };
    this.workspaceService.changeMemberRole(this.workspaceId, member.userId, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadData());
  }

  confirmRemoveMember(member: WorkspaceMemberDto) {
    this.workspaceService.removeMember(this.workspaceId, member.userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadData());
  }

  revokeInvitation(invitation: InvitationDto) {
    this.workspaceService.revokeInvitation(this.workspaceId, invitation.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadData());
  }

  isOnlyAdmin(member: WorkspaceMemberDto): boolean {
    if (member.role !== 'ADMIN') { return false; }
    const adminCount = this.members().filter(m => m.role === 'ADMIN').length;
    return adminCount <= 1;
  }
}
