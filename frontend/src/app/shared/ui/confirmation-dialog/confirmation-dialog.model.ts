import { Observable } from 'rxjs';

export interface ConfirmationData {
  titleKey: string;
  messageKey: string;
  confirmLabelKey: string;
  itemIdentifier?: string;
  onConfirm?: () => Observable<void> | Promise<void>;
}
