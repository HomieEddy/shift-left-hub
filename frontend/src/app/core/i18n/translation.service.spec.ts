import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let service: TranslationService;
  const STORAGE_KEY = 'shiftleft_language';

  // Save original navigator.language descriptor to restore in afterEach
  const originalLanguageDescriptor = Object.getOwnPropertyDescriptor(
    Object.getPrototypeOf(navigator),
    'language',
  );

  beforeEach(() => {
    localStorage.clear();
    // Default browser language is English
    Object.defineProperty(navigator, 'language', {
      value: 'en-US',
      configurable: true,
    });
  });

  afterEach(() => {
    localStorage.clear();
    // Restore the original navigator.language to prevent test pollution
    if (originalLanguageDescriptor) {
      Object.defineProperty(navigator, 'language', originalLanguageDescriptor);
    }
  });

  describe('initialization', () => {
    it('should default to English when no stored preference and browser is English', () => {
      service = TestBed.inject(TranslationService);
      expect(service.currentLang()).toBe('en');
    });

    it('should default to French when browser language starts with fr', () => {
      Object.defineProperty(navigator, 'language', {
        value: 'fr-FR',
        configurable: true,
      });
      service = TestBed.inject(TranslationService);
      expect(service.currentLang()).toBe('fr');
    });

    it('should read stored language preference from localStorage', () => {
      localStorage.setItem(STORAGE_KEY, 'fr');
      service = TestBed.inject(TranslationService);
      expect(service.currentLang()).toBe('fr');
    });

    it('should save language to localStorage on init from browser detection', () => {
      service = TestBed.inject(TranslationService);
      const stored = localStorage.getItem(STORAGE_KEY);
      expect(stored).toBe('en');
    });

    it('should ignore invalid stored language and fall back to browser default', () => {
      localStorage.setItem(STORAGE_KEY, 'de');
      service = TestBed.inject(TranslationService);
      expect(service.currentLang()).toBe('en');
    });
  });

  describe('switchLanguage', () => {
    it('should switch to French and persist to localStorage', () => {
      service = TestBed.inject(TranslationService);
      expect(service.currentLang()).toBe('en');

      service.switchLanguage('fr');
      expect(service.currentLang()).toBe('fr');
      expect(localStorage.getItem(STORAGE_KEY)).toBe('fr');
    });

    it('should switch back to English and persist', () => {
      localStorage.setItem(STORAGE_KEY, 'fr');
      service = TestBed.inject(TranslationService);
      expect(service.currentLang()).toBe('fr');

      service.switchLanguage('en');
      expect(service.currentLang()).toBe('en');
      expect(localStorage.getItem(STORAGE_KEY)).toBe('en');
    });

    it('should toggle between languages multiple times', () => {
      service = TestBed.inject(TranslationService);

      service.switchLanguage('fr');
      expect(service.currentLang()).toBe('fr');

      service.switchLanguage('en');
      expect(service.currentLang()).toBe('en');

      service.switchLanguage('fr');
      expect(service.currentLang()).toBe('fr');
    });
  });

  describe('signal reactivity', () => {
    it('should update signal value immediately on switchLanguage', () => {
      service = TestBed.inject(TranslationService);

      service.switchLanguage('fr');
      expect(service.currentLang()).toBe('fr');

      service.switchLanguage('en');
      expect(service.currentLang()).toBe('en');
    });

    it('should not mutate the signal without calling switchLanguage', () => {
      service = TestBed.inject(TranslationService);
      expect(service.currentLang()).toBe('en');

      // Direct localStorage manipulation without service call
      localStorage.setItem(STORAGE_KEY, 'fr');
      // Signal should still be 'en' since we didn't call switchLanguage
      expect(service.currentLang()).toBe('en');
    });
  });
});
