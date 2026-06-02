import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf, NgFor } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ArticleService } from '../../services/article.service';
import { TagService } from '../../services/tag.service';
import { TagDto, CreateArticleRequest, UpdateArticleRequest } from '../../models/article.models';
import { MarkdownModule } from 'ngx-markdown';
import { TranslationService } from '../../../../core/i18n/translation.service';

@Component({
  selector: 'app-article-editor',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor, MarkdownModule],
  templateUrl: './article-editor.component.html',
})
export class ArticleEditorComponent implements OnInit {
  private articleService = inject(ArticleService);
  private tagService = inject(TagService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  protected translationService = inject(TranslationService);

  isEdit = signal(false);
  articleId = signal<string | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  errorMessage = signal('');

  activeTab = signal<'en' | 'fr'>('en');

  titleEn = '';
  contentEn = '';
  titleFr = '';
  contentFr = '';
  excerpt = '';
  featuredImage = '';

  allTags = signal<TagDto[]>([]);
  selectedTagIds = signal<Set<string>>(new Set());

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      this.articleId.set(id);
      this.loadArticle(id);
    }
    this.loadTags();
  }

  loadArticle(id: string): void {
    this.isLoading.set(true);
    this.articleService.getArticleById(id).subscribe({
      next: (article) => {
        this.titleEn = article.titleEn;
        this.contentEn = article.contentEn;
        this.titleFr = article.titleFr ?? '';
        this.contentFr = article.contentFr ?? '';
        this.excerpt = article.excerpt ?? '';
        this.featuredImage = article.featuredImage ?? '';
        this.selectedTagIds.set(new Set(article.tags.map(t => t.id)));
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load article.');
        this.isLoading.set(false);
      },
    });
  }

  loadTags(): void {
    this.tagService.getTags().subscribe({
      next: (tags) => this.allTags.set(tags),
    });
  }

  toggleTag(tagId: string): void {
    this.selectedTagIds.update(ids => {
      const newIds = new Set(ids);
      if (newIds.has(tagId)) {
        newIds.delete(tagId);
      } else {
        newIds.add(tagId);
      }
      return newIds;
    });
  }

  save(): void {
    this.isSaving.set(true);
    this.errorMessage.set('');

    const request: CreateArticleRequest | UpdateArticleRequest = {
      titleEn: this.titleEn,
      contentEn: this.contentEn,
      titleFr: this.titleFr || undefined,
      contentFr: this.contentFr || undefined,
      excerpt: this.excerpt || undefined,
      featuredImage: this.featuredImage || undefined,
      tagIds: Array.from(this.selectedTagIds()),
    };

    const action = this.isEdit() && this.articleId()
      ? this.articleService.updateArticle(this.articleId()!, request as UpdateArticleRequest)
      : this.articleService.createArticle(request as CreateArticleRequest);

    action.subscribe({
      next: (article) => {
        this.isSaving.set(false);
        this.router.navigate(['/admin/articles']);
      },
      error: () => {
        this.errorMessage.set('Failed to save article. Please try again.');
        this.isSaving.set(false);
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/articles']);
  }
}
