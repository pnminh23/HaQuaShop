import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { OrderTracking } from '../../core/models/order.model';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { SeoService } from '../../core/services/seo.service';
import { VndPipe } from '../../shared/pipes/vnd.pipe';

export const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PACKING: 'Đang đóng gói', SHIPPING: 'Đang giao', COMPLETED: 'Hoàn tất', CANCELLED: 'Đã hủy',
};

/** UC09 – tra cứu đơn bằng mã đơn + SĐT; thành viên xem danh sách đơn của mình. */
@Component({
  selector: 'app-order-lookup',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, VndPipe, DatePipe],
  templateUrl: './order-lookup.html',
  styleUrl: './order-lookup.scss',
})
export class OrderLookup {
  private readonly dialog = inject(DialogService);
  private readonly orders = inject(OrderService);
  protected readonly auth = inject(AuthService);

  protected code = inject(ActivatedRoute).snapshot.queryParamMap.get('code') ?? '';
  protected phone = '';
  protected readonly order = signal<OrderTracking | null>(null);
  protected readonly mine = signal<OrderTracking[]>([]);
  protected readonly error = signal('');
  protected readonly label = (s: string) => STATUS_LABEL[s] ?? s;

  constructor() {
    inject(SeoService).set('Tra cứu đơn hàng | HaQua');
    if (this.auth.isLoggedIn()) this.orders.mine().subscribe((l) => this.mine.set(l));
  }

  protected search(): void {
    this.error.set('');
    this.orders.lookup(this.code, this.phone).subscribe({
      next: (o) => this.order.set(o),
      error: (e) => {
        this.order.set(null);
        this.error.set(e.status === 429 ? e.error.detail : 'Không tìm thấy đơn hàng');
      },
    });
  }

  protected async cancel(o: OrderTracking): Promise<void> {
    if (!(await this.dialog.confirm({ message: 'Bạn chắc chắn muốn hủy đơn này?', confirmLabel: 'Hủy đơn', cancelLabel: 'Giữ đơn', danger: true }))) return;
    this.orders.cancel(o.code, this.phone || '').subscribe({
      next: (r) => this.order.set(r),
      error: (e) => this.error.set(e.error?.detail ?? 'Không hủy được đơn'),
    });
  }
}
