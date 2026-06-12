import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass } from '@angular/common';
import { WorkspaceService } from './workspace.service';
import { WorkspaceDto } from './workspace.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { WorkspaceMembersComponent } from './workspace-members.component';
import { WorkspaceSettingsComponent } from './workspace-settings.component';

@Component({
  selector: 'app-workspace-detail',
  standalone: true,
  imports: [NgClass, WorkspaceMembersComponent, WorkspaceSettingsComponent],
  template: `
    <div class="p-6 max-w-6xl mx-auto">
      <button (click)="goBack()" class="text-sm text-text-secondary hover:text-text-primary cursor-pointer flex items-center gap-1 mb-4">
        &larr; {{ translationService.translate('kb.back') }}
      </button>

      @if (workspace(); as ws) {
        <div class="flex items-center gap-3 mb-6">
          <div class="w-10 h-10 rounded-xl bg-accent-info/10 flex items-center justify-center text-accent-info text-lg font-bold">
            {{ ws.name.charAt(0).toUpperCase() }}
          </div>
          <div>
            <h1 class="text-2xl font-bold text-text-primary">{{ ws.name }}</h1>
            <p class="text-sm text-text-secondary">{{ ws.slug }}</p>
          </div>
        </div>

        <div class="flex gap-1 border-b border-border-default mb-6">
          @for (tab of TABS; track tab.id) {
            <button (click)="switchTab(tab.id)"
              class="px-4 py-2 text-sm font-medium transition-colors border-b-2 -mb-px"
              [class.border-accent-info]="activeTab() === tab.id"
              [class.text-accent-info]="activeTab() === tab.id"
              [class.border-transparent]="activeTab() !== tab.id"
              [class.text-text-secondary]="activeTab() !== tab.id"
              [class.hover:text-text-primary]="activeTab() !== tab.id">
              {{ translationService.translate(tab.label) }}
            </button>
          }
        </div>

        @switch (activeTab()) {
          @case ('members') { <app-workspace-members [workspaceId]="ws.id" /> }
          @case ('settings') { <app-workspace-settings [workspace]="ws" /> }
          @default { <app-workspace-members [workspaceId]="ws.id" /> }
        }
      }

      @if (isLoading()) {
        <div class="text-center py-12 text-text-tertiary">{{ translationService.translate('admin.workspaces.loading') }}</div>
      }
      @if (errorMessage()) {
        <div class="bg-accent-danger-muted text-accent-danger px-4 py-3 rounded-lg">{{ errorMessage() }}</div>
      }
    </div>
  `,
})
export class WorkspaceDetailComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  protected translationService = inject(TranslationService);
  private destroyRef = inject(DestroyRef);

  readonly TABS = [
    { id: 'members', label: 'admin.workspaces.detail.tab.members' },
    { id: 'llm', label: 'admin.workspaces.detail.tab.llm' },
    { id: 'documents', label: 'admin.workspaces.detail.tab.documents' },
    { id: 'settings', label: 'admin.workspaces.detail.tab.settings' },
  ] as const;

  workspace = signal<WorkspaceDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal('');
  activeTab = signal<'members' | 'llm' | 'documents' | 'settings'>('members');

  ngOnInit() {
    const idOrNull = this.route.snapshot.paramMap.get('id');
    const id = idOrNull as string;
    if (!idOrNull) {
      this.errorMessage.set(this.translationService.translate('admin.workspaces.detail.not-found'));
      this.isLoading.set(false);
      return;
    }
    this.workspaceService.getWorkspace(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ws => { this.workspace.set(ws); this.isLoading.set(false); },
        error: () => { this.errorMessage.set(this.translationService.translate('admin.workspaces.detail.not-found')); this.isLoading.set(false); },
      });
  }

  switchTab(tabId: string) {
    this.activeTab.set(tabId as 'members' | 'llm' | 'documents' | 'settings');
    void this.router.navigate(['/admin/workspaces', this.workspace()?.id as string], { fragment: tabId });
  }

  goBack() {
    void this.router.navigate(['/admin/workspaces']);
  }
}
