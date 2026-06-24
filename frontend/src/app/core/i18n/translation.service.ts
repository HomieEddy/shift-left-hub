import { Injectable, signal } from '@angular/core';
import { TRANSLATIONS_EN } from './translations.en';
import type { TRANSLATIONS_FR } from './translations.fr';

export type SupportedLanguage = 'en' | 'fr';

type TranslationMap = Record<string, string>;

@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly STORAGE_KEY = 'shiftleft_language';
  readonly currentLang = signal<SupportedLanguage>('en');

  private translations: TranslationMap = { ...TRANSLATIONS_EN };
  private frenchLoaded = false;

  constructor() {
    this.initLanguage();
  }

  private initLanguage(): void {
    const stored = localStorage.getItem(this.STORAGE_KEY) as SupportedLanguage | null;
    if (stored && ['en', 'fr'].includes(stored)) {
      this.currentLang.set(stored);
      if (stored === 'fr') {
        void this.loadFrenchTranslations();
      }
      return;
    }

    const browserLang = navigator.language.startsWith('fr') ? 'fr' : 'en';
    this.currentLang.set(browserLang);
    if (browserLang === 'fr') {
      void this.loadFrenchTranslations();
    }
    localStorage.setItem(this.STORAGE_KEY, browserLang);
  }

  async switchLanguage(lang: SupportedLanguage): Promise<void> {
    this.currentLang.set(lang);
    localStorage.setItem(this.STORAGE_KEY, lang);
    if (lang === 'fr') {
      await this.loadFrenchTranslations();
    }
  }

  private async loadFrenchTranslations(): Promise<void> {
    if (this.frenchLoaded) {
      return;
    }
    const mod: { TRANSLATIONS_FR: typeof TRANSLATIONS_FR } = await import('./translations.fr');
    this.translations = { ...this.translations, ...mod.TRANSLATIONS_FR };
    this.frenchLoaded = true;
  }

  translate(key: string, params?: Record<string, string | number>): string {
    const value = this.translations[key] ?? key;
    if (params) {
      let result = value;
      for (const [k, v] of Object.entries(params)) {
        result = result.replaceAll(`{${k}}`, String(v));
      }
      return result;
    }
    return value;
  }
}
