import { Component, input } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  templateUrl: './skeleton.component.html',
})
export class SkeletonComponent {
  readonly variant = input<'text' | 'circle' | 'rect'>('text');
  readonly width = input('100%');
  readonly height = input('1rem');
}
