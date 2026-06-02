import { Component, inject, OnInit, signal } from '@angular/core';
import { NgFor, NgIf, DatePipe } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PublicArticleService } from '../../services/public-article.service';
import { ArticleSearchResult } from '../../models/article.models';
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
  protected translationService = inject(TranslationService);

  query = signal('');
  results = signal<ArticleSearchResult[]>([]);
  isLoading = signal(false);
  hasSearched = signal(false);
  totalResults = signal(0);
  currentPage = signal(0);
  totalPages = signal(0);
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const q = params['q'];
      if (q) {
        this.query.set(q);
        this.doSearch(q);
      }
    });
  }

  onSearchInput(value: string): void {
    this.query.set(value);
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    this.debounceTimer = setTimeout(() => {
      if (value.trim()) {
        this.router.navigate([], {
          queryParams: { q: value.trim() },
          queryParamsHandling: 'merge',
        });
        this.doSearch(value.trim());
      } else {
        this.results.set([]);
        this.hasSearched.set(false);
      }
    }, 300);
  }

  doSearch(query: string, page: number = 0): void {
    this.isLoading.set(true);
    this.currentPage.set(page);
    this.publicArticleService.search(query, page).subscribe({
      next: (pageData) => {
        this.results.set(pageData.content);
        this.totalResults.set(pageData.totalElements);
        this.totalPages.set(pageData.totalPages);
        this.hasSearched.set(true);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.hasSearched.set(true);
      },
    });
  }

  changePage(page: number): void {
    this.doSearch(this.query(), page);
  }

  sanitizeHeadline(html: string): string {
    if (!html) return '';
    // Only allow <mark> tags from ts_headline, strip everything else
    return html.replace(/<(?!\/?mark(?=>|\s.*>))\/?.*?>/gi, '');
  }
}
