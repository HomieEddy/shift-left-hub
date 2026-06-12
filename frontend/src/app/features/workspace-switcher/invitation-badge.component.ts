import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WorkspaceService } from '../admin/workspaces/workspace.service';
import { InvitationDto } from '../admin/workspaces/workspace.model';
import { TranslationService } from '../../core/i18n/translation.service';
import { NgClass } from '@angular/common';
import { LucideBell, LucideCheck, LucideX, LucideUsers, LucideLoader2 } from '@lucide/angular';

@Component({
  selector: 'app-invitation-badge',
  standalone: true,
  imports: [NgClass, LucideBell, LucideCheck, LucideX, LucideUsers, LucideLoader2],
  template: `
    <div class="relative">
      <button (click)="toggleDropdown()" class="relative p-2 rounded-lg hover:bg-surface-tertiary transition-colors">
        <svg lucideBell class="w-5 h-5 text-text-secondary" />
        @if (hasPending()) {
          <span class="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-accent-danger text-[10px] font-bold text-white">
            {{ pendingCount() }}
          </span>
        }
      </button>

      @if (isOpen()) {
        <div class="absolute right-0 top-full mt-1 w-80 bg-surface-primary rounded-xl shadow-xl border border-border-default py-2 z-50">
          <div class="px-3 py-1.5 text-xs font-semibold text-text-tertiary uppercase tracking-wider">
            {{ translationService.translate('workspace.switcher.invitations') }}
          </div>

          @if (isLoading()) {
            <div class="px-3 py-4 text-center text-text-tertiary text-sm">{{ translationService.translate('common.loading') }}</div>
          }

          @if (!isLoading() && invitations().length === 0) {
            <div class="px-3 py-4 text-center text-text-tertiary text-sm">{{ translationService.translate('workspace.switcher.no-invitations') }}</div>
          }

          @for (inv of invitations(); track inv.id) {
            <div class="px-3 py-2.5 hover:bg-surface-secondary transition-colors">
              <div class="flex items-start gap-3">
                <svg lucideUsers class="w-5 h-5 text-text-tertiary mt-0.5 flex-shrink-0" />
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-text-primary truncate">{{ inv.invitedUserDisplayName }}</p>
                  <p class="text-xs text-text-tertiary mt-0.5">
                    {{ translationService.translate('workspace.invitation.from') }}
                    <span class="font-medium">{{ inv.invitedBy }}</span>
                    {{ translationService.translate('workspace.invitation.for-workspace') }}
                    <span class="font-medium">{{ inv.workspaceId }}</span>
                  </p>
                  <span class="inline-block mt-1 rounded-full bg-accent-info-muted px-2 py-0.5 text-xs font-medium text-accent-info">
                    {{ translationService.translate('workspace.role.' + inv.role.toLowerCase()) }}
                  </span>
                </div>
                <div class="flex gap-1 flex-shrink-0">
                  <button (click)="acceptInvitation(inv)" [disabled]="processingIds().has(inv.id)"
                    class="p-1.5 rounded-lg bg-accent-success-muted text-accent-success hover:bg-accent-success/20 transition-colors disabled:opacity-50"
                    [title]="translationService.translate('workspace.invitation.accept')">
                    <svg lucideCheck class="w-4 h-4" />
                  </button>
                  <button (click)="rejectInvitation(inv)" [disabled]="processingIds().has(inv.id)"
                    class="p-1.5 rounded-lg bg-accent-danger-muted text-accent-danger hover:bg-accent-danger/20 transition-colors disabled:opacity-50"
                    [title]="translationService.translate('workspace.invitation.reject')">
                    <svg lucideX class="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class InvitationBadgeComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  protected translationService = inject(TranslationService);
  private destroyRef = inject(DestroyRef);

  invitations = signal<InvitationDto[]>([]);
  isOpen = signal(false);
  isLoading = signal(true);
  processingIds = signal<Set<string>>(new Set());

  pendingCount = computed(() => this.invitations().length);
  hasPending = computed(() => this.pendingCount() > 0);

  ngOnInit() {
    this.loadInvitations();
  }

  toggleDropdown() {
    this.isOpen.update(v => !v);
  }

  loadInvitations() {
    this.isLoading.set(true);
    this.workspaceService.getMyInvitations()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(invs => {
        this.invitations.set(invs);
        this.isLoading.set(false);
      });
  }

  acceptInvitation(inv: InvitationDto) {
    this.processingIds.update(set => { set.add(inv.id); return new Set(set); });
    this.workspaceService.acceptInvitation(inv.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.invitations.update(list => list.filter(i => i.id !== inv.id));
          this.processingIds.update(set => { set.delete(inv.id); return new Set(set); });
        },
        error: () => this.processingIds.update(set => { set.delete(inv.id); return new Set(set); }),
      });
  }

  rejectInvitation(inv: InvitationDto) {
    this.processingIds.update(set => { set.add(inv.id); return new Set(set); });
    this.workspaceService.rejectInvitation(inv.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.invitations.update(list => list.filter(i => i.id !== inv.id));
          this.processingIds.update(set => { set.delete(inv.id); return new Set(set); });
        },
        error: () => this.processingIds.update(set => { set.delete(inv.id); return new Set(set); }),
      });
  }
}
