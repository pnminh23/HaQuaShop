import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, startWith, switchMap, tap } from 'rxjs';
import { AdminApiService } from '../../core/services/admin-api.service';
import { ChatStats, DashboardPeriod } from '../../core/models/admin.model';
import { SeoService } from '../../core/services/seo.service';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { STATUS_LABEL } from '../orders/order-lookup';
import { isoDate, monthOptions, rangeError } from './dashboard-period';

/** UC12 – bảng điều khiển thống kê. */
@Component({
  selector: 'app-admin-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [VndPipe, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrls: ['./admin.scss'],
})
export class AdminDashboard {
  private readonly api = inject(AdminApiService);

  protected readonly ranges = [7, 14, 30];
  protected readonly period = signal<DashboardPeriod>({ range: 14 });
  protected readonly today = isoDate(new Date());
  protected readonly months = monthOptions(new Date(), 12);
  protected readonly fromDate = signal('');
  protected readonly toDate = signal('');
  protected readonly error = signal('');
  protected readonly selected = signal<number | null>(null);

  // null = đang tải. Bắt lỗi bên trong switchMap để một lần lỗi không làm các lần chọn sau mất tác dụng.
  protected readonly data = toSignal(
    toObservable(this.period).pipe(
      tap(() => this.selected.set(null)),
      switchMap((p) =>
        this.api.dashboard(p).pipe(
          // bộ lọc luôn ghi đúng kỳ của số liệu đang hiển thị, kể cả khi xem theo nút 7/14/30 ngày
          tap((d) => {
            this.fromDate.set(d.from);
            this.toDate.set(d.to);
          }),
          catchError((e) => {
            this.error.set(e.error?.detail ?? 'Không tải được số liệu, vui lòng thử lại.');
            return of(null);
          }),
          startWith(null),
        ),
      ),
    ),
    { initialValue: null },
  );
  protected readonly range = computed(() => {
    const p = this.period();
    return 'range' in p ? p.range : null;
  });
  /** Chỉ chọn sẵn tháng khi số liệu đang hiển thị đúng là trọn tháng đó. */
  protected readonly month = computed(() => {
    const d = this.data();
    return d ? (this.months.find((m) => m.from === d.from && m.to === d.to)?.value ?? '') : '';
  });
  protected readonly chat = toSignal<ChatStats | null>(this.api.chatStats(), { initialValue: null });
  protected readonly label = (s: string) => STATUS_LABEL[s] ?? s;

  protected readonly max = computed(() => Math.max(1, ...(this.data()?.revenueByDay.map((d) => d.revenue) ?? [1])));
  protected readonly bars = computed(() => (this.data()?.revenueByDay ?? []).map((d, i) => ({ ...d, i, pct: (d.revenue / this.max()) * 100 })));
  protected readonly picked = computed(() => this.bars()[this.selected() ?? this.bars().length - 1]);

  constructor() {
    inject(SeoService).set('Quản trị · Tổng quan | HaQua');
  }

  protected isMoney(label: string): boolean {
    return label === 'Doanh thu';
  }

  protected pickRange(days: number): void {
    this.fromDate.set('');
    this.toDate.set('');
    this.show({ range: days });
  }

  protected pickMonth(value: string): void {
    const m = this.months.find((x) => x.value === value);
    if (!m) return;
    this.fromDate.set(m.from);
    this.toDate.set(m.to);
    this.show({ from: m.from, to: m.to });
  }

  protected applyDates(): void {
    const problem = rangeError(this.fromDate(), this.toDate(), this.today);
    if (problem) {
      this.error.set(problem);
      return;
    }
    this.show({ from: this.fromDate(), to: this.toDate() });
  }

  private show(period: DashboardPeriod): void {
    this.error.set('');
    this.period.set(period);
  }

  protected export(): void {
    const p = this.period();
    const name = 'range' in p ? `${p.range}-ngay` : `${p.from}_${p.to}`;
    this.api.exportDashboard(p).subscribe((blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `doanh-thu-${name}.csv`;
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }
}
