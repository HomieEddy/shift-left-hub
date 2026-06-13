import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WorkspaceService } from './workspace.service';
import { WorkspaceDto } from './workspace.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { WorkspaceMembersComponent } from './workspace-members.component';
import { WorkspaceSettingsComponent } from './workspace-settings.component';

@Component({
  selector: 'app-workspace-detail',
  standalone: true,
  imports: [WorkspaceMembersComponent, WorkspaceSettingsComponent],
  templateUrl: './workspace-detail.component.html',
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
    if (idOrNull === null) {
      this.errorMessage.set(this.translationService.translate('admin.workspaces.detail.not-found'));
      this.isLoading.set(false);
      return;
    }
    this.workspaceService.getWorkspace(idOrNull)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ws => { this.workspace.set(ws); this.isLoading.set(false); },
        error: () => { this.errorMessage.set(this.translationService.translate('admin.workspaces.detail.not-found')); this.isLoading.set(false); },
      });
  }

  switchTab(tabId: string) {
    this.activeTab.set(tabId as 'members' | 'llm' | 'documents' | 'settings');
    const ws = this.workspace();
    if (ws !== null) {
      void this.router.navigate(['/admin/workspaces', ws.id], { fragment: tabId });
    }
  }

  goBack() {
    void this.router.navigate(['/admin/workspaces']);
  }
}
