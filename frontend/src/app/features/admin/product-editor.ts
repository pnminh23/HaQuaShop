import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, firstValueFrom, forkJoin, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminApiService } from '../../core/services/admin-api.service';
import { AdminProductRequest, Brand, SeoReport } from '../../core/models/admin.model';
import { Category } from '../../core/models/catalog.model';
import { ProductStatus } from '../../core/models/common.model';
import { assetUrl } from '../../core/config/api.config';
import { ToastService } from '../../core/services/toast.service';
import { compressImage } from '../../shared/utils/compress-image';
import { SlugUtil } from './slug.util';

/**
 * Biểu mẫu sản phẩm + SEO dùng chung cho popup (nút Sửa ở bảng) và trang chi tiết.
 * Thanh Lưu / Quay lại luôn ghim ở đáy khung nhìn để nội dung dài không phải cuộn xuống mới lưu được.
 */
@Component({
  selector: 'app-product-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './product-editor.html',
  styleUrls: ['./admin.scss', './admin-products.scss', './product-editor.scss'],
  host: { '[class.page]': "mode() === 'page'" },
})
export class ProductEditor {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);

  /** null = thêm mới. */
  readonly productId = input<number | null>(null);
  readonly mode = input<'popup' | 'page'>('popup');
  readonly saved = output<number>();
  readonly closed = output<void>();

  protected readonly categories = signal<Category[]>([]);
  protected readonly brands = signal<Brand[]>([]);
  protected readonly brandQuery = signal('');
  protected readonly brandOpen = signal(false);
  protected readonly brandActive = signal(0);
  protected readonly brandCreating = signal(false);
  protected readonly loading = signal(false);
  /** Gợi ý lọc theo chữ đang gõ (không phân biệt hoa thường/dấu). */
  protected readonly brandMatches = computed(() => {
    const q = SlugUtil.toSlug(this.brandQuery());
    return this.brands().filter((b) => !q || SlugUtil.toSlug(b.name).includes(q));
  });
  /** Có cho tạo mới khi chữ gõ chưa trùng thương hiệu nào. */
  protected readonly brandCanCreate = computed(() => {
    const q = this.brandQuery().trim().toLowerCase();
    return !!q && !this.brands().some((b) => b.name.toLowerCase() === q);
  });

  protected readonly seo = signal<SeoReport | null>(null);
  protected readonly errors = signal('');
  protected readonly slugTouched = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    categoryId: [0, Validators.min(1)],
    brand: [''],
    description: [''],
    status: ['ACTIVE' as ProductStatus],
    slug: [''],
    focusKeyword: [''],
    metaTitle: [''],
    metaDescription: [''],
    variants: this.fb.array([this.variantGroup()]),
    images: this.fb.array<ReturnType<typeof this.imageGroup>>([]),
  });

  constructor() {
    this.api.brands().subscribe((b) => this.brands.set(b));
    effect(() => { const id = this.productId(); untracked(() => this.open(id)); });

    // chấm điểm SEO trực tiếp khi gõ (server là nơi chấm điểm chính thức khi lưu)
    this.form.valueChanges
      .pipe(debounceTime(400), switchMap(() => this.api.evaluateSeo(this.request())), takeUntilDestroyed())
      .subscribe({ next: (r) => this.seo.set(r), error: () => undefined });
  }

  protected get variants(): FormArray { return this.form.controls.variants; }
  protected get images(): FormArray { return this.form.controls.images; }
  protected readonly img = (url: string) => assetUrl(url);

  private open(id: number | null): void {
    this.reset();
    if (id == null) { this.api.categories().subscribe((c) => this.categories.set(c)); return; }
    this.loading.set(true);
    forkJoin({ p: this.api.product(id), c: this.api.categories() }).subscribe({
      next: ({ p, c }) => {
        this.categories.set(c);
        this.form.patchValue({
          name: p.name, categoryId: c.find((x) => x.slug === p.categorySlug)?.id ?? 0, brand: p.brand, description: p.description,
          status: p.status, slug: p.slug, focusKeyword: p.seo?.focusKeyword ?? '', metaTitle: p.seo?.metaTitle ?? '', metaDescription: p.seo?.metaDescription ?? '',
        });
        this.brandQuery.set(p.brand ?? '');
        this.slugTouched.set(true);
        this.variants.clear();
        p.variants.forEach((v) => this.variants.push(this.variantGroup(v)));
        p.images.forEach((i) => this.images.push(this.imageGroup(i.url, i.altText)));
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); this.errors.set('Không tải được sản phẩm'); },
    });
  }

  // ---- combobox thương hiệu ----
  protected onBrandInput(v: string): void {
    this.brandQuery.set(v);
    this.brandOpen.set(true);
    this.brandActive.set(0);
  }

  protected selectBrand(b: Brand): void {
    this.form.controls.brand.setValue(b.name);
    this.brandQuery.set(b.name);
    this.brandOpen.set(false);
  }

  /** Tạo thương hiệu mới ngay trong form: lưu vào bảng cấu hình rồi chọn luôn. */
  protected createBrand(): void {
    const name = this.brandQuery().trim();
    if (!name || this.brandCreating()) return;
    this.brandCreating.set(true);
    this.api.createBrand(name).subscribe({
      next: (b) => {
        this.brands.update((l) => (l.some((x) => x.id === b.id) ? l : [...l, b].sort((x, y) => x.name.localeCompare(y.name, 'vi'))));
        this.selectBrand(b);
        this.brandCreating.set(false);
        this.toast.show(`Đã tạo thương hiệu “${b.name}”`);
      },
      error: (e) => { this.brandCreating.set(false); this.errors.set(e.error?.detail ?? 'Không tạo được thương hiệu'); },
    });
  }

  protected onBrandKey(e: KeyboardEvent): void {
    const total = this.brandMatches().length + (this.brandCanCreate() ? 1 : 0);
    if (e.key === 'ArrowDown') { e.preventDefault(); this.brandOpen.set(true); this.brandActive.set(Math.min(this.brandActive() + 1, total - 1)); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); this.brandActive.set(Math.max(this.brandActive() - 1, 0)); }
    else if (e.key === 'Escape') { this.brandOpen.set(false); }
    else if (e.key === 'Enter') {
      e.preventDefault();
      const m = this.brandMatches()[this.brandActive()];
      if (m) this.selectBrand(m); else if (this.brandCanCreate()) this.createBrand();
    }
  }

  protected onBrandBlur(): void {
    this.brandOpen.set(false);
    const q = this.brandQuery().trim();
    if (!q) { this.form.controls.brand.setValue(''); return; }
    const exact = this.brands().find((b) => b.name.toLowerCase() === q.toLowerCase());
    // chữ gõ dở chưa chọn gì thì trả về thương hiệu đang chọn
    this.brandQuery.set(exact?.name ?? this.form.controls.brand.value);
    if (exact) this.form.controls.brand.setValue(exact.name);
  }

  protected onName(): void {
    // slug tự sinh từ tên (bỏ dấu) cho tới khi người dùng sửa tay
    if (!this.slugTouched()) this.form.controls.slug.setValue(SlugUtil.toSlug(this.form.controls.name.value), { emitEvent: false });
  }

  protected addVariant(): void { this.variants.push(this.variantGroup()); }
  protected removeVariant(i: number): void { if (this.variants.length > 1) this.variants.removeAt(i); }
  protected removeImage(i: number): void { this.images.removeAt(i); }

  protected readonly uploading = signal(false);

  protected moveFirst(i: number): void {
    const g = this.images.at(i);
    this.images.removeAt(i);
    this.images.insert(0, g);
  }

  protected async upload(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = ''; // cho phép chọn lại cùng một file
    if (!files.length) return;
    this.uploading.set(true);
    for (const f of files) {
      try {
        const blob = await compressImage(f);
        const r = await firstValueFrom(this.api.upload(new File([blob], 'image', { type: blob.type })));
        this.images.push(this.imageGroup(r.url, this.form.controls.name.value));
      } catch (e) {
        const detail = (e as { error?: { detail?: string } }).error?.detail;
        this.toast.show(`${f.name}: ${detail ?? 'Không tải được ảnh'}`);
      }
    }
    this.uploading.set(false);
  }

  protected save(status: ProductStatus): void {
    this.errors.set('');
    this.form.controls.status.setValue(status);
    if (this.form.invalid) { this.form.markAllAsTouched(); this.errors.set('Vui lòng nhập tên, danh mục và thông tin biến thể'); return; }
    this.api.saveProduct(this.productId(), this.request()).subscribe({
      next: (p) => { this.toast.show(status === 'DRAFT' ? 'Đã lưu nháp' : 'Đã lưu sản phẩm'); this.saved.emit(p.id); },
      error: (e) => this.errors.set(e.error?.detail ?? 'Không lưu được'),
    });
  }

  private request(): AdminProductRequest {
    const v = this.form.getRawValue();
    return {
      name: v.name, categoryId: v.categoryId, brand: v.brand, description: v.description, status: v.status, slug: v.slug || undefined,
      variants: v.variants.map((x: Record<string, unknown>) => ({ ...x, price: Number(x['price']), originalPrice: Number(x['originalPrice'] || 0), stock: Number(x['stock']) })) as AdminProductRequest['variants'],
      images: v.images as AdminProductRequest['images'],
      specs: [],
      seo: { focusKeyword: v.focusKeyword, metaTitle: v.metaTitle, metaDescription: v.metaDescription },
    };
  }

  private reset(): void {
    this.form.reset({ status: 'ACTIVE', categoryId: 0 });
    this.brandQuery.set('');
    this.brandOpen.set(false);
    this.variants.clear();
    this.variants.push(this.variantGroup());
    this.images.clear();
    this.seo.set(null);
    this.errors.set('');
    this.slugTouched.set(false);
  }

  private imageGroup(url = '', altText = '') {
    return this.fb.nonNullable.group({ url: [url], altText: [altText] });
  }

  private variantGroup(v?: { id?: number; sku: string; color: string | null; storage: string | null; price: number; originalPrice: number; stock: number }) {
    return this.fb.nonNullable.group({
      id: [v?.id as number | undefined],
      sku: [v?.sku ?? '', Validators.required],
      color: [v?.color ?? ''],
      storage: [v?.storage ?? ''],
      price: [v?.price ?? 0, Validators.min(1)],
      originalPrice: [v?.originalPrice ?? 0],
      stock: [v?.stock ?? 0, Validators.min(0)],
    });
  }
}
