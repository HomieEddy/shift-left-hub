import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { WorkspaceRoleService } from '../../core/auth/workspace-role.service';
import { WorkspaceService } from '../admin/workspaces/workspace.service';
import { WorkspaceDto } from '../admin/workspaces/workspace.model';
import { TranslationService } from '../../core/i18n/translation.service';
import { NgClass } from '@angular/common';
import { LucideChevronsUpDown, LucideCheck, LucideBuilding2 } from '@lucide/angular';

@Component({
  selector: 'app-workspace-switcher',
  standalone: true,
  imports: [NgClass, LucideChevronsUpDown, LucideCheck, LucideBuilding2],
  template: `
    <div class="relative">
      <button
        (click)="toggleDropdown()"
        class="flex items-center gap-1.5 px-2 py-1 rounded-lg hover:bg-surface-tertiary transition-colors text-sm">
        <span class="text-text-secondary">{{ currentWorkspace()?.name || '...' }}</span>
        @if (isDefaultWorkspace(currentWorkspace())) {
          <span class="text-xs text-text-tertiary">{{ translationService.translate('workspace.default-suffix') }}</span>
        }
        <svg lucideChevronsUpDown class="w-3.5 h-3.5 text-text-tertiary" />
      </button>

      @if (isOpen()) {
        <div class="absolute right-0 top-full mt-1 w-64 bg-surface-primary rounded-xl shadow-xl border border-border-default py-1 z-50">
          @for (ws of workspaces(); track ws.id) {
            <button
              (click)="switchWorkspace(ws)"
              class="w-full flex items-center gap-3 px-3 py-2 hover:bg-surface-secondary transition-colors text-sm text-left">
              <span class="text-text-secondary w-5 h-5 flex items-center justify-center">
                <svg lucideBuilding2 class="w-4 h-4" />
              </span>
              <span class="flex-1">{{ ws.name }}</span>
              @if (isDefaultWorkspace(ws)) {
                <span class="text-xs text-text-tertiary">{{ translationService.translate('workspace.default-suffix') }}</span>
              }
              @if (ws.id === currentWorkspace()?.id) {
                <svg lucideCheck class="w-4 h-4 text-accent-info" />
              }
            </button>
          }
        </div>
      }
    </div>
  `,
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
