import { Component, DestroyRef, inject, Input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { WorkspaceService } from './workspace.service';
import { WorkspaceDto, UpdateWorkspaceRequest } from './workspace.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../shared/ui/confirmation-dialog/confirmation-dialog.service';
import { IconPickerComponent } from './icon-picker.component';

@Component({
  selector: 'app-workspace-settings',
  standalone: true,
  imports: [FormsModule, IconPickerComponent],
  templateUrl: './workspace-settings.component.html',
})
export class WorkspaceSettingsComponent implements OnInit {
  @Input({ required: true }) workspace!: WorkspaceDto;

  private workspaceService = inject(WorkspaceService);
  private router = inject(Router);
  protected translationService = inject(TranslationService);
  private confirmationDialog = inject(ConfirmationDialogService);
  private destroyRef = inject(DestroyRef);

  name = signal('');
  description = signal('');
  icon = signal<string | null>(null);
  isSaving = signal(false);
  saveMessage = signal('');
  deleteConfirmName = signal('');

  ngOnInit() {
    this.name.set(this.workspace.name);
    this.description.set(this.workspace.description ?? '');
    this.icon.set(this.workspace.icon);
  }

  onIconChange(iconName: string) {
    this.icon.set(iconName);
  }

  isDefaultWorkspace(): boolean {
    return this.workspace?.slug === 'default';
  }

  saveSettings() {
    this.isSaving.set(true);
    this.saveMessage.set('');
    const request: UpdateWorkspaceRequest = {};
    if (this.name() !== this.workspace.name) { request.name = this.name(); }
    if (this.description() !== (this.workspace.description ?? '')) { request.description = this.description(); }
    if (this.icon() !== this.workspace.icon) { request.icon = this.icon() ?? undefined; }
    this.workspaceService.updateWorkspace(this.workspace.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSaving.set(false);
          this.saveMessage.set(this.translationService.translate('admin.workspaces.settings.saved'));
        },
        error: () => { this.isSaving.set(false); },
      });
  }

  confirmDeleteWorkspace() {
    this.workspaceService.deleteWorkspace(this.workspace.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.router.navigate(['/admin/workspaces']));
  }

  confirmLeaveWorkspace() {
    this.confirmationDialog.confirm({
      title: this.translationService.translate('workspace.action.leave'),
      message: this.translationService.translate('workspace.action.leave-confirm'),
      confirmLabel: this.translationService.translate('workspace.action.leave'),
    }).subscribe(confirmed => {
      if (confirmed) {
        this.workspaceService.leaveWorkspace(this.workspace.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe(() => void this.router.navigate(['/admin/workspaces']));
      }
    });
  }
}
