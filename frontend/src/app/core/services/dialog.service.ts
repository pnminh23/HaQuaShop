import { Injectable, signal } from '@angular/core';

export interface DialogOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Nút xác nhận màu đỏ cho thao tác xóa/ẩn/hủy. */
  danger?: boolean;
  /** Có `input` thì là hộp nhập (thay prompt): người dùng phải nhập chữ mới xác nhận được. */
  input?: { label: string; placeholder?: string; required?: boolean };
}

export interface ActiveDialog extends DialogOptions { resolve: (value: string | boolean | null) => void }

/** Thay thế confirm()/prompt() gốc của trình duyệt bằng hộp thoại đúng giao diện của site. */
@Injectable({ providedIn: 'root' })
export class DialogService {
  readonly active = signal<ActiveDialog | null>(null);

  confirm(opts: DialogOptions): Promise<boolean> {
    return this.open(opts).then((v) => v === true);
  }

  /** Trả về chữ đã nhập, hoặc null nếu người dùng hủy. */
  prompt(opts: DialogOptions & { input: NonNullable<DialogOptions['input']> }): Promise<string | null> {
    return this.open(opts).then((v) => (typeof v === 'string' ? v : null));
  }

  close(value: string | boolean | null): void {
    const d = this.active();
    this.active.set(null);
    d?.resolve(value);
  }

  private open(opts: DialogOptions): Promise<string | boolean | null> {
    this.active()?.resolve(null); // đang mở hộp khác thì coi như hủy
    return new Promise((resolve) => this.active.set({ ...opts, resolve }));
  }
}
