import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal, viewChild, ElementRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { interval, switchMap } from 'rxjs';
import { ChatMessage, ChatState } from '../../core/models/chat.model';
import { ProductCard } from '../../core/models/catalog.model';
import { CartService } from '../../core/services/cart.service';
import { ChatService } from '../../core/services/chat.service';
import { ToastService } from '../../core/services/toast.service';
import { chatMarkdown } from '../../shared/utils/chat-markdown';
import { VndPipe } from '../../shared/pipes/vnd.pipe';

@Component({
  selector: 'app-chat-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, VndPipe],
  templateUrl: './chat-widget.html',
  styleUrl: './chat-widget.scss',
})
export class ChatWidget {
  private readonly chat = inject(ChatService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly open = signal(false);
  protected readonly state = signal<ChatState | null>(null);
  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly typing = signal(false);
  protected draft = '';
  protected readonly md = chatMarkdown;

  protected readonly quick = ['Tư vấn laptop đồ họa', 'Điện thoại dưới 15 triệu', 'Chính sách đổi trả', 'Tra cứu đơn hàng'];
  private readonly list = viewChild<ElementRef<HTMLElement>>('list');

  constructor() {
    // tin nhắn của nhân viên đến qua polling khi khung chat đang mở
    interval(5000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.pollStaff());

    effect(() => {
      this.messages();
      queueMicrotask(() => this.list()?.nativeElement.scrollTo({ top: 1e6 }));
    });
  }

  protected toggle(): void {
    this.open.update((v) => !v);
    if (this.open() && !this.state()) {
      this.chat.open().subscribe((s) => this.apply(s));
    }
  }

  protected send(text = this.draft): void {
    const content = text.trim();
    if (!content) return;
    this.draft = '';
    this.messages.update((m) => [...m, { id: -Date.now(), sender: 'USER', content, products: [], createdAt: '' }]);
    this.typing.set(true);
    this.chat.send(content).subscribe({
      next: (s) => {
        this.typing.set(false);
        this.apply(s);
      },
      error: () => this.typing.set(false),
    });
  }

  protected human(): void {
    this.chat.requestStaff().pipe(switchMap(() => this.chat.open())).subscribe((s) => this.apply(s));
  }

  protected restart(): void {
    this.chat.reset();
    this.state.set(null);
    this.messages.set([]);
    this.chat.open().subscribe((s) => this.apply(s));
  }

  protected addToCart(p: ProductCard): void {
    if (!p.defaultVariantId) return;
    this.cart.addFromChat(p.defaultVariantId).subscribe({
      next: () => this.toast.show(`Đã thêm ${p.name} vào giỏ`, { label: 'Xem giỏ', href: '/gio-hang' }),
      error: (e) => this.toast.show(e.error?.detail ?? 'Không thêm được vào giỏ'),
    });
  }

  private pollStaff(): void {
    const s = this.state();
    if (!this.open() || !s || !['STAFF', 'WAITING_STAFF'].includes(s.status)) return;
    const last = Math.max(0, ...this.messages().map((m) => m.id));
    this.chat.poll(last).subscribe((r) => {
      this.state.set({ ...s, status: r.status });
      if (r.messages.length) this.messages.update((m) => [...m, ...r.messages]);
    });
  }

  private apply(s: ChatState): void {
    this.state.set(s);
    this.messages.set(s.messages);
  }
}
