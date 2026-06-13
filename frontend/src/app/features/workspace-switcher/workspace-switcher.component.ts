import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { WorkspaceRoleService } from '../../core/auth/workspace-role.service';
import { WorkspaceService } from '../admin/workspaces/workspace.service';
import { WorkspaceDto } from '../admin/workspaces/workspace.model';
import { TranslationService } from '../../core/i18n/translation.service';
import { LucideChevronsUpDown, LucideCheck, LucideBuilding2 } from '@lucide/angular';

@Component({
  selector: 'app-workspace-switcher',
  standalone: true,
  imports: [LucideChevronsUpDown, LucideCheck, LucideBuilding2],
  templateUrl: './workspace-switcher.component.html',
})
export class WorkspaceSwitcherComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  private authService = inject(AuthService);
  private workspaceRoleService = inject(WorkspaceRoleService);
  private router = inject(Router);
  protected translationService = inject(TranslationService);
  private destroyRef = inject(DestroyRef);

  workspaces = signal<WorkspaceDto[]>([]);
  isOpen = signal(false);

  currentWorkspace = computed(() => {
    const user = this.authService.user();
    const list = this.workspaces();
    if (!user) { return null; }
    // Find workspace matching JWT workspace_id from user details
    const found = list.find(ws => ws.id === user.workspaceId);
    return found ?? list[0] ?? null;
  });

  ngOnInit() {
    this.workspaceService.getMyWorkspaces()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(ws => this.workspaces.set(ws));
  }

  toggleDropdown() {
    this.isOpen.update(v => !v);
  }

  switchWorkspace(workspace: WorkspaceDto) {
    this.authService.switchWorkspace(workspace.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.workspaceRoleService.refreshRole();
          this.isOpen.set(false);
          void this.router.navigateByUrl('/');
        },
        error: () => this.isOpen.set(false),
      });
  }

  isDefaultWorkspace(ws: WorkspaceDto | null): boolean {
    return ws?.slug === 'default';
  }
}
