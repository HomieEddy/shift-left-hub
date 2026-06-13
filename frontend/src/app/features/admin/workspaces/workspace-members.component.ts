import { Component, DestroyRef, inject, Input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { WorkspaceService } from './workspace.service';
import { WorkspaceMemberDto, InvitationDto, CreateInvitationRequest, ChangeRoleRequest } from './workspace.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-workspace-members',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './workspace-members.component.html',
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
