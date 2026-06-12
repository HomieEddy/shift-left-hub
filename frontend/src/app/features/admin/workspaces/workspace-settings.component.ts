import { Component, DestroyRef, inject, Input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { WorkspaceService } from './workspace.service';
import { WorkspaceDto, UpdateWorkspaceRequest } from './workspace.model';
import { TranslationService } from '../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../shared/ui/confirmation-dialog/confirmation-dialog.service';
import { NgClass } from '@angular/common';
import { IconPickerComponent } from './icon-picker.component';

@Component({
  selector: 'app-workspace-settings',
  standalone: true,
  imports: [NgClass, FormsModule, IconPickerComponent],
  template: `
    <div class="space-y-6">
      <div class="rounded-xl border border-border-default bg-surface-primary p-6 space-y-4">
        <h3 class="text-lg font-semibold text-text-primary">{{ translationService.translate('admin.workspaces.settings.name') }}</h3>
        <div>
          <label for="ws-name" class="block text-sm font-medium text-text-secondary mb-1">{{ translationService.translate('admin.workspaces.settings.name') }}</label>
          <input id="ws-name" type="text" [ngModel]="name()" (ngModelChange)="name.set($event)"
            class="w-full rounded-lg border border-border-default px-3 py-2 text-sm bg-surface-primary text-text-primary" />
        </div>
        <div>
          <label for="ws-desc" class="block text-sm font-medium text-text-secondary mb-1">{{ translationService.translate('admin.workspaces.settings.description') }}</label>
          <textarea id="ws-desc" [ngModel]="description()" (ngModelChange)="description.set($event)" rows="3"
            class="w-full rounded-lg border border-border-default px-3 py-2 text-sm bg-surface-primary text-text-primary"></textarea>
        </div>
        <div role="group" [attr.aria-label]="translationService.translate('admin.workspaces.settings.icon')">
          <span class="block text-sm font-medium text-text-secondary mb-2">{{ translationService.translate('admin.workspaces.settings.icon') }}</span>
          <app-icon-picker [selected]="icon()" (iconChange)="onIconChange($event)" />
        </div>
        <button (click)="saveSettings()" [disabled]="isSaving()"
          class="px-4 py-2 text-sm rounded-lg bg-accent-info text-white font-medium hover:opacity-90 disabled:opacity-50">
          {{ isSaving() ? translationService.translate('common.loading') : translationService.translate('admin.workspaces.settings.save') }}
        </button>
        @if (saveMessage()) {
          <p class="text-sm text-accent-success">{{ saveMessage() }}</p>
        }
      </div>

      <div class="rounded-xl border border-accent-danger/30 bg-surface-primary p-6 space-y-4">
        <h3 class="text-lg font-semibold text-accent-danger">{{ translationService.translate('admin.workspaces.settings.danger') }}</h3>

        @if (!isDefaultWorkspace()) {
          <div>
            <p class="text-sm text-text-secondary mb-2">{{ translationService.translate('workspace.action.leave') }}</p>
            <button (click)="confirmLeaveWorkspace()"
              class="px-4 py-2 text-sm rounded-lg border border-border-default text-text-secondary hover:bg-surface-tertiary">
              {{ translationService.translate('admin.workspaces.settings.leave') }}
            </button>
          </div>
        }

        @if (!isDefaultWorkspace()) {
          <div>
            <p class="text-sm text-text-secondary mb-2">{{ translationService.translate('admin.workspaces.settings.delete') }}</p>
            <p class="text-xs text-text-tertiary mb-2">{{ translationService.translate('workspace.action.delete-desc') }}</p>
            <div class="space-y-2">
              <input type="text" [ngModel]="deleteConfirmName()" (ngModelChange)="deleteConfirmName.set($event)"
                [placeholder]="translationService.translate('workspace.action.delete-confirm')"
                class="w-full rounded-lg border border-border-default px-3 py-2 text-sm bg-surface-primary text-text-primary" />
              <button (click)="confirmDeleteWorkspace()"
                [disabled]="deleteConfirmName() !== workspace?.name"
                class="px-4 py-2 text-sm rounded-lg bg-accent-danger text-white font-medium hover:opacity-90 disabled:opacity-50">
                {{ translationService.translate('workspace.action.delete') }}
              </button>
            </div>
          </div>
        } @else {
          <p class="text-sm text-text-tertiary">{{ translationService.translate('admin.workspaces.members.only-admin') }}</p>
        }
      </div>
    </div>
  `,
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
