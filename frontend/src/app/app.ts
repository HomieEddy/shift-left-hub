import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { TranslationService, SupportedLanguage } from './core/i18n/translation.service';
import { KcsDraftService } from './features/admin/kcs-draft.service';
import { interval, startWith, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { 
  LucideMenu, 
  LucideLogOut, 
  LucideBookOpen, 
  LucideMessageSquare, 
  LucideTicket, 
  LucideLayoutList, 
  LucideUsers, 
  LucideFileText, 
  LucideClipboardList, 
  LucideTag, 
  LucideSettings, 
  LucideLayoutDashboard, 
  LucideX 
} from '@lucide/angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, 
    RouterLink, 
    RouterLinkActive, 
    LucideMenu, 
    LucideLogOut, 
    LucideBookOpen, 
    LucideMessageSquare, 
    LucideTicket, 
    LucideLayoutList, 
    LucideUsers, 
    LucideFileText, 
    LucideClipboardList, 
    LucideTag, 
    LucideSettings, 
    LucideLayoutDashboard, 
    LucideX
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected authService = inject(AuthService);
  protected translationService = inject(TranslationService);
  private router = inject(Router);
  private kcsDraftService = inject(KcsDraftService);
  private destroyRef = inject(DestroyRef);

  pendingKcsCount = signal(0);
  isMobileMenuOpen = signal(false);

  constructor() {
    // Poll pending KCS draft count every 60 seconds for nav badge
    interval(60000).pipe(
      startWith(0),
      switchMap(() => this.kcsDraftService.getPendingCount()),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => this.pendingKcsCount.set(response.pendingCount),
      error: (err) => console.warn('KCS pending-count poll failed:', err),
    });
  }

  switchLanguage(lang: SupportedLanguage): void {
    this.translationService.switchLanguage(lang);
  }

  logout(): void {
    this.authService.logout().subscribe(() => { void this.router.navigate(['/']); });
  }
}
