import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { MarkdownModule } from 'ngx-markdown';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { $localize } from '@angular/localize/init';
import { TicketService } from '../ticket.service';
import { Ticket, ShiftLeftContext } from '../ticket.model';
import { statusBadgeClass } from '../../../shared/ui/badge/badge-utils';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [DatePipe, RouterLink, MarkdownModule],
  templateUrl: './ticket-detail.component.html',
})
export class TicketDetailComponent implements OnInit {
  private ticketService = inject(TicketService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  ticket = signal<Ticket | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  showCancelConfirm = signal(false);
  showTranscript = signal(false);
  showSources = signal(false);

  readonly statusBadgeClass = statusBadgeClass;

  parsedContext = computed(() => {
    const raw = this.ticket()?.shiftLeftContext;
    if (raw == null) return null;
    try {
      return JSON.parse(raw) as ShiftLeftContext;
    } catch {
      return null;
    }
  });

  transcriptMessages = computed(() => this.parsedContext()?.transcript ?? []);
  kbSources = computed(() => this.parsedContext()?.sources ?? []);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id != null) {
      this.loadTicket(id);
    }
  }

  private loadTicket(id: string): void {
    this.isLoading.set(true);
    this.ticketService.getTicket(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set($localize`:@@tickets.detail.error.load:Failed to load ticket.`);
      },
    });
  }

  cancelTicket(): void {
    const t = this.ticket();
    if (!t) return;

    this.ticketService.cancelTicket(t.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        void this.router.navigate(['/tickets']);
      },
      error: () => {
        this.errorMessage.set($localize`:@@tickets.detail.error.cancel:Failed to cancel ticket.`);
      },
    });
  }
}
