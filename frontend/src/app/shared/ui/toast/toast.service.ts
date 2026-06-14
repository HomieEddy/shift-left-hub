import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { ToastMessage, ToastType, DEFAULT_DURATION } from './toast.model';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private _toasts = new Subject<ToastMessage>();
  readonly toasts$ = this._toasts.asObservable();
  private _dismiss = new Subject<string>();
  readonly dismiss$ = this._dismiss.asObservable();

  private add(
    type: ToastType,
    message: string,
    duration?: number,
    action?: ToastMessage['action'],
  ): string {
    const id = crypto.randomUUID();
    this._toasts.next({ id, type, message, duration: duration ?? DEFAULT_DURATION, action });
    return id;
  }

  success(message: string, duration?: number): string {
    return this.add('success', message, duration);
  }
  error(message: string, duration?: number): string {
    return this.add('error', message, duration);
  }
  warning(message: string, duration?: number): string {
    return this.add('warning', message, duration);
  }
  info(message: string, duration?: number): string {
    return this.add('info', message, duration);
  }
  undo(message: string, onUndo: () => void, duration = 6000): string {
    return this.add('info', message, duration, { label: 'Undo', handler: onUndo });
  }
  dismiss(id: string): void {
    this._dismiss.next(id);
  }
}
