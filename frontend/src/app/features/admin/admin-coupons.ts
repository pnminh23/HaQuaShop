import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Coupon } from '../../core/models/admin.model';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { SearchBox } from '../../shared/components/search-box';
import { FilterOption, ThFilter } from '../../shared/components/th-filter';
import { VndPipe } from '../../shared/pipes/vnd.pipe';

/** UC19 – quản lý mã giảm giá. */
@Component({
  selector: 'app-admin-coupons',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DatePipe, VndPipe, SearchBox, ThFilter],
  templateUrl: './admin-coupons.html',
  styleUrls: ['./admin.scss'],
})
export class AdminCoupons {
  private readonly api = inject(AdminApiService);
  private readonly toast = inject(ToastService);

  protected readonly coupons = signal<Coupon[]>([]);
  protected readonly q = signal('');
  protected readonly typeFilter = signal('');
  protected readonly stateFilter = signal('');
  protected readonly typeOptions: FilterOption[] = [{ value: 'PERCENT', label: 'Phần trăm' }, { value: 'FIXED', label: 'Số tiền' }];
  protected readonly stateOptions: FilterOption[] = [{ value: 'on', label: 'Đang chạy' }, { value: 'off', label: 'Đã dừng' }];
  /** Danh sách ít nên lọc/tìm ngay trên trình duyệt. */
  protected readonly shown = computed(() => {
    const q = this.q().toLowerCase();
    return this.coupons().filter((c) =>
      (!q || c.code.toLowerCase().includes(q)) && (!this.typeFilter() || c.type === this.typeFilter())
      && (!this.stateFilter() || (this.stateFilter() === 'on') === this.active(c)));
  });
  protected readonly open = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(FormBuilder).nonNullable.group({
    code: ['', Validators.required],
    type: ['PERCENT' as 'PERCENT' | 'FIXED'],
    value: [10, Validators.min(1)],
    maxDiscount: [0],
    minOrder: [0],
    startAt: [''],
    endAt: [''],
    usageLimit: [0],
  });

  constructor() {
    inject(SeoService).set('Quản trị · Khuyến mãi | HaQua');
    this.load();
  }

  protected active(c: Coupon): boolean {
    return !c.endAt || new Date(c.endAt) > new Date();
  }

  protected create(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const body = { ...v, startAt: v.startAt ? new Date(v.startAt).toISOString() : null, endAt: v.endAt ? new Date(v.endAt).toISOString() : null };
    this.api.saveCoupon(null, body).subscribe({
      next: () => { this.open.set(false); this.form.reset({ type: 'PERCENT', value: 10 }); this.load(); this.toast.show('Đã tạo mã giảm giá'); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không lưu được'),
    });
  }

  protected suspend(c: Coupon): void {
    this.api.suspendCoupon(c.id).subscribe(() => this.load());
  }

  private load(): void {
    this.api.coupons().subscribe((l) => this.coupons.set(l));
  }
}
