import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from './core/auth/auth.service';
import { TranslationService, SupportedLanguage } from './core/i18n/translation.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIf],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected authService = inject(AuthService);
  protected translationService = inject(TranslationService);

  switchLanguage(lang: SupportedLanguage): void {
    this.translationService.switchLanguage(lang);
  }
}
