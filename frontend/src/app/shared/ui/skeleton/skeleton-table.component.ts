import { Component, computed, inject, input } from '@angular/core';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-skeleton-table',
  standalone: true,
  templateUrl: './skeleton-table.component.html',
})
export class SkeletonTableComponent {
  protected translationService = inject(TranslationService);
  readonly rows = input(5);
  protected skeletonRows = computed(() => Array.from({ length: this.rows() }, (_, i) => i));
}
