import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { $localize } from '@angular/localize/init';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { KcsDraftService } from '../kcs-draft.service';
import { KcsDraft } from '../kcs-draft.model';
import { ModalComponent } from '../../../shared/ui/modal/modal.component';

@Component({
  selector: 'app-kcs-draft-list',
  standalone: true,
  imports: [DatePipe, RouterLink, ModalComponent],
  templateUrl: './kcs-draft-list.component.html',
})
export class KcsDraftListComponent implements OnInit {
  private kcsDraftService = inject(KcsDraftService);
  private destroyRef = inject(DestroyRef);

  drafts = signal<KcsDraft[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');
  currentPage = signal(0);
  totalPages = signal(0);
  displayPage = computed(() => this.currentPage() + 1);
  actionLoading = signal<string | null>(null); // tracks which draft ID is being processed
  confirmActionTitle: string = $localize`:@@kcs.drafts.confirm.title:Confirm Action`;
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
        this.errorMessage.set($localize`:@@kcs.drafts.error.load:Failed to load KCS drafts.`);
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
          ? $localize`:@@kcs.drafts.success.approved:Draft approved and published.`
          : $localize`:@@kcs.drafts.success.rejected:Draft rejected and archived.`);
        this.actionLoading.set(null);
        this.pendingAction.set(null);
        this.loadDrafts();
      },
      error: () => {
        this.errorMessage.set(action.type === 'approve'
          ? $localize`:@@kcs.drafts.error.approve:Failed to approve draft.`
          : $localize`:@@kcs.drafts.error.reject:Failed to reject draft.`);
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
