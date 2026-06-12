import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { WorkspaceService } from './workspace.service';
import { WorkspaceDto, WorkspaceMemberDto } from './workspace.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-workspace-list',
  standalone: true,
  imports: [NgClass, DatePipe, FormsModule, RouterLink],
  templateUrl: './workspace-list.component.html',
})
export class WorkspaceListComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  protected workspaces = signal<WorkspaceDto[]>([]);
  protected isLoading = signal(true);
  protected errorMessage = signal('');

  protected showCreateForm = signal(false);
  protected newWorkspaceName = signal('');
  protected newWorkspaceDescription = signal('');
  protected isCreating = signal(false);
  protected createError = signal('');

  protected selectedWorkspace = signal<WorkspaceDto | null>(null);
  protected workspaceMembers = signal<WorkspaceMemberDto[]>([]);
  protected availableUsers = signal<WorkspaceMemberDto[]>([]);
  protected selectedUserId = signal('');
  protected selectedRole = signal<'ADMIN' | 'MEMBER' | 'READ_ONLY'>('MEMBER');
  protected showUserDialog = signal(false);

  ngOnInit(): void {
    this.loadWorkspaces();
  }

  navigateToWorkspace(workspace: WorkspaceDto): void {
    this.router.navigate(['/admin/workspaces', workspace.id]);
  }

  loadWorkspaces(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.workspaceService.getWorkspaces().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { this.workspaces.set(data); this.isLoading.set(false); },
      error: () => { this.errorMessage.set('Failed to load workspaces'); this.isLoading.set(false); },
    });
  }

  createWorkspace(): void {
    if (!this.newWorkspaceName().trim()) return;
    this.isCreating.set(true);
    this.createError.set('');
    this.workspaceService.createWorkspace({
      name: this.newWorkspaceName().trim(),
      description: this.newWorkspaceDescription().trim() || undefined,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.isCreating.set(false);
        this.showCreateForm.set(false);
        this.newWorkspaceName.set('');
        this.newWorkspaceDescription.set('');
        this.loadWorkspaces();
      },
      error: () => { this.createError.set('Failed to create workspace'); this.isCreating.set(false); },
    });
  }

  openUserDialog(workspace: WorkspaceDto): void {
    this.selectedWorkspace.set(workspace);
    this.showUserDialog.set(true);
    this.workspaceService.getMembers(workspace.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (members) => this.workspaceMembers.set(members),
    });
    this.workspaceService.getAvailableUsers(workspace.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (users) => this.availableUsers.set(users),
    });
  }

  assignUser(): void {
    const workspace = this.selectedWorkspace();
    if (!workspace || !this.selectedUserId()) return;
    this.workspaceService.assignUser(workspace.id, {
      userId: this.selectedUserId(),
      role: this.selectedRole(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.openUserDialog(workspace);
        this.selectedUserId.set('');
      },
      error: () => { /* user assignment error handled silently */ },
    });
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
    this.newWorkspaceName.set('');
    this.newWorkspaceDescription.set('');
    this.createError.set('');
  }

  closeUserDialog(): void {
    this.showUserDialog.set(false);
    this.selectedWorkspace.set(null);
    this.loadWorkspaces();
  }
}
