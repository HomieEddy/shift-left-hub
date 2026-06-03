import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgFor, NgIf, DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PublicArticleService } from '../../services/public-article.service';
import { ArticleSearchResult, ArticleSearchTag } from '../../models/article.models';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-article-search',
  standalone: true,
  imports: [NgFor, NgIf, RouterLink, FormsModule, DatePipe],
  templateUrl: './article-search.component.html',
})
export class ArticleSearchComponent implements OnInit {
  private publicArticleService = inject(PublicArticleService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

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
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.loadSearchTags();

    this.route.queryParams.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(params => {
      const q = params['q'];
      const tagsParam = params['tags'] as string | undefined;
      const tags = tagsParam
        ? tagsParam.split(',').map(t => t.trim()).filter(Boolean)
        : [];

      this.selectedTags.set(tags);

      if (q) {
        this.query.set(q);
        this.doSearch(q, 0, tags);
      }
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
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    this.debounceTimer = setTimeout(() => {
      if (value.trim()) {
        this.router.navigate([], {
          queryParams: {
            q: value.trim(),
            tags: this.selectedTags().length ? this.selectedTags().join(',') : null,
          },
        });
        this.doSearch(value.trim(), 0, this.selectedTags());
      } else {
        this.results.set([]);
        this.hasSearched.set(false);
      }
    }, 300);
  }

  doSearch(query: string, page: number = 0, tags: string[] = this.selectedTags()): void {
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

    if (!this.query().trim()) {
      return;
    }

    this.router.navigate([], {
      queryParams: {
        q: this.query().trim(),
        tags: this.selectedTags().length ? this.selectedTags().join(',') : null,
      },
    });

    this.doSearch(this.query().trim(), 0, this.selectedTags());
  }

  isTagSelected(tagName: string): boolean {
    return this.selectedTags().includes(tagName);
  }

  changePage(page: number): void {
    this.doSearch(this.query(), page, this.selectedTags());
  }

  sanitizeHeadline(html: string): string {
    if (!html) return '';
    // Only allow <mark> tags from ts_headline, strip everything else
    return html.replace(/<(?!\/?mark(?=>|\s.*>))\/?.*?>/gi, '');
  }
}
