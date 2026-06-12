import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-skeleton-table',
  standalone: true,
  templateUrl: './skeleton-table.component.html',
})
export class SkeletonTableComponent {
  readonly rows = input(5);
  protected skeletonRows = computed(() =>
    Array.from({ length: this.rows() }, (_, i) => i)
  );
}
