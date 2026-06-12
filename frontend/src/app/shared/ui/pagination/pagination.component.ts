import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  templateUrl: './pagination.component.html',
})
export class PaginationComponent {
  previousLabel = input('Previous');
  nextLabel = input('Next');
  pageOfLabel = input('Page 1 of 1');
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

  pageLabel(p: number | string): string {
    return typeof p === 'number' ? String(p + 1) : String(p);
  }
}
