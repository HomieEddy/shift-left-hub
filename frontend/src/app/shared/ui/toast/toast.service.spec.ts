import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';
import { DEFAULT_DURATION, ToastMessage } from './toast.model';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('should emit a success toast with default duration', () => {
    let received: ToastMessage | undefined;
    service.toasts$.subscribe((t) => {
      received = t;
    });
    const id = service.success('Saved');
    expect(received).toBeDefined();
    expect(received!.id).toBe(id);
    expect(received!.type).toBe('success');
    expect(received!.message).toBe('Saved');
    expect(received!.duration).toBe(DEFAULT_DURATION);
  });

  it('should emit an error toast', () => {
    let received: ToastMessage | undefined;
    service.toasts$.subscribe((t) => {
      received = t;
    });
    service.error('Boom');
    expect(received).toBeDefined();
    expect(received!.type).toBe('error');
  });

  it('should emit a warning toast with custom duration', () => {
    let received: ToastMessage | undefined;
    service.toasts$.subscribe((t) => {
      received = t;
    });
    service.warning('Heads up', 2000);
    expect(received).toBeDefined();
    expect(received!.type).toBe('warning');
    expect(received!.duration).toBe(2000);
  });

  it('should emit an info toast via info()', () => {
    let received: ToastMessage | undefined;
    service.toasts$.subscribe((t) => {
      received = t;
    });
    service.info('FYI');
    expect(received).toBeDefined();
    expect(received!.type).toBe('info');
  });

  it('should emit an undo toast with action handler and 6s default', () => {
    let received: ToastMessage | undefined;
    const handler = () => undefined;
    service.toasts$.subscribe((t) => {
      received = t;
    });
    service.undo('Item deleted', handler);
    expect(received).toBeDefined();
    expect(received!.type).toBe('info');
    expect(received!.duration).toBe(6000);
    expect(received!.action).toEqual({ label: 'Undo', handler });
  });

  it('should emit a dismiss event with the toast id', () => {
    let received: string | undefined;
    service.dismiss$.subscribe((id) => {
      received = id;
    });
    service.dismiss('toast-123');
    expect(received).toBe('toast-123');
  });

  it('should return a unique id per toast', () => {
    const a = service.success('A');
    const b = service.success('B');
    expect(a).not.toBe(b);
  });
});
