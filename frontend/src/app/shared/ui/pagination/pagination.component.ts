import { Component, input, output } from '@angular/core';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [NgIf],
  template: `
    <div *ngIf="totalPages() > 1" class="flex items-center justify-center gap-2 mt-6">
      <button
        (click)="goToPage(currentPage() - 1)"
        [disabled]="currentPage() === 0"
        class="px-3 py-1.5 text-sm rounded border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Previous
      </button>
      <span class="text-sm text-slate-600">
        Page {{ currentPage() + 1 }} of {{ totalPages() }}
      </span>
      <button
        (click)="goToPage(currentPage() + 1)"
        [disabled]="currentPage() >= totalPages() - 1"
        class="px-3 py-1.5 text-sm rounded border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Next
      </button>
    </div>
  `,
})
export class PaginationComponent {
  currentPage = input(0);
  totalPages = input(0);
  pageChange = output<number>();

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.pageChange.emit(page);
    }
  }
}
