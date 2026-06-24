import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  effect,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.css',
})
export class ModalComponent {
  protected translationService = inject(TranslationService);
  private cdr = inject(ChangeDetectorRef);
  private host = inject(ElementRef<HTMLElement>);
  private previouslyFocused: HTMLElement | null = null;
  open = input(false);
  title = input('');
  width = input('max-w-md');
  closed = output<void>();
  protected dialogRef = viewChild<ElementRef<HTMLDivElement>>('dialog');

  constructor() {
    effect(() => {
      const isOpen = this.open();
      if (isOpen) {
        this.previouslyFocused = document.activeElement as HTMLElement | null;
        queueMicrotask(() => this.focusFirst());
      } else if (this.previouslyFocused) {
        this.previouslyFocused.focus();
        this.previouslyFocused = null;
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }

  private focusFirst(): void {
    const dialog = this.dialogRef()?.nativeElement;
    if (!dialog) return;
    const focusable = dialog.querySelector<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    );
    focusable?.focus();
  }
}
