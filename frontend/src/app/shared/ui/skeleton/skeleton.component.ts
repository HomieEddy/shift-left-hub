import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  template: `
    <div
      class="animate-pulse bg-surface-tertiary rounded"
      [class.rounded-full]="variant() === 'circle'"
      [style.width]="width()"
      [style.height]="height()"
      role="status"
      aria-label="Loading"
    ></div>
  `,
})
export class SkeletonComponent {
  readonly variant = input<'text' | 'circle' | 'rect'>('text');
  readonly width = input('100%');
  readonly height = input('1rem');
}

@Component({
  selector: 'app-skeleton-card',
  standalone: true,
  template: `
    <div class="bg-surface-primary rounded-xl border border-border-default p-6 space-y-4" role="status" aria-label="Loading">
      <div class="animate-pulse bg-surface-tertiary rounded h-5 w-1/3"></div>
      <div class="animate-pulse bg-surface-tertiary rounded h-4 w-full"></div>
      <div class="animate-pulse bg-surface-tertiary rounded h-4 w-5/6"></div>
      <div class="animate-pulse bg-surface-tertiary rounded h-4 w-3/4"></div>
    </div>
  `,
})
export class SkeletonCardComponent {}

@Component({
  selector: 'app-skeleton-table',
  standalone: true,
  template: `
    <div class="bg-surface-primary rounded-xl border border-border-default" role="status" aria-label="Loading">
      <div class="p-4 border-b border-border-default">
        <div class="animate-pulse bg-surface-tertiary rounded h-4 w-1/4"></div>
      </div>
      @for (row of skeletonRows(); track $index) {
        <div class="px-4 py-3 border-b border-border-default flex items-center gap-4">
          <div class="animate-pulse bg-surface-tertiary rounded h-4 w-2/5"></div>
          <div class="animate-pulse bg-surface-tertiary rounded h-4 w-1/4"></div>
          <div class="animate-pulse bg-surface-tertiary rounded h-4 w-1/6"></div>
          <div class="animate-pulse bg-surface-tertiary rounded h-4 w-1/12 ml-auto"></div>
        </div>
      }
    </div>
  `,
})
export class SkeletonTableComponent {
  readonly rows = input(5);
  protected skeletonRows = computed(() =>
    Array.from({ length: this.rows() }, (_, i) => i)
  );
}
