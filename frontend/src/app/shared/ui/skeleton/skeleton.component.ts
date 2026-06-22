import { Component, inject, input } from '@angular/core';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  templateUrl: './skeleton.component.html',
})
export class SkeletonComponent {
  protected translationService = inject(TranslationService);
  readonly variant = input<'text' | 'circle' | 'rect'>('text');
  readonly width = input('100%');
  readonly height = input('1rem');
}
