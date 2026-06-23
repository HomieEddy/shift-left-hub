import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, signal } from '@angular/core';
import { ModalComponent } from './modal.component';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  standalone: true,
  imports: [ModalComponent],
  template: `
    <button (click)="trigger.set(true)">Open</button>
    <app-modal [open]="open()" [title]="title()" (closed)="closed.set(true)">
      <p>Body content</p>
    </app-modal>
  `,
})
class HostComponent {
  open = signal(false);
  trigger = signal(false);
  closed = signal(false);
  title = signal('Test Title');
}

describe('ModalComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let root: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [
        {
          provide: TranslationService,
          useValue: { translate: (key: string) => key },
        },
      ],
    });
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    root = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  it('does not render content when closed', () => {
    expect(root.querySelector('[role="dialog"]')).toBeNull();
  });

  it('renders dialog with role and aria-modal when open', () => {
    host.open.set(true);
    fixture.detectChanges();
    const dialog = root.querySelector('[role="dialog"]');
    expect(dialog).not.toBeNull();
    expect(dialog?.getAttribute('aria-modal')).toBe('true');
    expect(dialog?.getAttribute('aria-labelledby')).toBe('modal-title');
  });

  it('emits closed when close button is clicked', () => {
    host.open.set(true);
    fixture.detectChanges();
    const closeBtn = root.querySelector<HTMLButtonElement>(
      'button[aria-label*="a11y.close-modal"]',
    );
    expect(closeBtn).not.toBeNull();
    closeBtn?.click();
    fixture.detectChanges();
    expect(host.closed()).toBe(true);
  });

  it('emits closed on Escape keydown when open', () => {
    host.open.set(true);
    fixture.detectChanges();
    const event = new KeyboardEvent('keydown', { key: 'Escape' });
    document.dispatchEvent(event);
    expect(host.closed()).toBe(true);
  });
});
