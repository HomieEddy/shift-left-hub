import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { MarkdownModule } from 'ngx-markdown';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TicketService } from '../ticket.service';
import { Ticket, ShiftLeftContext } from '../ticket.model';
import { statusBadgeClass } from '../../../shared/ui/badge/badge-utils';
import { TranslationService } from '../../../core/i18n/translation.service';

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
  protected translationService = inject(TranslationService);

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
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = params.get('id');
      if (id != null) {
        this.loadTicket(id);
      }
    });
  }

  statusLabel(status: string): string {
    return this.translationService.translate('tickets.status.' + status);
  }

  categoryLabel(category: string): string {
    return this.translationService.translate('tickets.category.' + category);
  }

  urgencyLabel(urgency: string): string {
    return this.translationService.translate('tickets.urgency.' + urgency);
  }

  private loadTicket(id: string): void {
    this.isLoading.set(true);
    this.ticketService
      .getTicket(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ticket) => {
          this.ticket.set(ticket);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.errorMessage.set(this.translationService.translate('tickets.detail.error.load'));
        },
      });
  }

  cancelTicket(): void {
    const t = this.ticket();
    if (!t) return;

    this.ticketService
      .cancelTicket(t.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/tickets']);
        },
        error: () => {
          this.errorMessage.set(this.translationService.translate('tickets.detail.error.cancel'));
        },
      });
  }
}
