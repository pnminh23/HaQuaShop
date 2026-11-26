import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Brand } from '../../core/models/admin.model';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { SearchBox } from '../../shared/components/search-box';
import { SlugUtil } from './slug.util';

/**
 * Cấu hình thương hiệu: thêm, sửa (đổi tên thì cập nhật sản phẩm liên quan), xóa khi chưa có sản phẩm,
 * và chọn hãng máy tính hiện trên menu cửa hàng cùng thứ tự.
 */
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
  protected readonly onlyComputer = signal(false);
  protected readonly shown = computed(() => {
    const q = SlugUtil.toSlug(this.q());
    return this.brands().filter((b) => (!this.onlyComputer() || b.computerBrand) && (!q || SlugUtil.toSlug(b.name).includes(q)));
  });
  protected readonly editingId = signal<number | null>(null);
  protected readonly draft = signal('');
  protected readonly error = signal('');

  constructor() {
    inject(SeoService).set('Quản trị · Thương hiệu | HaQua');
    this.load();
  }

  protected add(input: HTMLInputElement, computer: HTMLInputElement): void {
    const name = input.value.trim();
    if (!name) return;
    // không tích thì để trống: không ghi đè cấu hình nếu thương hiệu đã tồn tại
    this.api.createBrand(name, computer.checked || undefined).subscribe({
      next: (b) => { input.value = ''; this.error.set(''); this.toast.show(`Đã thêm thương hiệu “${b.name}”`); this.load(); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không thêm được'),
    });
  }

  protected startEdit(b: Brand): void { this.draft.set(b.name); this.editingId.set(b.id); }

  protected save(b: Brand, name: string): void {
    const v = name.trim();
    if (!v || v === b.name) { this.editingId.set(null); return; }
    this.api.updateBrand(b.id, { name: v }).subscribe({
      next: () => { this.editingId.set(null); this.error.set(''); this.toast.show('Đã đổi tên thương hiệu'); this.load(); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không đổi được tên'),
    });
  }

  protected toggleComputer(b: Brand, box: HTMLInputElement): void {
    this.api.updateBrand(b.id, { name: b.name, computerBrand: box.checked }).subscribe({
      next: () => { this.error.set(''); this.toast.show(box.checked ? `Đã đưa ${b.name} lên menu` : `Đã gỡ ${b.name} khỏi menu`); this.load(); },
      error: (e) => { box.checked = b.computerBrand; this.error.set(e.error?.detail ?? 'Không lưu được'); },
    });
  }

  protected saveOrder(b: Brand, input: HTMLInputElement): void {
    const menuOrder = Math.max(0, Math.trunc(Number(input.value) || 0));
    input.value = String(menuOrder);
    if (menuOrder === b.menuOrder) return;
    this.api.updateBrand(b.id, { name: b.name, menuOrder }).subscribe({
      next: () => { this.error.set(''); this.toast.show('Đã lưu thứ tự menu'); this.load(); },
      error: (e) => { input.value = String(b.menuOrder); this.error.set(e.error?.detail ?? 'Không lưu được'); },
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
