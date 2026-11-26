import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Subject, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Page } from '../../core/models/common.model';
import { OrderDetail, OrderRow } from '../../core/models/order.model';
import { SeoService } from '../../core/services/seo.service';
import { SearchBox } from '../../shared/components/search-box';
import { FilterOption, ThFilter } from '../../shared/components/th-filter';
import { RowLink } from '../../shared/directives/row-link';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { STATUS_LABEL } from '../orders/order-lookup';
import { OrderView } from './order-view';

/** UC15 – quản lý đơn hàng. Nhấp đúp hàng → trang chi tiết; bấm mã đơn → popup xem nhanh. */
@Component({
  selector: 'app-admin-orders',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [VndPipe, DatePipe, SearchBox, ThFilter, RowLink, OrderView],
  templateUrl: './admin-orders.html',
  styleUrls: ['./admin.scss'],
})
export class AdminOrders {
  private readonly api = inject(AdminApiService);

  protected readonly statusOptions: FilterOption[] = [
    { value: 'PENDING', label: 'Chờ xác nhận' }, { value: 'PROCESSING', label: 'Đang xử lý' },
    { value: 'SHIPPING', label: 'Đang giao' }, { value: 'COMPLETED', label: 'Hoàn tất' }, { value: 'CANCELLED', label: 'Đã hủy' },
  ];
  protected readonly tab = signal('');
  protected readonly q = signal('');
  protected readonly page = signal(0);
  protected readonly result = signal<Page<OrderRow> | null>(null);
  protected readonly selected = signal<OrderDetail | null>(null);
  protected readonly label = (s: string) => STATUS_LABEL[s] ?? s;

  private readonly reload$ = new Subject<void>();

  constructor() {
    inject(SeoService).set('Quản trị · Đơn hàng | HaQua');
    this.reload$
      .pipe(switchMap(() => this.api.orders(this.tab() || 'ALL', this.q(), this.page())), takeUntilDestroyed())
      .subscribe((r) => this.result.set(r));
    this.reload();
  }

  protected reload(): void { this.reload$.next(); }
  protected setTab(id: string): void { this.tab.set(id); this.page.set(0); this.reload(); }
  protected search(v: string): void { this.q.set(v); this.page.set(0); this.reload(); }
  protected goto(p: number): void { this.page.set(p); this.reload(); }

  protected open(row: OrderRow): void {
    this.api.order(row.id).subscribe((o) => this.selected.set(o));
  }

  protected updated(o: OrderDetail): void {
    this.selected.set(o);
    this.reload();
  }
}
