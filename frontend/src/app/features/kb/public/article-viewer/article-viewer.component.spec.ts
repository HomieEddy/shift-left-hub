import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { provideMarkdown } from 'ngx-markdown';
import { PublicArticleService } from '../../services/public-article.service';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { ArticleViewerComponent } from './article-viewer.component';

describe('ArticleViewerComponent', () => {
  let component: ArticleViewerComponent;
  let fixture: ComponentFixture<ArticleViewerComponent>;
  let publicArticleService: { getArticleById: ReturnType<typeof vi.fn> };
  let translationService: { translate: ReturnType<typeof vi.fn>; currentLang: ReturnType<typeof vi.fn> };

  const mockArticle = {
    id: '1',
    titleEn: 'Setup VPN',
    contentEn: 'VPN setup instructions',
    titleFr: 'Configuration VPN',
    contentFr: 'Instructions de configuration VPN',
    slug: 'setup-vpn',
    excerpt: 'How to setup VPN',
    excerptFr: 'Comment configurer VPN',
    featuredImage: null,
    categoryId: null,
    status: 'PUBLISHED' as const,
    viewCount: 42,
    publishedAt: '2026-01-15T00:00:00Z',
    authorId: 'a1',
    authorName: 'Admin',
    lastEditorId: null,
    lastEditorName: null,
    tags: [],
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  };

  beforeEach(async () => {
    publicArticleService = {
      getArticleById: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => {
        const map: Record<string, string> = {
          'kb.invalid-id': 'Invalid article ID',
          'kb.not-found': 'Article not found',
          'common.english': 'English',
          'common.french': 'French',
        };
        return map[key] ?? key;
      }),
      currentLang: vi.fn().mockReturnValue('en'),
    };

    const paramMapSubject = new BehaviorSubject(new Map([['id', '1']]));
    await TestBed.configureTestingModule({
      imports: [ArticleViewerComponent],
      providers: [
        provideMarkdown(),
        { provide: PublicArticleService, useValue: publicArticleService },
        { provide: TranslationService, useValue: translationService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: (k: string) => paramMapSubject.value.get(k) ?? null } },
            paramMap: paramMapSubject.asObservable(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ArticleViewerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    publicArticleService.getArticleById.mockReturnValue(of(mockArticle));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load article on init', () => {
    publicArticleService.getArticleById.mockReturnValue(of(mockArticle));
    fixture.detectChanges();

    expect(publicArticleService.getArticleById).toHaveBeenCalledWith('1');
    expect(component.article()?.titleEn).toBe('Setup VPN');
    expect(component.isLoading()).toBe(false);
  });

  it('should show loading and loaded states', () => {
    const pendingSubject = new Subject<unknown>();
    publicArticleService.getArticleById.mockReturnValue(pendingSubject.asObservable());

    fixture.detectChanges();

    expect(component.isLoading()).toBe(true);

    pendingSubject.next(mockArticle);
    pendingSubject.complete();

    expect(component.isLoading()).toBe(false);
    expect(component.article()).toEqual(mockArticle);
  });

  it('should handle article load error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    publicArticleService.getArticleById.mockReturnValue(errorSubject.asObservable());

    fixture.detectChanges();
    errorSubject.error(new Error('Network error'));

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Article not found');
    expect(component.article()).toBeNull();
  });

  it('should handle article not found', () => {
    const errorSubject = new Subject<unknown>();
    publicArticleService.getArticleById.mockReturnValue(errorSubject.asObservable());

    fixture.detectChanges();
    errorSubject.error(new Error('404'));

    expect(component.errorMessage()).toBe('Article not found');
  });

  it('should translate labels via TranslationService', () => {
    translationService.translate = vi.fn((key: string) => {
      const map: Record<string, string> = {
        'kb.invalid-id': 'ID invalide',
        'kb.not-found': 'Article introuvable',
        'common.english': 'English',
        'common.french': 'Français',
      };
      return map[key] ?? key;
    });
    publicArticleService.getArticleById.mockReturnValue(of(mockArticle));
    fixture.detectChanges();

    expect(component.fallbackLanguage()).toBe('Français');
  });

  it('should display article metadata', () => {
    publicArticleService.getArticleById.mockReturnValue(of(mockArticle));
    fixture.detectChanges();

    expect(component.article()?.authorName).toBe('Admin');
    expect(component.article()?.publishedAt).toBe('2026-01-15T00:00:00Z');
    expect(component.article()?.viewCount).toBe(42);
    expect(component.displayContent()).toBe('VPN setup instructions');
  });
});
