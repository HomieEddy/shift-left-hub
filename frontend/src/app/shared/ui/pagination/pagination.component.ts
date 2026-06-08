import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  template: `
    @if (totalPages() > 1) {
      <div class="flex items-center justify-center gap-2 mt-6">
      <button
        (click)="goToPage(currentPage() - 1)"
        [disabled]="currentPage() === 0"
        class="px-3 py-1.5 text-sm rounded border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
        i18n="@@pagination.previous"
      >
        Previous
      </button>
      <span class="text-sm text-slate-600" i18n="@@pagination.pageOf">
        Page {{ currentPage() + 1 }} of {{ totalPages() }}
      </span>
      <button
        (click)="goToPage(currentPage() + 1)"
        [disabled]="currentPage() >= totalPages() - 1"
        class="px-3 py-1.5 text-sm rounded border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
        i18n="@@pagination.next"
      >
        Next
      </button>
      </div>
    }
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
