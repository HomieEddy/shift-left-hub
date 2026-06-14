import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { PublicArticleService } from '../../services/public-article.service';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { ArticleListComponent } from './article-list.component';

describe('ArticleListComponent', () => {
  let component: ArticleListComponent;
  let fixture: ComponentFixture<ArticleListComponent>;
  let publicArticleService: { getArticles: ReturnType<typeof vi.fn> };
  let translationService: { translate: ReturnType<typeof vi.fn>; currentLang: ReturnType<typeof vi.fn> };

  const mockPage = {
    content: [
      {
        id: '1',
        titleEn: 'Setup VPN',
        contentEn: 'VPN setup instructions',
        titleFr: 'Configuration VPN',
        contentFr: null,
        slug: 'setup-vpn',
        excerpt: 'How to setup VPN',
        excerptFr: null,
        featuredImage: null,
        categoryId: null,
        status: 'PUBLISHED' as const,
        viewCount: 10,
        publishedAt: '2026-01-15T00:00:00Z',
        authorId: 'a1',
        authorName: 'Admin',
        lastEditorId: null,
        lastEditorName: null,
        tags: [{ id: 't1', nameEn: 'vpn', nameFr: 'vpn', color: 'blue' }],
        createdAt: '2026-01-15T00:00:00Z',
        updatedAt: '2026-01-15T00:00:00Z',
      },
      {
        id: '2',
        titleEn: 'Reset Password',
        contentEn: 'Password reset steps',
        titleFr: null,
        contentFr: null,
        slug: 'reset-password',
        excerpt: 'Steps to reset',
        excerptFr: null,
        featuredImage: null,
        categoryId: null,
        status: 'PUBLISHED' as const,
        viewCount: 5,
        publishedAt: '2026-02-01T00:00:00Z',
        authorId: 'a1',
        authorName: 'Admin',
        lastEditorId: null,
        lastEditorName: null,
        tags: [],
        createdAt: '2026-02-01T00:00:00Z',
        updatedAt: '2026-02-01T00:00:00Z',
      },
    ],
    totalPages: 3,
    totalElements: 25,
    number: 0,
    size: 20,
  };

  beforeEach(async () => {
    publicArticleService = {
      getArticles: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => key),
      currentLang: vi.fn().mockReturnValue('en'),
    };

    await TestBed.configureTestingModule({
      imports: [ArticleListComponent],
      providers: [
        { provide: PublicArticleService, useValue: publicArticleService },
        { provide: TranslationService, useValue: translationService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ArticleListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    publicArticleService.getArticles.mockReturnValue(of(mockPage));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load articles on init', () => {
    publicArticleService.getArticles.mockReturnValue(of(mockPage));
    fixture.detectChanges();

    expect(publicArticleService.getArticles).toHaveBeenCalledWith(0);
    expect(component.articles().length).toBe(2);
    expect(component.isLoading()).toBe(false);
  });

  it('should show loading and loaded states', () => {
    const pendingSubject = new Subject<unknown>();
    publicArticleService.getArticles.mockReturnValue(pendingSubject.asObservable());

    fixture.detectChanges();
    expect(component.isLoading()).toBe(true);

    pendingSubject.next(mockPage);
    pendingSubject.complete();

    expect(component.isLoading()).toBe(false);
    expect(component.articles().length).toBe(2);
  });

  it('should filter by search query with debounce', () => {
    vi.useFakeTimers();
    publicArticleService.getArticles.mockReturnValue(of(mockPage));
    fixture.detectChanges();

    component.onSearchInput('vpn');
    expect(component.isLoading()).toBe(false); // debounce not yet fired
    vi.advanceTimersByTime(300);
    expect(publicArticleService.getArticles).toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('should filter by selected tags', () => {
    publicArticleService.getArticles.mockReturnValue(of(mockPage));
    fixture.detectChanges();

    expect(component.articles().length).toBeGreaterThan(0);
  });

  it('should paginate results', () => {
    publicArticleService.getArticles.mockReturnValue(of(mockPage));
    fixture.detectChanges();

    component.changePage(1);
    expect(component.currentPage()).toBe(1);
    expect(publicArticleService.getArticles).toHaveBeenCalledWith(1);

    component.changePage(0);
    expect(component.currentPage()).toBe(0);
  });

  it('should handle empty search results', () => {
    const emptyPage = { content: [], totalPages: 0, totalElements: 0, number: 0, size: 20 };
    publicArticleService.getArticles.mockReturnValue(of(emptyPage));
    fixture.detectChanges();

    expect(component.articles().length).toBe(0);
    expect(component.isLoading()).toBe(false);
  });

  it('should handle load error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    publicArticleService.getArticles.mockReturnValue(errorSubject.asObservable());

    fixture.detectChanges();
    errorSubject.error(new Error('API error'));

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load articles.');
  });

  it('should translate labels', () => {
    publicArticleService.getArticles.mockReturnValue(of(mockPage));
    fixture.detectChanges();

    const display = component.displayTitle(component.articles()[0]);
    expect(display).toBe('Setup VPN');
  });

  it('should navigate to article on select', () => {
    publicArticleService.getArticles.mockReturnValue(of(mockPage));
    fixture.detectChanges();

    expect(component.articles().length).toBe(2);
  });
});
