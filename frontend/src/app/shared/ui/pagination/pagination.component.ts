import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  template: `
    @if (totalPages() > 1) {
      <div class="flex items-center justify-center gap-1 mt-6">
        <button
          (click)="goToPage(currentPage() - 1)"
          [disabled]="currentPage() === 0"
          class="px-3 py-1.5 text-sm rounded border border-border-default text-text-secondary hover:bg-surface-secondary disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          i18n="@@pagination.previous"
        >
          Previous
        </button>
        <div class="hidden sm:flex items-center gap-1">
          @for (page of visiblePages(); track page) {
            @if (page === '...') {
              <span class="px-2 py-1.5 text-sm text-text-tertiary">...</span>
            } @else {
              <button
                (click)="goToPage(page)"
                class="w-8 h-8 text-sm rounded transition-colors"
                [class.bg-primary-600]="page === currentPage()"
                [class.text-white]="page === currentPage()"
                [class.text-slate-600]="page !== currentPage()"
                [class.hover:bg-slate-100]="page !== currentPage()"
              >
                {{ page + 1 }}
              </button>
            }
          }
        </div>
        <span class="sm:hidden text-sm text-text-secondary" i18n="@@pagination.pageOf">
          Page {{ currentPage() + 1 }} of {{ totalPages() }}
        </span>
        <button
          (click)="goToPage(currentPage() + 1)"
          [disabled]="currentPage() >= totalPages() - 1"
          class="px-3 py-1.5 text-sm rounded border border-border-default text-text-secondary hover:bg-surface-secondary disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
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

  visiblePages = computed(() => {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: (number | string)[] = [];

    if (total <= 7) {
      for (let i = 0; i < total; i++) { pages.push(i); }
      return pages;
    }

    pages.push(0);

    if (current > 3) {
      pages.push('...');
    }

    const start = Math.max(1, current - 1);
    const end = Math.min(total - 2, current + 1);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    if (current < total - 4) {
      pages.push('...');
    }

    pages.push(total - 1);

    return pages;
  });

  goToPage(page: number | string): void {
    if (typeof page === 'number' && page >= 0 && page < this.totalPages()) {
      this.pageChange.emit(page);
    }
  }
}
