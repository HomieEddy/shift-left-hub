import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { KcsDraftService } from '../kcs-draft.service';

@Component({
  selector: 'app-kcs-draft-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (pendingCount() > 0) {
      <span class="absolute top-1 right-2 flex h-2 w-2">
        <span
          class="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"
        ></span>
        <span class="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
      </span>
    }
  `,
})
export class KcsDraftBadgeComponent implements OnInit, OnDestroy {
  private kcsDraftService = inject(KcsDraftService);
  private destroyRef = inject(DestroyRef);

  pendingCount = signal(0);
  private pollHandle: number | null = null;

  ngOnInit(): void {
    this.refresh();
    this.pollHandle = window.setInterval(() => this.refresh(), 60_000);
  }

  ngOnDestroy(): void {
    if (this.pollHandle !== null) {
      window.clearInterval(this.pollHandle);
    }
  }

  private refresh(): void {
    this.kcsDraftService
      .getPendingCount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.pendingCount.set(response.pendingCount),
        error: () => this.pendingCount.set(0),
      });
  }
}
