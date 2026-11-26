import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  link?: { label: string; href: string };
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  show(message: string, link?: Toast['link'], ms = 3500): void {
    const toast: Toast = { id: ++this.seq, message, link };
    this.toasts.update((l) => [...l, toast]);
    setTimeout(() => this.dismiss(toast.id), ms);
  }

  dismiss(id: number): void {
    this.toasts.update((l) => l.filter((t) => t.id !== id));
  }
}
