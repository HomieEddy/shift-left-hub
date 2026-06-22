import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { CategoryService } from './category.service';
import { CategoryDto } from './category.model';

describe('CategoryService', () => {
  let service: CategoryService;
  let httpMock: HttpTestingController;

  const mockCategory: CategoryDto = {
    id: 'cat-1',
    nameEn: 'Hardware',
    nameFr: 'Matériel',
    parentId: null,
    childCount: 0,
    createdAt: '2024-06-01T10:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CategoryService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(CategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should GET /api/admin/categories', () => {
    service.getAll().subscribe((cats) => {
      expect(cats.length).toBe(1);
    });
    const req = httpMock.expectOne('/api/admin/categories');
    expect(req.request.method).toBe('GET');
    req.flush([mockCategory]);
  });

  it('should GET /api/admin/categories/:id on get', () => {
    service.get('cat-1').subscribe((cat) => {
      expect(cat.id).toBe('cat-1');
    });
    const req = httpMock.expectOne('/api/admin/categories/cat-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockCategory);
  });

  it('should POST on create', () => {
    service.create({ nameEn: 'Network', nameFr: 'Réseau', parentId: null }).subscribe((cat) => {
      expect(cat.id).toBe('cat-2');
    });
    const req = httpMock.expectOne('/api/admin/categories');
    expect(req.request.method).toBe('POST');
    const body = req.request.body as { nameEn?: string };
    expect(body.nameEn).toBe('Network');
    req.flush({ ...mockCategory, id: 'cat-2' });
  });

  it('should PUT on update', () => {
    service.update('cat-1', { nameEn: 'Hardware v2', nameFr: 'Matériel v2', parentId: null }).subscribe();
    const req = httpMock.expectOne('/api/admin/categories/cat-1');
    expect(req.request.method).toBe('PUT');
    req.flush(mockCategory);
  });

  it('should DELETE without query param when no reassignTo', () => {
    service.delete('cat-1').subscribe();
    const req = httpMock.expectOne('/api/admin/categories/cat-1');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.params.has('reassignTo')).toBe(false);
    req.flush(null);
  });

  it('should DELETE with reassignTo query param when provided', () => {
    service.delete('cat-1', 'cat-other').subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === '/api/admin/categories/cat-1'
        && r.params.get('reassignTo') === 'cat-other');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should POST on merge', () => {
    service.merge({ sourceCategoryId: 'cat-1', targetCategoryId: 'cat-2' }).subscribe();
    const req = httpMock.expectOne('/api/admin/categories/merge');
    expect(req.request.method).toBe('POST');
    const body = req.request.body as { sourceCategoryId?: string };
    expect(body.sourceCategoryId).toBe('cat-1');
    req.flush(mockCategory);
  });
});
