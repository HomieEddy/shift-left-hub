import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { KcsDraftService } from '../kcs-draft.service';
import { KcsDraft } from '../kcs-draft.model';
import { ModalComponent } from '../../../shared/ui/modal/modal.component';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-kcs-draft-list',
  standalone: true,
  imports: [DatePipe, RouterLink, ModalComponent],
  templateUrl: './kcs-draft-list.component.html',
})
export class KcsDraftListComponent implements OnInit {
  private kcsDraftService = inject(KcsDraftService);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  drafts = signal<KcsDraft[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');
  currentPage = signal(0);
  totalPages = signal(0);
  displayPage = computed(() => this.currentPage() + 1);
  actionLoading = signal<string | null>(null); // tracks which draft ID is being processed
  confirmActionTitle = this.translationService.translate('kcs.drafts.confirm.title');
  confirmModalOpen = signal(false);
  pendingAction = signal<{ type: 'approve' | 'reject'; id: string; title: string } | null>(null);

  ngOnInit(): void {
    this.loadDrafts();
  }

  loadDrafts(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.kcsDraftService.getDrafts(this.currentPage()).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (page) => {
        this.drafts.set(page.content);
        this.totalPages.set(page.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translationService.translate('kcs.drafts.error.load'));
        this.isLoading.set(false);
      },
    });
  }

  requestApprove(draft: KcsDraft): void {
    this.pendingAction.set({ type: 'approve', id: draft.id, title: draft.titleEn });
    this.confirmModalOpen.set(true);
  }

  requestReject(draft: KcsDraft): void {
    this.pendingAction.set({ type: 'reject', id: draft.id, title: draft.titleEn });
    this.confirmModalOpen.set(true);
  }

  executePendingAction(): void {
    const action = this.pendingAction();
    if (action == null) return;
    this.confirmModalOpen.set(false);
    this.actionLoading.set(action.id);
    this.errorMessage.set('');
    this.successMessage.set('');
    const request$ = action.type === 'approve'
      ? this.kcsDraftService.approveDraft(action.id)
      : this.kcsDraftService.rejectDraft(action.id);
    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.successMessage.set(action.type === 'approve'
          ? this.translationService.translate('kcs.drafts.success.approved')
          : this.translationService.translate('kcs.drafts.success.rejected'));
        this.actionLoading.set(null);
        this.pendingAction.set(null);
        this.loadDrafts();
      },
      error: () => {
        this.errorMessage.set(action.type === 'approve'
          ? this.translationService.translate('kcs.drafts.error.approve')
          : this.translationService.translate('kcs.drafts.error.reject'));
        this.actionLoading.set(null);
        this.pendingAction.set(null);
      },
    });
  }

  cancelAction(): void {
    this.confirmModalOpen.set(false);
    this.pendingAction.set(null);
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.loadDrafts();
  }
}
