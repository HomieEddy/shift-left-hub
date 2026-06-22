import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TagService } from './tag.service';
import { TagDto } from '../models/tag.models';

describe('TagService', () => {
  let service: TagService;
  let httpMock: HttpTestingController;

  const mockTag: TagDto = {
    id: 'tag-1',
    nameEn: 'VPN',
    nameFr: 'VPN',
    color: '#3366ff',
    articleCount: 5,
    createdAt: '2024-06-01T10:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TagService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should GET /api/admin/tags with article counts', () => {
    service.getTags().subscribe((tags) => {
      expect(tags.length).toBe(1);
      expect(tags[0].articleCount).toBe(5);
    });
    const req = httpMock.expectOne('/api/admin/tags');
    expect(req.request.method).toBe('GET');
    req.flush([mockTag]);
  });

  it('should GET /api/admin/tags/:id on getTagById', () => {
    service.getTagById('tag-1').subscribe((tag) => {
      expect(tag.id).toBe('tag-1');
    });
    const req = httpMock.expectOne('/api/admin/tags/tag-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockTag);
  });

  it('should POST on createTag', () => {
    service.createTag({ nameEn: 'Email', nameFr: 'Courriel', color: '#3366ff' }).subscribe();
    const req = httpMock.expectOne('/api/admin/tags');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nameEn).toBe('Email');
    req.flush(mockTag);
  });

  it('should PUT on updateTag', () => {
    service.updateTag('tag-1', { nameEn: 'VPN v2', nameFr: 'VPN v2', color: '#3366ff' }).subscribe();
    const req = httpMock.expectOne('/api/admin/tags/tag-1');
    expect(req.request.method).toBe('PUT');
    req.flush(mockTag);
  });

  it('should DELETE on deleteTag', () => {
    service.deleteTag('tag-1').subscribe();
    const req = httpMock.expectOne('/api/admin/tags/tag-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
