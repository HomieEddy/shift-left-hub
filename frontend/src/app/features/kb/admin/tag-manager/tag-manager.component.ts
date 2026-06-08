import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { $localize as _localize } from '@angular/localize';

const $localize = _localize as unknown as (messageParts: TemplateStringsArray, ...args: unknown[]) => string;
import { TagService } from '../../services/tag.service';
import { TagDto } from '../../models/tag.models';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../../shared/ui/confirmation-dialog/confirmation-dialog.service';

@Component({
  selector: 'app-tag-manager',
  standalone: true,
  imports: [FormsModule],
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
        this.errorMessage.set($localize`:@@kb.tags.error.load:Failed to load tags.`);
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

    const action = editing !== null
      ? this.tagService.updateTag(editing.id, request)
      : this.tagService.createTag(request);

    action.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.loadTags();
        this.cancelForm();
      },
      error: (err: unknown) => {
        const serverError = err !== null && typeof err === 'object'
          ? (err as Record<string, unknown>).error
          : undefined;
        const detail = serverError !== null && typeof serverError === 'object'
          ? (serverError as Record<string, unknown>).error
          : undefined;
        this.errorMessage.set(typeof detail === 'string' ? detail : 'Failed to save tag.');
      },
    });
  }

  deleteTag(id: string, nameEn: string): void {
    this.confirmationDialog.confirm({
      title: $localize`:@@confirm.title.delete:Delete Confirmation`,
      message: $localize`:@@confirm.message.delete-tag:Delete tag "${nameEn}"?`,
      confirmLabel: $localize`:@@confirm.label.delete:Delete`,
    }).subscribe((confirmed) => {
      if (confirmed === true) {
        this.tagService.deleteTag(id).pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe({
          next: () => this.loadTags(),
          error: (err: unknown) => {
            const serverError = err !== null && typeof err === 'object'
              ? (err as Record<string, unknown>).error
              : undefined;
            const detail = serverError !== null && typeof serverError === 'object'
              ? (serverError as Record<string, unknown>).error
              : undefined;
            this.errorMessage.set(typeof detail === 'string' ? detail : $localize`:@@kb.tags.error.delete:This tag is in use and cannot be deleted.`);
          },
        });
      }
    });
  }
}
