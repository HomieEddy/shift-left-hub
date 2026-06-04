import { Component, inject, input, output, signal } from '@angular/core';
import { NgIf, NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TicketService } from '../ticket.service';
import { EscalationPayload } from '../ticket.model';

@Component({
  selector: 'app-escalation-form',
  standalone: true,
  imports: [NgIf, NgFor, FormsModule, RouterLink],
  templateUrl: './escalation-form.component.html',
})
export class EscalationFormComponent {
  private ticketService = inject(TicketService);

  escalationPayload = input<EscalationPayload | null>(null);
  ticketCreated = output<string>();
  cancelled = output<void>();

  issue = signal('');
  category = signal<string>('NETWORK');
  urgency = signal<string>('LOW');
  isSubmitting = signal(false);
  successTicketNumber = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  categories = [
    { value: 'NETWORK', label: 'Network' },
    { value: 'HARDWARE', label: 'Hardware' },
    { value: 'SOFTWARE', label: 'Software' },
    { value: 'ACCESS', label: 'Access' },
    { value: 'PERIPHERALS', label: 'Peripherals' },
  ];
  urgencies = [
    { value: 'LOW', label: 'Low' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HIGH', label: 'High' },
  ];

  submit(): void {
    if (!this.issue().trim()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const payload = this.escalationPayload();
    const shiftLeftContext = payload ? JSON.stringify({
      issue: payload.issue,
      category: this.category(),
      urgency: this.urgency(),
      transcript: payload.transcript,
      sources: payload.sources,
      aiSummary: '',
      confidenceScore: 0,
    }) : undefined;

    this.ticketService.createTicket({
      issue: this.issue(),
      category: this.category(),
      urgency: this.urgency(),
      shiftLeftContext,
    }).subscribe({
      next: (ticket) => {
        this.isSubmitting.set(false);
        this.successTicketNumber.set(ticket.ticketNumber);
        this.ticketCreated.emit(ticket.ticketNumber);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('Failed to create ticket. Please try again.');
      },
    });
  }

  dismiss(): void {
    this.cancelled.emit();
  }
}
