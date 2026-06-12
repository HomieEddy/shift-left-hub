import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-icon-picker',
  standalone: true,
  imports: [NgClass],
  template: `
    <div class="grid grid-cols-5 gap-2">
      @for (iconName of ICONS; track iconName) {
        <button
          type="button"
          (click)="selectIcon(iconName)"
          class="flex items-center justify-center w-10 h-10 rounded-lg border transition-colors text-sm"
          [class.border-accent-info]="selectedIcon() === iconName"
          [class.bg-accent-info-muted]="selectedIcon() === iconName"
          [class.border-border-default]="selectedIcon() !== iconName"
          [class.hover:bg-surface-tertiary]="selectedIcon() !== iconName"
          [title]="iconName">
          {{ iconName.charAt(0).toUpperCase() }}
        </button>
      }
    </div>
  `,
})
export class IconPickerComponent {
  readonly ICONS = [
    'building2', 'globe', 'rocket', 'heart', 'star', 'cloud',
    'zap', 'shield', 'book-open', 'settings', 'palette',
    'compass', 'award', 'lightbulb', 'layers',
  ];

  @Input() set selected(value: string | null) {
    this.selectedIcon.set(value);
  }
  @Output() iconChange = new EventEmitter<string>();

  selectedIcon = signal<string | null>(null);

  selectIcon(iconName: string) {
    this.selectedIcon.set(iconName);
    this.iconChange.emit(iconName);
  }
}
