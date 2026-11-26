import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Brand } from '../../core/models/admin.model';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { SearchBox } from '../../shared/components/search-box';
import { SlugUtil } from './slug.util';

/** Cấu hình danh mục thương hiệu: thêm, đổi tên (cập nhật sản phẩm liên quan), xóa khi chưa có sản phẩm. */
@Component({
  selector: 'app-admin-brands',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SearchBox],
  templateUrl: './admin-brands.html',
  styleUrls: ['./admin.scss'],
})
export class AdminBrands {
  private readonly dialog = inject(DialogService);
  private readonly api = inject(AdminApiService);
  private readonly toast = inject(ToastService);

  protected readonly brands = signal<Brand[]>([]);
  protected readonly q = signal('');
  protected readonly shown = computed(() => {
    const q = SlugUtil.toSlug(this.q());
    return this.brands().filter((b) => !q || SlugUtil.toSlug(b.name).includes(q));
  });
  protected readonly editingId = signal<number | null>(null);
  protected readonly draft = signal('');
  protected readonly error = signal('');

  constructor() {
    inject(SeoService).set('Quản trị · Thương hiệu | HaQua');
    this.load();
  }

  protected add(input: HTMLInputElement): void {
    const name = input.value.trim();
    if (!name) return;
    this.api.createBrand(name).subscribe({
      next: (b) => { input.value = ''; this.error.set(''); this.toast.show(`Đã thêm thương hiệu “${b.name}”`); this.load(); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không thêm được'),
    });
  }

  protected startEdit(b: Brand): void { this.draft.set(b.name); this.editingId.set(b.id); }

  protected save(b: Brand, name: string): void {
    const v = name.trim();
    if (!v || v === b.name) { this.editingId.set(null); return; }
    this.api.renameBrand(b.id, v).subscribe({
      next: () => { this.editingId.set(null); this.error.set(''); this.toast.show('Đã đổi tên thương hiệu'); this.load(); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không đổi được tên'),
    });
  }

  protected async remove(b: Brand): Promise<void> {
    if (!(await this.dialog.confirm({ message: `Xóa thương hiệu "${b.name}"?`, confirmLabel: 'Xóa', danger: true }))) return;
    this.api.deleteBrand(b.id).subscribe({
      next: () => { this.error.set(''); this.toast.show('Đã xóa thương hiệu'); this.load(); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không xóa được'),
    });
  }

  private load(): void {
    this.api.brands().subscribe((l) => this.brands.set(l));
  }
}
