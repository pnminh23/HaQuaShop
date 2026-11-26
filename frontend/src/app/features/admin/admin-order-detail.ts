import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { AdminApiService } from '../../core/services/admin-api.service';
import { OrderDetail } from '../../core/models/order.model';
import { SeoService } from '../../core/services/seo.service';
import { OrderView } from './order-view';

/** Trang chi tiết đơn hàng (nhấp đúp hàng ở bảng đơn). Thanh thao tác ghim ở đáy màn hình. */
@Component({
  selector: 'app-admin-order-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, OrderView],
  styleUrl: './admin.scss',
  template: `
    <nav class="breadcrumb" aria-label="Đường dẫn"><ol><li><a routerLink="/admin/don-hang">Đơn hàng</a></li><li aria-hidden="true">/</li><li aria-current="page">{{ order()?.code ?? '…' }}</li></ol></nav>
    @if (order(); as o) {
      <div class="page-head"><div><h1>Đơn {{ o.code }}</h1><p>{{ o.createdAt | date: 'dd/MM/yyyy HH:mm' }} · Kênh: {{ o.channel }}</p></div></div>
      <div class="card"><app-order-view [order]="o" [sticky]="true" (changed)="order.set($event)">
        <button leading type="button" class="btn" (click)="back()">← Quay lại</button>
      </app-order-view></div>
    } @else if (error()) {
      <div class="card"><p class="error-text" role="alert">{{ error() }}</p><button type="button" class="btn" style="margin-top: 12px" (click)="back()">← Quay lại</button></div>
    }
  `,
})
export class AdminOrderDetail {
  private readonly api = inject(AdminApiService);
  private readonly router = inject(Router);
  protected readonly order = signal<OrderDetail | null>(null);
  protected readonly error = signal('');
  private readonly id = toSignal(inject(ActivatedRoute).paramMap.pipe(map((p) => Number(p.get('id')))), { initialValue: 0 });

  constructor() {
    inject(SeoService).set('Quản trị · Chi tiết đơn hàng | HaQua');
    this.api.order(this.id()).subscribe({ next: (o) => this.order.set(o), error: () => this.error.set('Không tìm thấy đơn hàng') });
  }

  protected back(): void { void this.router.navigate(['/admin/don-hang']); }
}
