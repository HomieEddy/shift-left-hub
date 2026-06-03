import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PublicArticleService } from '../../services/public-article.service';
import { ArticleDto } from '../../models/article.models';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { MarkdownModule } from 'ngx-markdown';

@Component({
  selector: 'app-article-viewer',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, RouterLink, MarkdownModule],
  templateUrl: './article-viewer.component.html',
  styleUrls: ['./article-viewer.component.css'],
})
export class ArticleViewerComponent implements OnInit {
  private publicArticleService = inject(PublicArticleService);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  article = signal<ArticleDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadArticle(id);
    } else {
      this.errorMessage.set('Invalid article ID.');
      this.isLoading.set(false);
    }
  }

  loadArticle(id: string): void {
    this.isLoading.set(true);
    this.publicArticleService.getArticleById(id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (article) => {
        this.article.set(article);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Article not found.');
        this.isLoading.set(false);
      },
    });
  }

  get displayContent(): string {
    const a = this.article();
    if (!a) return '';
    if (this.translationService.currentLang() === 'fr') {
      return a.contentFr || a.contentEn;
    }
    return a.contentEn;
  }

  get isFallback(): boolean {
    const a = this.article();
    if (!a) return false;
    if (this.translationService.currentLang() === 'fr') {
      return !a.contentFr;
    }
    return false;
  }

  get fallbackLanguage(): string {
    return this.translationService.currentLang() === 'fr' ? 'English' : 'Français';
  }
}
