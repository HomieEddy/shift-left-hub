import { Component, inject } from '@angular/core';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-skeleton-card',
  standalone: true,
  templateUrl: './skeleton-card.component.html',
})
export class SkeletonCardComponent {
  protected translationService = inject(TranslationService);
}
