import { Injectable, signal } from '@angular/core';
import { translations } from './translations';

export type SupportedLanguage = 'en' | 'fr';

@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly STORAGE_KEY = 'shiftleft_language';
  readonly currentLang = signal<SupportedLanguage>('en');

  constructor() {
    this.initLanguage();
  }

  private initLanguage(): void {
    const stored = localStorage.getItem(this.STORAGE_KEY) as SupportedLanguage | null;
    if (stored && ['en', 'fr'].includes(stored)) {
      this.currentLang.set(stored);
      return;
    }

    const browserLang = navigator.language.startsWith('fr') ? 'fr' : 'en';
    this.currentLang.set(browserLang);
    localStorage.setItem(this.STORAGE_KEY, browserLang);
  }

  switchLanguage(lang: SupportedLanguage): void {
    this.currentLang.set(lang);
    localStorage.setItem(this.STORAGE_KEY, lang);
  }

  translate(key: string, params?: Record<string, string | number>): string {
    const entry = translations[key];
    if (entry == null) return key;
    let result = this.currentLang() === 'fr' ? entry.fr : entry.en;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        result = result.replaceAll(`{${k}}`, String(v));
      }
    }
    return result;
  }
}
