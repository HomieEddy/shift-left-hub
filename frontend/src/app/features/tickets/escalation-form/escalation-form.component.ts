import { Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TicketService } from '../ticket.service';
import { EscalationPayload } from '../ticket.model';
import { TranslationService } from '../../../core/i18n/translation.service';

@Component({
  selector: 'app-escalation-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './escalation-form.component.html',
})
export class EscalationFormComponent {
  private ticketService = inject(TicketService);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  escalationPayload = input<EscalationPayload | null>(null);
  ticketCreated = output<string>();
  cancelled = output<void>();

  issue = signal('');
  category = signal<string>('NETWORK');
  urgency = signal<string>('LOW');
  isSubmitting = signal(false);
  successTicketNumber = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  categories = computed(() => [
    { value: 'NETWORK', label: this.translationService.translate('tickets.category.network') },
    { value: 'HARDWARE', label: this.translationService.translate('tickets.category.hardware') },
    { value: 'SOFTWARE', label: this.translationService.translate('tickets.category.software') },
    { value: 'ACCESS', label: this.translationService.translate('tickets.category.access') },
    {
      value: 'PERIPHERALS',
      label: this.translationService.translate('tickets.category.peripherals'),
    },
  ]);
  urgencies = computed(() => [
    { value: 'LOW', label: this.translationService.translate('agent.urgency.low') },
    { value: 'MEDIUM', label: this.translationService.translate('agent.urgency.medium') },
    { value: 'HIGH', label: this.translationService.translate('agent.urgency.high') },
  ]);

  submit(): void {
    if (this.isSubmitting() || !this.issue().trim()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const payload = this.escalationPayload();
    const shiftLeftContext = payload
      ? JSON.stringify({
          issue: payload.issue,
          category: this.category(),
          urgency: this.urgency(),
          transcript: payload.transcript,
          sources: payload.sources,
          aiSummary: '',
          confidenceScore: 0,
        })
      : undefined;

    this.ticketService
      .createTicket({
        issue: this.issue(),
        category: this.category(),
        urgency: this.urgency(),
        shiftLeftContext,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ticket) => {
          this.isSubmitting.set(false);
          this.successTicketNumber.set(ticket.ticketNumber);
          this.ticketCreated.emit(ticket.ticketNumber);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.errorMessage.set(this.translationService.translate('escalation.error.create'));
        },
      });
  }

  dismiss(): void {
    this.cancelled.emit();
  }
}
