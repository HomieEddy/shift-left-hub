import { TestBed } from '@angular/core/testing';
import type { MockInstance } from 'vitest';

import { LoggerService } from './logger.service';

describe('LoggerService', () => {
  let service: LoggerService;
  let consoleErrorSpy: MockInstance;
  let consoleWarnSpy: MockInstance;

  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoggerService);
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    consoleWarnSpy.mockRestore();
  });

  it('forwards error message to console.error', () => {
    service.error('boom');
    expect(consoleErrorSpy).toHaveBeenCalledWith('boom');
  });

  it('forwards error message and context to console.error', () => {
    const ctx = { requestId: 'abc' };
    service.error('boom', ctx);
    expect(consoleErrorSpy).toHaveBeenCalledWith('boom', ctx);
  });

  it('forwards warn message to console.warn', () => {
    service.warn('careful');
    expect(consoleWarnSpy).toHaveBeenCalledWith('careful');
  });

  it('forwards warn message and context to console.warn', () => {
    service.warn('careful', { reason: 'transient' });
    expect(consoleWarnSpy).toHaveBeenCalledWith('careful', { reason: 'transient' });
  });

  it('is a singleton (providedIn: root)', () => {
    const again = TestBed.inject(LoggerService);
    expect(again).toBe(service);
  });
});