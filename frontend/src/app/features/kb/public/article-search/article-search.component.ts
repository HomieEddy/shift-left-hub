import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { PublicArticleService } from '../../services/public-article.service';
import { ArticleSearchResult, ArticleSearchTag } from '../../models/article.models';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-article-search',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './article-search.component.html',
})
export class ArticleSearchComponent implements OnInit {
  private http = inject(HttpClient);
  private publicArticleService = inject(PublicArticleService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);
  private sanitizer = inject(DomSanitizer);

  query = signal('');
  availableTags = signal<ArticleSearchTag[]>([]);
  selectedTags = signal<string[]>([]);
  results = signal<ArticleSearchResult[]>([]);
  isLoading = signal(false);
  hasSearched = signal(false);
  errorMessage = signal('');
  totalResults = signal(0);
  currentPage = signal(0);
  totalPages = signal(0);
  categories = signal<{ id: string; nameEn: string; nameFr: string }[]>([]);
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.loadSearchTags();
    this.loadCategories();

    this.destroyRef.onDestroy(() => {
      if (this.debounceTimer != null) {
        clearTimeout(this.debounceTimer);
        this.debounceTimer = null;
      }
    });

    this.route.queryParams.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(params => {
      const q = params['q'] as string | undefined;
      const tagsParam = params['tags'] as string | undefined;
      const tags = tagsParam != null && tagsParam.length > 0
        ? tagsParam.split(',').map(t => t.trim()).filter(Boolean)
        : [];

      this.selectedTags.set(tags);

      if (q != null && q !== '') {
        this.query.set(q);
        this.doSearch(q, 0, tags);
      } else {
        this.query.set('');
        this.results.set([]);
        this.hasSearched.set(false);
        this.totalResults.set(0);
        this.totalPages.set(0);
        this.currentPage.set(0);
      }
    });
  }

  loadCategories(): void {
    this.http.get<{ id: string; nameEn: string; nameFr: string }[]>('/api/admin/categories').pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => { /* categories are non-critical, silently ignore */ },
    });
  }

  loadSearchTags(): void {
    this.publicArticleService.getSearchTags().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (tags) => this.availableTags.set(tags),
      error: () => this.availableTags.set([]),
    });
  }

  onSearchInput(value: string): void {
    this.query.set(value);
    if (this.debounceTimer != null) {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = null;
    }
    this.debounceTimer = setTimeout(() => {
      if (value.trim()) {
        void this.router.navigate([], {
          queryParams: {
            q: value.trim(),
            tags: this.selectedTags().length ? this.selectedTags().join(',') : null,
          },
        });
      } else {
        void this.router.navigate([], {
          queryParams: {
            q: null,
            tags: null,
          },
        });
      }
    }, 300);
  }

  doSearch(query: string, page = 0, tags: string[] = this.selectedTags()): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.currentPage.set(page);
    this.publicArticleService.search(query, page, 20, tags).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (pageData) => {
        this.results.set(pageData.content);
        this.totalResults.set(pageData.totalElements);
        this.totalPages.set(pageData.totalPages);
        this.hasSearched.set(true);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.hasSearched.set(false);
        this.errorMessage.set('Search failed. Please try again.');
      },
    });
  }

  toggleTag(tagName: string): void {
    this.selectedTags.update(current => {
      if (current.includes(tagName)) {
        return current.filter(t => t !== tagName);
      }
      return [...current, tagName];
    });

    if (this.query().trim().length === 0) {
      return;
    }

    void this.router.navigate([], {
      queryParams: {
        q: this.query().trim(),
        tags: this.selectedTags().length ? this.selectedTags().join(',') : null,
      },
    });
  }

  isTagSelected(tagName: string): boolean {
    return this.selectedTags().includes(tagName);
  }

  changePage(page: number): void {
    this.doSearch(this.query(), page, this.selectedTags());
  }

  sanitizeHeadline(html: string): SafeHtml {
    if (html === '') return '';
    // Keep only mark tags and remove any attributes from opening mark tags.
    const cleaned = html
      .replace(/<(?!\/?mark(?=>|\s[^>]*>))[^>]*>/gi, '')
      .replace(/<mark\b[^>]*>/gi, '<mark>');
    const sanitized = this.sanitizer.sanitize(1 /* SecurityContext.HTML */, cleaned);
    return this.sanitizer.bypassSecurityTrustHtml(sanitized ?? cleaned);
  }
}
