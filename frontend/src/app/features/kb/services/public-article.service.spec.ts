import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { PublicArticleService } from './public-article.service';
import { ArticleDto, ArticleSearchResult } from '../models/article.models';

describe('PublicArticleService', () => {
  let service: PublicArticleService;
  let httpMock: HttpTestingController;

  const mockArticle: ArticleDto = {
    id: 'article-1',
    titleEn: 'How to reset your password',
    contentEn: 'Follow these steps...',
    titleFr: 'Comment réinitialiser votre mot de passe',
    contentFr: 'Suivez ces étapes...',
    slug: 'how-to-reset',
    excerpt: 'Reset guide',
    excerptFr: 'Guide de réinitialisation',
    featuredImage: null,
    categoryId: null,
    status: 'PUBLISHED',
    viewCount: 10,
    publishedAt: '2024-06-01T10:00:00Z',
    authorId: 'author-1',
    authorName: 'Admin',
    lastEditorId: null,
    lastEditorName: null,
    tags: [],
    createdAt: '2024-06-01T09:00:00Z',
    updatedAt: '2024-06-01T10:00:00Z',
  };

  const mockSearchResult: ArticleSearchResult = {
    id: 'article-1',
    title: 'How to reset your password',
    headline: 'Reset guide',
    slug: 'how-to-reset',
    excerpt: 'Reset guide',
    publishedAt: '2024-06-01T10:00:00Z',
    tagNames: ['password'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PublicArticleService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(PublicArticleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should GET /api/articles with page and size params', () => {
    service.getArticles(0, 20).subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === '/api/articles'
        && r.params.get('page') === '0'
        && r.params.get('size') === '20');
    req.flush({ content: [mockArticle], totalPages: 1, totalElements: 1, number: 0, size: 20 });
  });

  it('should GET /api/articles/search with q, page, size, and tags', () => {
    service.search('reset', 0, 20, ['password', 'howto']).subscribe((r) => {
      expect(r.content[0].id).toBe('article-1');
    });
    const req = httpMock.expectOne(
      (r) => r.url === '/api/articles/search'
        && r.params.get('q') === 'reset'
        && r.params.get('page') === '0'
        && r.params.get('size') === '20'
        && (r.params.getAll('tags') ?? []).join(',') === 'password,howto');
    req.flush({ content: [mockSearchResult], totalPages: 1, totalElements: 1, number: 0, size: 20 });
  });

  it('should GET /api/articles/search/tags', () => {
    service.getSearchTags().subscribe((tags) => {
      expect(tags.length).toBe(2);
    });
    const req = httpMock.expectOne('/api/articles/search/tags');
    expect(req.request.method).toBe('GET');
    req.flush([
      { nameEn: 'VPN', nameFr: 'VPN', color: '#000', articleCount: 3 },
      { nameEn: 'Email', nameFr: 'Courriel', color: '#000', articleCount: 1 },
    ]);
  });

  it('should GET /api/articles/:id on getArticleById', () => {
    service.getArticleById('article-1').subscribe((a) => {
      expect(a.id).toBe('article-1');
    });
    const req = httpMock.expectOne('/api/articles/article-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockArticle);
  });
});
