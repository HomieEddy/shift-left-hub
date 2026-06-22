import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ArticleService } from './article.service';

describe('ArticleService', () => {
  let service: ArticleService;
  let httpMock: HttpTestingController;

  const mockArticle = {
    id: 'article-1',
    titleEn: 'How to reset your password',
    contentEn: 'Follow these steps...',
    titleFr: 'Comment réinitialiser votre mot de passe',
    contentFr: 'Suivez ces étapes...',
    slug: 'how-to-reset-your-password',
    excerpt: 'Step-by-step password reset guide',
    featuredImage: null,
    status: 'PUBLISHED' as const,
    viewCount: 42,
    publishedAt: '2024-06-01T10:00:00Z',
    authorId: 'author-1',
    authorName: 'Admin User',
    lastEditorId: null,
    lastEditorName: null,
    tags: [],
    createdAt: '2024-06-01T09:00:00Z',
    updatedAt: '2024-06-01T10:00:00Z',
  };

  const mockPaginatedResponse = {
    content: [mockArticle, { ...mockArticle, id: 'article-2', slug: 'how-to-install-app' }],
    totalPages: 1,
    totalElements: 2,
    number: 0,
    size: 20,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ArticleService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ArticleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getArticles', () => {
    it('should GET /api/admin/articles with pagination params', () => {
      service.getArticles(0, 20).subscribe((response) => {
        expect(response.content.length).toBe(2);
        expect(response.totalElements).toBe(2);
        expect(response.number).toBe(0);
      });

      const req = httpMock.expectOne((r) => r.url === '/api/admin/articles');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPaginatedResponse);
    });

    it('should use default pagination values', () => {
      service.getArticles().subscribe();

      const req = httpMock.expectOne('/api/admin/articles?page=0&size=20');
      expect(req.request.method).toBe('GET');
      req.flush(mockPaginatedResponse);
    });

    it('should handle different page and size', () => {
      service.getArticles(2, 10).subscribe();

      const req = httpMock.expectOne('/api/admin/articles?page=2&size=10');
      expect(req.request.method).toBe('GET');
      req.flush(mockPaginatedResponse);
    });
  });

  describe('getArticleById', () => {
    it('should GET /api/admin/articles/:id', () => {
      service.getArticleById('article-1').subscribe((article) => {
        expect(article.id).toBe('article-1');
        expect(article.titleEn).toBe('How to reset your password');
      });

      const req = httpMock.expectOne('/api/admin/articles/article-1');
      expect(req.request.method).toBe('GET');
      req.flush(mockArticle);
    });
  });

  describe('createArticle', () => {
    it('should POST to /api/admin/articles', () => {
      const request = {
        titleEn: 'New Article',
        contentEn: 'Content here',
      };

      service.createArticle(request).subscribe((article) => {
        expect(article.id).toBe('article-1');
      });

      const req = httpMock.expectOne('/api/admin/articles');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockArticle);
    });
  });

  describe('updateArticle', () => {
    it('should PUT to /api/admin/articles/:id', () => {
      const request = {
        titleEn: 'Updated Title',
        contentEn: 'Updated content',
      };

      service.updateArticle('article-1', request).subscribe((article) => {
        expect(article.slug).toBe('how-to-reset-your-password');
      });

      const req = httpMock.expectOne('/api/admin/articles/article-1');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockArticle);
    });
  });

  describe('publishArticle', () => {
    it('should PUT to /api/admin/articles/:id/publish', () => {
      service.publishArticle('article-1').subscribe((article) => {
        expect(article.status).toBe('PUBLISHED');
      });

      const req = httpMock.expectOne('/api/admin/articles/article-1/publish');
      expect(req.request.method).toBe('PUT');
      req.flush(mockArticle);
    });
  });

  describe('archiveArticle', () => {
    it('should PUT to /api/admin/articles/:id/archive', () => {
      service.archiveArticle('article-1').subscribe((article) => {
        expect(article.status).toBe('PUBLISHED');
      });

      const req = httpMock.expectOne('/api/admin/articles/article-1/archive');
      expect(req.request.method).toBe('PUT');
      req.flush(mockArticle);
    });
  });

  describe('deleteArticle', () => {
    it('should DELETE to /api/admin/articles/:id', () => {
      service.deleteArticle('article-1').subscribe();

      const req = httpMock.expectOne('/api/admin/articles/article-1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('error handling', () => {
    it('should propagate 404 error', () => {
      let errorResponse: unknown = null;

      service.getArticleById('nonexistent').subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/articles/nonexistent');
      req.flush({ message: 'Article not found' }, { status: 404, statusText: 'Not Found' });

      expect((errorResponse as { status: number }).status).toBe(404);
    });

    it('should propagate 400 error on createArticle', () => {
      let errorResponse: unknown = null;

      service.createArticle({ titleEn: '', contentEn: '' }).subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/articles');
      req.flush({ message: 'Validation error' }, { status: 400, statusText: 'Bad Request' });

      expect((errorResponse as { status: number }).status).toBe(400);
    });
  });
});
