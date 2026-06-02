import { Component, inject, OnInit, signal } from '@angular/core';
import { NgIf, NgFor, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PublicArticleService } from '../../services/public-article.service';
import { ArticleDto } from '../../models/article.models';
import { TranslationService } from '../../../../core/i18n/translation.service';
import { MarkdownModule } from 'ngx-markdown';

@Component({
  selector: 'app-article-viewer',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, RouterLink, MarkdownModule],
  templateUrl: './article-viewer.component.html',
})
export class ArticleViewerComponent implements OnInit {
  private publicArticleService = inject(PublicArticleService);
  private route = inject(ActivatedRoute);
  protected translationService = inject(TranslationService);

  article = signal<ArticleDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadArticle(id);
    }
  }

  loadArticle(id: string): void {
    this.isLoading.set(true);
    this.publicArticleService.getArticleById(id).subscribe({
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

  get displayTitle(): string {
    const a = this.article();
    if (!a) return '';
    if (this.translationService.currentLang() === 'fr' && a.titleFr) {
      return a.titleFr;
    }
    return a.titleEn;
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
