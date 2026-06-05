import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe, NgClass } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AgentTicketService } from '../agent-ticket.service';
import { AgentTicket, WorkNote } from '../agent-ticket.model';

@Component({
  selector: 'app-agent-ticket-detail',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, NgClass, RouterLink, FormsModule],
  templateUrl: './agent-ticket-detail.component.html',
})
/**
 * Smart component that displays the detail view of a single agent ticket,
 * including work notes, context sections, and claim/resolve actions.
 */
export class AgentTicketDetailComponent implements OnInit {
  private agentTicketService = inject(AgentTicketService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  ticket = signal<AgentTicket | null>(null);
  workNotes = signal<WorkNote[]>([]);
  isLoading = signal(true);
  isError = signal(false);

  contextExpanded = signal(false);
  transcriptExpanded = signal(false);
  sourcesExpanded = signal(false);

  newWorkNote = signal('');
  resolutionNotes = signal('');
  isKnowledgeGap = signal(false);
  isSubmittingNote = signal(false);
  isResolving = signal(false);

  resolveConfirmOpen = signal(false);
  isClaiming = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTicket(id);
      this.loadWorkNotes(id);
    }
  }

  /** Loads the full ticket detail from the API. */
  loadTicket(id: string): void {
    this.isLoading.set(true);
    this.isError.set(false);
    this.agentTicketService.getTicket(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.isError.set(true);
      },
    });
  }

  /** Loads work notes for the ticket from the API. */
  loadWorkNotes(id: string): void {
    this.agentTicketService.getWorkNotes(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (notes) => this.workNotes.set(notes),
      error: () => {},
    });
  }

  /** Submits a new work note and refreshes the work notes list. */
  addWorkNote(): void {
    const content = this.newWorkNote().trim();
    if (!content) return;
    const ticketId = this.ticket()?.id;
    if (!ticketId) return;

    this.isSubmittingNote.set(true);
    this.agentTicketService.addWorkNote(ticketId, content).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.newWorkNote.set('');
        this.isSubmittingNote.set(false);
        this.loadWorkNotes(ticketId);
      },
      error: () => {
        this.isSubmittingNote.set(false);
        alert('Failed to add work note.');
      },
    });
  }

  /** Claims the current ticket for the authenticated agent. */
  claimTicket(): void {
    const ticketId = this.ticket()?.id;
    if (!ticketId) return;

    this.isClaiming.set(true);
    this.agentTicketService.claimTicket(ticketId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.isClaiming.set(false);
      },
      error: () => {
        this.isClaiming.set(false);
        alert('Failed to claim ticket.');
      },
    });
  }

  /** Opens the resolve confirmation dialog. */
  openResolveConfirm(): void {
    this.resolveConfirmOpen.set(true);
  }

  /** Confirms ticket resolution and submits to the API. */
  confirmResolve(): void {
    const ticketId = this.ticket()?.id;
    if (!ticketId || !this.resolutionNotes().trim()) return;

    this.isResolving.set(true);
    this.resolveConfirmOpen.set(false);
    this.agentTicketService.resolveTicket(ticketId, {
      resolutionNotes: this.resolutionNotes().trim(),
      isKnowledgeGap: this.isKnowledgeGap(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.isResolving.set(false);
      },
      error: () => {
        this.isResolving.set(false);
        alert('Failed to resolve ticket.');
      },
    });
  }

  /** Cancels the resolve confirmation dialog. */
  cancelResolveConfirm(): void {
    this.resolveConfirmOpen.set(false);
  }

  statusLabels: Record<string, string> = {
    'NEW': 'New',
    'IN_PROGRESS': 'In Progress',
    'RESOLVED': 'Resolved',
    'CANCELLED': 'Cancelled',
  };

  urgencyLabels: Record<string, string> = {
    'LOW': 'Low',
    'MEDIUM': 'Medium',
    'HIGH': 'High',
  };

  categoryLabels: Record<string, string> = {
    'NETWORK': 'Network',
    'HARDWARE': 'Hardware',
    'SOFTWARE': 'Software',
    'ACCESS': 'Access',
    'PERIPHERALS': 'Peripherals',
  };

  /** Returns the appropriate Tailwind badge classes for a ticket status. */
  statusBadgeClass(status: string): string {
    switch (status) {
      case 'NEW': return 'bg-blue-100 text-blue-700';
      case 'IN_PROGRESS': return 'bg-amber-100 text-amber-700';
      case 'RESOLVED': return 'bg-green-100 text-green-700';
      case 'CANCELLED': return 'bg-gray-100 text-gray-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  /** Returns the appropriate Tailwind badge classes for an urgency level. */
  urgencyBadgeClass(urgency: string): string {
    switch (urgency) {
      case 'HIGH': return 'bg-red-100 text-red-700';
      case 'MEDIUM': return 'bg-amber-100 text-amber-700';
      case 'LOW': return 'bg-gray-100 text-gray-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  /** Returns the appropriate Tailwind badge classes for a ticket category. */
  categoryBadgeClass(category: string): string {
    switch (category) {
      case 'NETWORK': return 'bg-purple-100 text-purple-700';
      case 'HARDWARE': return 'bg-cyan-100 text-cyan-700';
      case 'SOFTWARE': return 'bg-indigo-100 text-indigo-700';
      case 'ACCESS': return 'bg-teal-100 text-teal-700';
      case 'PERIPHERALS': return 'bg-pink-100 text-pink-700';
      default: return 'bg-slate-100 text-slate-600';
    }
  }
}
