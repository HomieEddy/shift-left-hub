import { TestBed } from '@angular/core/testing';
import { AuthTokenService } from './auth-token.service';

describe('AuthTokenService', () => {
  let service: AuthTokenService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthTokenService);
  });

  it('should start with no access token', () => {
    expect(service.accessToken()).toBeNull();
  });

  it('should store and return the access token', () => {
    service.setAccessToken('abc123');
    expect(service.accessToken()).toBe('abc123');
  });

  it('should clear the access token', () => {
    service.setAccessToken('abc123');
    service.clear();
    expect(service.accessToken()).toBeNull();
  });
});
