import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TagService } from '../../services/tag.service';
import { TagDto, CreateTagRequest } from '../../models/tag.models';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../../shared/ui/confirmation-dialog/confirmation-dialog.service';

@Component({
  selector: 'app-tag-manager',
  standalone: true,
  imports: [FormsModule, NgFor, NgIf],
  templateUrl: './tag-manager.component.html',
})
export class TagManagerComponent implements OnInit {
  private tagService = inject(TagService);
  private destroyRef = inject(DestroyRef);
  private confirmationDialog = inject(ConfirmationDialogService);
  protected translationService = inject(TranslationService);

  tags = signal<TagDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  showForm = signal(false);
  editingTag = signal<TagDto | null>(null);

  formNameEn = '';
  formNameFr = '';
  formColor = '#3B82F6';

  ngOnInit(): void {
    this.loadTags();
  }

  loadTags(): void {
    this.isLoading.set(true);
    this.tagService.getTags().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (data) => {
        this.tags.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load tags.');
        this.isLoading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingTag.set(null);
    this.formNameEn = '';
    this.formNameFr = '';
    this.formColor = '#3B82F6';
    this.showForm.set(true);
  }

  openEdit(tag: TagDto): void {
    this.editingTag.set(tag);
    this.formNameEn = tag.nameEn;
    this.formNameFr = tag.nameFr;
    this.formColor = tag.color;
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingTag.set(null);
  }

  saveTag(): void {
    const editing = this.editingTag();
    const request = { nameEn: this.formNameEn, nameFr: this.formNameFr, color: this.formColor };

    const action = editing
      ? this.tagService.updateTag(editing.id, request)
      : this.tagService.createTag(request);

    action.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.loadTags();
        this.cancelForm();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.error || 'Failed to save tag.');
      },
    });
  }

  deleteTag(id: string, nameEn: string): void {
    this.confirmationDialog.confirm({
      titleKey: 'confirm.title.delete',
      messageKey: 'Delete tag "' + nameEn + '"?',
      confirmLabelKey: 'confirm.label.delete',
    }).subscribe((confirmed) => {
      if (confirmed) {
        this.tagService.deleteTag(id).pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe({
          next: () => this.loadTags(),
          error: (err) => {
            this.errorMessage.set(err.error?.error || 'Cannot delete tag: it is used by articles.');
          },
        });
      }
    });
  }
}
