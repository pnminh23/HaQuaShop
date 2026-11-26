import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { AdminApiService } from '../../core/services/admin-api.service';
import { ChatStats } from '../../core/models/admin.model';
import { SeoService } from '../../core/services/seo.service';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { STATUS_LABEL } from '../orders/order-lookup';

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
  protected readonly range = signal(14);
  protected readonly selected = signal<number | null>(null);

  protected readonly data = toSignal(toObservable(this.range).pipe(switchMap((r) => this.api.dashboard(r))), { initialValue: null });
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

  protected export(): void {
    this.api.exportDashboard(this.range()).subscribe((blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `doanh-thu-${this.range()}-ngay.csv`;
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }
}
