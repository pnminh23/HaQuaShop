import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { interval } from 'rxjs';
import { AdminApiService } from '../../core/services/admin-api.service';
import { AuthService } from '../../core/services/auth.service';
import { ChatbotConfig, ChatStats, ConversationDetail, ConversationRow } from '../../core/models/admin.model';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';

/** UC16 – giám sát, tiếp quản hội thoại; UC17 – cấu hình chatbot. */
@Component({
  selector: 'app-admin-chatbot',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DatePipe],
  templateUrl: './admin-chatbot.html',
  styleUrls: ['./admin.scss', './admin-chatbot.scss'],
})
export class AdminChatbot {
  private readonly api = inject(AdminApiService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly stats = signal<ChatStats | null>(null);
  protected readonly list = signal<ConversationRow[]>([]);
  protected readonly current = signal<ConversationDetail | null>(null);
  protected readonly config = signal<ChatbotConfig | null>(null);
  protected draft = '';

  protected readonly tones = [
    { id: 'FRIENDLY', label: 'Thân thiện' }, { id: 'PROFESSIONAL', label: 'Chuyên nghiệp' }, { id: 'SHORT', label: 'Ngắn gọn' },
  ] as const;
  protected readonly rules = [
    { id: 'ASK_OR_UNSURE', label: 'Khách yêu cầu hoặc AI không chắc chắn' },
    { id: 'ASK_ONLY', label: 'Chỉ khi khách yêu cầu' },
    { id: 'ORDER_OVER_30M', label: 'Đơn hàng trên 30 triệu' },
  ] as const;
  protected readonly sourceLabel: Record<string, string> = {
    CATALOG: 'Danh mục sản phẩm', POLICY: 'Chính sách', FAQ: 'Câu hỏi thường gặp', ORDER_LOOKUP: 'Tra cứu đơn hàng',
  };

  constructor() {
    inject(SeoService).set('Quản trị · Chatbot AI | HaQua');
    this.refresh();
    if (this.auth.isAdmin()) this.api.chatbotConfig().subscribe((c) => this.config.set(c));
    // theo dõi hội thoại mới / tin nhắn của khách khi nhân viên đang mở
    interval(5000).pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.refresh();
      const c = this.current();
      if (c) this.api.conversation(c.id).subscribe((d) => this.current.set(d));
    });
  }

  protected pick(row: ConversationRow): void {
    this.api.conversation(row.id).subscribe((d) => this.current.set(d));
  }

  protected act(action: 'takeover' | 'give-back' | 'close'): void {
    const c = this.current();
    if (!c) return;
    this.api.conversationAction(c.id, action).subscribe({
      next: (d) => { this.current.set(d); this.refresh(); },
      error: (e) => this.toast.show(e.error?.detail ?? 'Không thực hiện được'),
    });
  }

  protected reply(): void {
    const c = this.current();
    if (!c || !this.draft.trim()) return;
    this.api.staffReply(c.id, this.draft).subscribe({
      next: (d) => { this.current.set(d); this.draft = ''; },
      error: (e) => this.toast.show(e.error?.detail ?? 'Không gửi được'),
    });
  }

  protected setSource(id: number, enabled: boolean): void {
    this.config.update((c) => c && { ...c, sources: c.sources.map((s) => (s.id === id ? { ...s, enabled } : s)) });
  }

  protected saveConfig(): void {
    const c = this.config();
    if (!c) return;
    this.api.saveChatbotConfig(c).subscribe({
      next: (r) => { this.config.set(r); this.toast.show('Đã lưu cấu hình'); },
      error: (e) => this.toast.show(e.error?.errors?.extraInstructions ?? e.error?.detail ?? 'Không lưu được'),
    });
  }

  protected patch(p: Partial<ChatbotConfig>): void {
    this.config.update((c) => c && { ...c, ...p });
  }

  private refresh(): void {
    this.api.chatStats().subscribe((s) => this.stats.set(s));
    this.api.conversations().subscribe((l) => this.list.set(l));
  }
}
