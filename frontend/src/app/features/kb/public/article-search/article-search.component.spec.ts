import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { PublicArticleService } from '../../services/public-article.service';
import { ArticleSearchComponent } from './article-search.component';

describe('ArticleSearchComponent', () => {
  let component: ArticleSearchComponent;
  let fixture: ComponentFixture<ArticleSearchComponent>;
  let publicArticleService: {
    search: ReturnType<typeof vi.fn>;
    getSearchTags: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let queryParamsSubject: Subject<Record<string, string>>;

  const mockResults = {
    content: [
      {
        id: '1',
        title: 'Test Article',
        headline: '<mark>Test</mark> result',
        slug: 'test-article',
        excerpt: 'An article',
        publishedAt: '2026-01-01T00:00:00Z',
        tagNames: ['tag1'],
      },
      {
        id: '2',
        title: 'Second Article',
        headline: 'No match',
        slug: 'second-article',
        excerpt: 'Another article',
        publishedAt: '2026-01-02T00:00:00Z',
        tagNames: ['tag2'],
      },
    ],
    totalPages: 1,
    totalElements: 2,
    number: 0,
    size: 20,
  };

  beforeEach(async () => {
    queryParamsSubject = new Subject<Record<string, string>>();
    publicArticleService = {
      search: vi.fn(),
      getSearchTags: vi.fn().mockReturnValue(of([])),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [ArticleSearchComponent],
      providers: [
        { provide: PublicArticleService, useValue: publicArticleService },
        { provide: ActivatedRoute, useValue: { queryParams: queryParamsSubject.asObservable() } },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ArticleSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load search tags on init', () => {
    expect(publicArticleService.getSearchTags).toHaveBeenCalled();
  });

  it('should call doSearch when query param is present on init', () => {
    publicArticleService.search.mockReturnValue(of(mockResults));
    const doSearchSpy = vi.spyOn(component, 'doSearch');

    queryParamsSubject.next({ q: 'test' });

    expect(doSearchSpy).toHaveBeenCalledWith('test', 0, []);
  });

  it('should set results from search service response', () => {
    publicArticleService.search.mockReturnValue(of(mockResults));

    component.doSearch('test', 0, []);

    expect(publicArticleService.search).toHaveBeenCalledWith('test', 0, 20, []);
    expect(component.results().length).toBe(2);
    expect(component.hasSearched()).toBe(true);
    expect(component.isLoading()).toBe(false);
  });

  it('should show empty state when no results', () => {
    publicArticleService.search.mockReturnValue(
      of({
        content: [],
        totalPages: 0,
        totalElements: 0,
        number: 0,
        size: 20,
      }),
    );

    component.doSearch('nonexistent', 0, []);

    expect(component.results().length).toBe(0);
    expect(component.hasSearched()).toBe(true);
  });

  it('should set error state on search failure', () => {
    const errorSubject = new Subject<unknown>();
    publicArticleService.search.mockReturnValue(errorSubject.asObservable());

    component.doSearch('test', 0, []);
    errorSubject.error(new Error('API error'));

    expect(component.isLoading()).toBe(false);
    expect(component.hasSearched()).toBe(false);
    expect(component.errorMessage()).toBe('Search failed. Please try again.');
  });

  it('should set loading state during search', () => {
    const pendingSubject = new Subject<unknown>();
    publicArticleService.search.mockReturnValue(pendingSubject.asObservable());

    component.doSearch('test', 0, []);

    expect(component.isLoading()).toBe(true);
  });

  it('should set query signal on search input', () => {
    component.onSearchInput('test query');
    expect(component.query()).toBe('test query');
  });
});
