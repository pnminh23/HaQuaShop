import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Category } from '../../core/models/catalog.model';
import { AdminApiService } from '../../core/services/admin-api.service';
import { DialogService } from '../../core/services/dialog.service';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';

/**
 * Cấu hình danh mục sản phẩm: thêm, đổi tên, xóa khi chưa có sản phẩm, và ẩn/hiện trên cửa hàng.
 * Ẩn danh mục thì mọi sản phẩm của nó cũng ẩn theo (backend lo), trạng thái từng sản phẩm giữ nguyên.
 */
@Component({
  selector: 'app-admin-categories',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-categories.html',
  styleUrls: ['./admin.scss'],
})
export class AdminCategories {
  private readonly api = inject(AdminApiService);
  private readonly dialog = inject(DialogService);
  private readonly toast = inject(ToastService);

  protected readonly categories = signal<Category[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly draft = signal('');
  protected readonly error = signal('');
  /** Danh mục đang chờ server đổi trạng thái, để không bấm hai lần. */
  protected readonly busyId = signal<number | null>(null);

  constructor() {
    inject(SeoService).set('Quản trị · Danh mục | HaQua');
    this.load();
  }

  protected add(input: HTMLInputElement): void {
    const name = input.value.trim();
    if (!name) return;
    this.api.createCategory({ name }).subscribe({
      next: (c) => { input.value = ''; this.done(`Đã thêm danh mục “${c.name}”`); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không thêm được'),
    });
  }

  protected async toggle(c: Category): Promise<void> {
    const hide = !c.hidden;
    if (hide && !(await this.dialog.confirm({
      message: `Ẩn danh mục "${c.name}"? Khách sẽ không còn thấy và không mua được ${c.productCount} sản phẩm thuộc danh mục này.`,
      confirmLabel: 'Ẩn danh mục', danger: true,
    }))) return;
    this.busyId.set(c.id);
    this.api.setCategoryHidden(c.id, hide).subscribe({
      next: () => { this.busyId.set(null); this.done(hide ? `Đã ẩn “${c.name}” và sản phẩm của nó` : `Đã hiện lại “${c.name}”`); },
      error: (e) => { this.busyId.set(null); this.error.set(e.error?.detail ?? 'Không đổi được trạng thái'); },
    });
  }

  protected startEdit(c: Category): void { this.draft.set(c.name); this.editingId.set(c.id); }

  protected save(c: Category): void {
    const name = this.draft().trim();
    if (!name || name === c.name) { this.editingId.set(null); return; }
    // giữ nguyên slug: đường dẫn, menu và chatbot đều dựa vào slug
    this.api.updateCategory(c.id, { name, slug: c.slug, seoDescription: c.seoDescription, parentId: c.parentId }).subscribe({
      next: () => { this.editingId.set(null); this.done('Đã đổi tên danh mục'); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không đổi được tên'),
    });
  }

  protected async remove(c: Category): Promise<void> {
    if (!(await this.dialog.confirm({ message: `Xóa danh mục "${c.name}"?`, confirmLabel: 'Xóa', danger: true }))) return;
    this.api.deleteCategory(c.id).subscribe({
      next: () => this.done('Đã xóa danh mục'),
      error: (e) => this.error.set(e.error?.detail ?? 'Không xóa được'),
    });
  }

  private done(message: string): void {
    this.error.set('');
    this.toast.show(message);
    this.load();
  }

  private load(): void {
    this.api.categories().subscribe({
      next: (list) => this.categories.set(list),
      error: (e) => this.error.set(e.error?.detail ?? 'Không tải được danh mục'),
    });
  }
}
