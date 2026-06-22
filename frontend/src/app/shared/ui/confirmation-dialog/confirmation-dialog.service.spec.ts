import { TestBed } from '@angular/core/testing';
import { Dialog } from '@angular/cdk/dialog';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ConfirmationDialogService } from './confirmation-dialog.service';
import { ConfirmationData } from './confirmation-dialog.model';

describe('ConfirmationDialogService', () => {
  let service: ConfirmationDialogService;
  let mockDialog: { open: ReturnType<typeof vi.fn> };

  const data: ConfirmationData = {
    title: 'Delete item',
    message: 'Are you sure?',
    confirmLabel: 'Delete',
  };

  beforeEach(() => {
    mockDialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        ConfirmationDialogService,
        { provide: Dialog, useValue: mockDialog },
      ],
    });
    service = TestBed.inject(ConfirmationDialogService);
  });

  it('should open the dialog with the provided data', () => {
    mockDialog.open.mockReturnValue({ closed: of(true) });
    service.confirm(data).subscribe();
    expect(mockDialog.open).toHaveBeenCalledTimes(1);
    const call = mockDialog.open.mock.calls[0];
    const component = call[0] as unknown;
    expect(component).toBeDefined();
    const options = call[1] as { data: ConfirmationData; width: string; disableClose: boolean };
    expect(options.data).toEqual(data);
    expect(options.width).toBe('420px');
    expect(options.disableClose).toBe(false);
  });

  it('should return the closed observable from the dialog ref', () => {
    mockDialog.open.mockReturnValue({ closed: of(true) });
    let received: boolean | undefined;
    service.confirm(data).subscribe((result) => {
      received = result;
    });
    expect(received).toBe(true);
  });

  it('should return undefined when the dialog is dismissed without a result', () => {
    mockDialog.open.mockReturnValue({ closed: of(undefined) });
    let received: boolean | undefined = false;
    service.confirm(data).subscribe((result) => {
      received = result;
    });
    expect(received).toBeUndefined();
  });
});
