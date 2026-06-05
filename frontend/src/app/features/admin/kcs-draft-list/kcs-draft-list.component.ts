import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgFor, NgIf, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { KcsDraftService } from '../kcs-draft.service';
import { KcsDraft } from '../kcs-draft.model';

@Component({
  selector: 'app-kcs-draft-list',
  standalone: true,
  imports: [NgFor, NgIf, DatePipe, RouterLink],
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
  actionLoading = signal<string | null>(null); // tracks which draft ID is being processed

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
        this.errorMessage.set('Failed to load KCS drafts.');
        this.isLoading.set(false);
      },
    });
  }

  approveDraft(id: string): void {
    if (!confirm('Approve this draft? It will be published immediately.')) {
      return;
    }
    this.actionLoading.set(id);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.kcsDraftService.approveDraft(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.successMessage.set('Draft approved and published.');
        this.actionLoading.set(null);
        this.loadDrafts();
      },
      error: () => {
        this.errorMessage.set('Failed to approve draft.');
        this.actionLoading.set(null);
      },
    });
  }

  rejectDraft(id: string): void {
    if (!confirm('Reject this draft? It will be archived.')) {
      return;
    }
    this.actionLoading.set(id);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.kcsDraftService.rejectDraft(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.successMessage.set('Draft rejected and archived.');
        this.actionLoading.set(null);
        this.loadDrafts();
      },
      error: () => {
        this.errorMessage.set('Failed to reject draft.');
        this.actionLoading.set(null);
      },
    });
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.loadDrafts();
  }
}
