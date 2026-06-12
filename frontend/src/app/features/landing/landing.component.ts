import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TranslationService } from '../../core/i18n/translation.service';
import {
  LucideMessageSquare,
  LucideBookOpen,
  LucideTicket,
  LucideSearch,
  LucideZap,
  LucideFileText,
  LucideSparkles,
  LucideUsers,
  LucideArrowRight,
  LucideLayoutDashboard,
  LucideSettings,
  LucideClipboardList,
  LucideTag,
  LucideShield,
  LucideCheckCircle,
    LucideHelpCircle,
  LucideInbox,
  LucideFolderTree,
} from '@lucide/angular';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    RouterLink,
    LucideMessageSquare,
    LucideBookOpen,
    LucideTicket,
    LucideSearch,
    LucideZap,
    LucideFileText,
    LucideSparkles,
    LucideUsers,
    LucideArrowRight,
    LucideLayoutDashboard,
    LucideSettings,
    LucideClipboardList,
    LucideTag,
    LucideShield,
    LucideCheckCircle,
        LucideHelpCircle,
    LucideInbox,
    LucideFolderTree,
  ],
  templateUrl: './landing.component.html',
})
export class LandingComponent {
  protected authService = inject(AuthService);
  protected translationService = inject(TranslationService);

  protected get currentYear(): number {
    return new Date().getFullYear();
  }

  protected get firstLetter(): string {
    const name = this.authService.user()?.displayName ?? '';
    return name.length > 0 ? name.charAt(0).toUpperCase() : '?';
  }
}
