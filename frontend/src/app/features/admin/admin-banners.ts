import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { assetUrl } from '../../core/config/api.config';
import { Banner } from '../../core/models/banner.model';
import { AdminApiService } from '../../core/services/admin-api.service';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { compressImage } from '../../shared/utils/compress-image';

/** Cấu hình banner trang chủ: nhiều slide, mỗi slide có ảnh + tiêu đề (+ mô tả, nút bấm), kéo thứ tự bằng nút ▲▼. */
@Component({
  selector: 'app-admin-banners',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-banners.html',
  styleUrls: ['./admin.scss', './admin-banners.scss'],
})
export class AdminBanners {
  private readonly dialog = inject(DialogService);
  private readonly api = inject(AdminApiService);
  private readonly toast = inject(ToastService);

  protected readonly items = signal<Banner[]>([]);
  protected readonly editing = signal<{ id: number | null } | null>(null);
  protected readonly uploading = signal(false);
  protected readonly error = signal('');
  protected readonly img = (u: string | null) => assetUrl(u);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    subtitle: [''],
    imageUrl: ['', Validators.required],
    altText: [''],
    linkUrl: [''],
    ctaLabel: [''],
    active: [true],
  });

  constructor() {
    inject(SeoService).set('Quản trị · Banner trang chủ | HaQua');
    this.load();
  }

  private load(): void { this.api.banners().subscribe((b) => this.items.set(b)); }

  protected add(): void {
    this.form.reset({ active: true });
    this.error.set('');
    this.editing.set({ id: null });
  }

  protected edit(b: Banner): void {
    this.form.reset({ title: b.title, subtitle: b.subtitle ?? '', imageUrl: b.imageUrl, altText: b.altText ?? '', linkUrl: b.linkUrl ?? '', ctaLabel: b.ctaLabel ?? '', active: b.active });
    this.error.set('');
    this.editing.set({ id: b.id });
  }

  protected async upload(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.uploading.set(true);
    try {
      // giữ nguyên ảnh gốc nếu hợp lệ và ≤ 6 MB (giữ chất lượng cho banner); ngược lại nén xuống ≤ 5 MB
      const keep = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type) && file.size <= 6 * 1024 * 1024;
      const blob = keep ? file : await compressImage(file, 2400, 0.9, 5 * 1024 * 1024);
      const r = await firstValueFrom(this.api.upload(new File([blob], 'banner', { type: blob.type || file.type })));
      this.form.patchValue({ imageUrl: r.url });
    } catch (e) {
      this.toast.show((e as { error?: { detail?: string } }).error?.detail ?? 'Không tải được ảnh');
    }
    this.uploading.set(false);
  }

  protected save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.error.set('Cần có tiêu đề và ảnh banner'); return; }
    this.api.saveBanner(this.editing()!.id, this.form.getRawValue()).subscribe({
      next: () => { this.toast.show('Đã lưu banner'); this.editing.set(null); this.load(); },
      error: (e) => this.error.set(e.error?.detail ?? 'Không lưu được'),
    });
  }

  protected toggle(b: Banner): void {
    this.api.saveBanner(b.id, { ...b, active: !b.active }).subscribe(() => this.load());
  }

  protected move(i: number, d: number): void {
    const list = [...this.items()];
    const j = i + d;
    if (j < 0 || j >= list.length) return;
    [list[i], list[j]] = [list[j], list[i]];
    this.items.set(list);
    this.api.reorderBanners(list.map((x) => x.id)).subscribe({ next: (r) => this.items.set(r), error: () => this.load() });
  }

  protected async remove(b: Banner): Promise<void> {
    if (await this.dialog.confirm({ message: `Xóa banner "${b.title}"?`, confirmLabel: 'Xóa', danger: true })) this.api.deleteBanner(b.id).subscribe(() => this.load());
  }
}
