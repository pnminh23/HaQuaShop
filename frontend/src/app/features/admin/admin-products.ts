import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Subject, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminApiService } from '../../core/services/admin-api.service';
import { AdminProductRow } from '../../core/models/admin.model';
import { Category } from '../../core/models/catalog.model';
import { Page } from '../../core/models/common.model';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { SearchBox } from '../../shared/components/search-box';
import { FilterOption, ThFilter } from '../../shared/components/th-filter';
import { RowLink } from '../../shared/directives/row-link';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { ProductEditor } from './product-editor';

const STATUS_LABEL: Record<string, string> = { DRAFT: 'Nháp', ACTIVE: 'Đang bán', OUT_OF_STOCK: 'Hết hàng', HIDDEN: 'Ẩn' };

/** UC13 – quản lý sản phẩm. Nhấp đúp hàng → trang chi tiết; nút Sửa → popup. */
@Component({
  selector: 'app-admin-products',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [VndPipe, SearchBox, ThFilter, RowLink, ProductEditor],
  templateUrl: './admin-products.html',
  styleUrls: ['./admin.scss', './admin-products.scss'],
})
export class AdminProducts {
  private readonly dialog = inject(DialogService);
  private readonly api = inject(AdminApiService);
  private readonly toast = inject(ToastService);

  protected readonly statusOptions: FilterOption[] = Object.entries(STATUS_LABEL).map(([value, label]) => ({ value, label }));
  protected readonly statusLabel = (s: string) => STATUS_LABEL[s] ?? s;
  protected readonly status = signal('');
  protected readonly categoryId = signal('');
  protected readonly categoryOptions = signal<FilterOption[]>([]);
  protected readonly q = signal('');
  protected readonly page = signal(0);
  protected readonly result = signal<Page<AdminProductRow> | null>(null);
  protected readonly editing = signal<{ id: number | null } | null>(null);

  private readonly reload$ = new Subject<void>();

  constructor() {
    inject(SeoService).set('Quản trị · Sản phẩm & SEO | HaQua');
    this.api.categories().subscribe((c: Category[]) => this.categoryOptions.set(c.map((x) => ({ value: String(x.id), label: x.hidden ? `${x.name} (đang ẩn)` : x.name }))));
    // switchMap: kết quả cũ về muộn không đè lên kết quả của lần lọc/tìm mới nhất
    this.reload$
      .pipe(switchMap(() => this.api.products(this.status() || null, Number(this.categoryId()) || null, this.q(), this.page())), takeUntilDestroyed())
      .subscribe((r) => this.result.set(r));
    this.reload();
  }

  protected reload(): void { this.reload$.next(); }
  protected filter(kind: 'status' | 'category', v: string): void {
    (kind === 'status' ? this.status : this.categoryId).set(v);
    this.page.set(0);
    this.reload();
  }
  protected search(v: string): void { this.q.set(v); this.page.set(0); this.reload(); }
  protected goto(p: number): void { this.page.set(p); this.reload(); }

  protected newProduct(): void { this.editing.set({ id: null }); }
  protected edit(row: AdminProductRow): void { this.editing.set({ id: row.id }); }

  protected async hide(row: AdminProductRow): Promise<void> {
    if (await this.dialog.confirm({ title: 'Ẩn sản phẩm', message: `Ẩn sản phẩm "${row.name}"? Sản phẩm đã có trong đơn hàng không xóa được, chỉ ẩn khỏi cửa hàng và có thể hiện lại.`, confirmLabel: 'Ẩn', danger: true })) {
      this.api.hideProduct(row.id).subscribe(() => { this.toast.show('Đã ẩn sản phẩm'); this.reload(); });
    }
  }

  protected show(row: AdminProductRow): void {
    this.api.showProduct(row.id).subscribe(() => { this.toast.show('Đã hiện lại sản phẩm'); this.reload(); });
  }
}
