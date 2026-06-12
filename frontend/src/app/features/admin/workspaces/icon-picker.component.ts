import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-icon-picker',
  standalone: true,
  imports: [NgClass],
  templateUrl: './icon-picker.component.html',
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
