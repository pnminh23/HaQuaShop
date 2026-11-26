import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Subject, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminApiService } from '../../core/services/admin-api.service';
import { CustomerRow } from '../../core/models/admin.model';
import { Page } from '../../core/models/common.model';
import { SeoService } from '../../core/services/seo.service';
import { SearchBox } from '../../shared/components/search-box';
import { FilterOption, ThFilter } from '../../shared/components/th-filter';
import { VndPipe } from '../../shared/pipes/vnd.pipe';

/** UC20 – quản lý khách hàng. */
@Component({
  selector: 'app-admin-customers',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [VndPipe, SearchBox, ThFilter],
  templateUrl: './admin-customers.html',
  styleUrls: ['./admin.scss'],
})
export class AdminCustomers {
  private readonly dialog = inject(DialogService);
  private readonly api = inject(AdminApiService);
  protected readonly result = signal<Page<CustomerRow> | null>(null);
  protected readonly page = signal(0);
  protected readonly status = signal('');
  protected readonly statusOptions: FilterOption[] = [{ value: 'ACTIVE', label: 'Hoạt động' }, { value: 'LOCKED', label: 'Đã khóa' }];
  private q = '';
  private readonly reload$ = new Subject<void>();

  constructor() {
    inject(SeoService).set('Quản trị · Khách hàng | HaQua');
    this.reload$
      .pipe(switchMap(() => this.api.customers(this.q, this.status(), this.page())), takeUntilDestroyed())
      .subscribe((r) => this.result.set(r));
    this.load();
  }

  protected search(value: string): void { this.q = value.trim(); this.page.set(0); this.load(); }
  protected setStatus(v: string): void { this.status.set(v); this.page.set(0); this.load(); }
  protected goto(p: number): void { this.page.set(p); this.load(); }

  protected async toggleLock(c: CustomerRow): Promise<void> {
    const lock = c.status === 'ACTIVE';
    if (!(await this.dialog.confirm({ message: lock ? `Khóa tài khoản ${c.fullName}?` : `Mở khóa tài khoản ${c.fullName}?`, confirmLabel: lock ? 'Khóa' : 'Mở khóa', danger: lock }))) return;
    this.api.setCustomerLock(c.id, lock).subscribe(() => this.load());
  }

  private load(): void { this.reload$.next(); }
}
