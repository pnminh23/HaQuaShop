import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map, switchMap, tap } from 'rxjs';
import { ProductCard, ProductFilter, ProductListResponse } from '../../core/models/catalog.model';
import { CartService } from '../../core/services/cart.service';
import { CatalogService } from '../../core/services/catalog.service';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductCardComponent } from '../../shared/components/product-card';

const PRICE_RANGES: Record<string, { label: string; min?: number; max?: number }> = {
  any: { label: 'Tất cả mức giá' },
  u5: { label: 'Dưới 5 triệu', max: 5_000_000 },
  m5: { label: '5 – 15 triệu', min: 5_000_000, max: 15_000_000 },
  m15: { label: '15 – 25 triệu', min: 15_000_000, max: 25_000_000 },
  o25: { label: 'Trên 25 triệu', min: 25_000_000 },
};

const SORTS = [
  { value: 'popular', label: 'Phổ biến nhất' },
  { value: 'priceAsc', label: 'Giá thấp → cao' },
  { value: 'priceDesc', label: 'Giá cao → thấp' },
  { value: 'rating', label: 'Đánh giá cao' },
];

/** UC03 + UC04: mọi bộ lọc nằm trên URL (query params) để chia sẻ được. */
@Component({
  selector: 'app-product-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ProductCardComponent, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);

  protected readonly sorts = SORTS;
  protected readonly skeleton = Array.from({ length: 8 }, (_, i) => i);
  protected readonly priceKeys = Object.keys(PRICE_RANGES);
  protected readonly priceLabel = (k: string) => PRICE_RANGES[k].label;

  private readonly params = toSignal(this.route.queryParamMap);

  /** Bộ lọc hiện tại, đọc từ URL. */
  protected readonly state = computed(() => {
    const p = this.params();
    return {
      q: p?.get('q') ?? '',
      categories: p?.getAll('category') ?? [],
      price: p?.get('price') ?? 'any',
      top: p?.get('top') === '1',
      sale: p?.get('onSale') === 'true' || p?.get('sale') === '1',
      sort: p?.get('sort') ?? 'popular',
      page: Number(p?.get('page') ?? 0),
    };
  });

  protected readonly loading = signal(false);
  protected readonly version = signal(0);

  protected readonly result = toSignal<ProductListResponse | null>(
    this.route.queryParamMap.pipe(
      map(() => this.toFilter()),
      tap(() => this.loading.set(true)),
      switchMap((f) => this.catalog.products(f)),
      tap(() => { this.version.update((v) => v + 1); this.loading.set(false); }),
    ),
    { initialValue: null },
  );

  protected readonly categoryOptions = toSignal(this.catalog.categories(), { initialValue: [] });
  protected readonly list = computed(() => this.result()?.page.content ?? []);
  protected readonly facets = computed(() => this.result()?.categoryFacets ?? {});
  protected readonly pages = computed(() => Array.from({ length: this.result()?.page.totalPages ?? 0 }, (_, i) => i));

  protected readonly chips = computed(() => {
    const s = this.state();
    const chips: { label: string; remove: Record<string, unknown> }[] = [];
    s.categories.forEach((c) =>
      chips.push({ label: this.categoryOptions().find((o) => o.slug === c)?.name ?? c, remove: { category: s.categories.filter((x) => x !== c) } }),
    );
    if (s.price !== 'any') chips.push({ label: PRICE_RANGES[s.price]?.label ?? '', remove: { price: null } });
    if (s.top) chips.push({ label: 'Từ 4,7 sao', remove: { top: null } });
    if (s.sale) chips.push({ label: 'Đang giảm giá', remove: { onSale: null, sale: null } });
    if (s.q) chips.push({ label: `Từ khóa: ${s.q}`, remove: { q: null } });
    return chips;
  });

  constructor() {
    inject(SeoService).set(
      'Tất cả sản phẩm – Laptop, điện thoại, tai nghe | HaQua',
      'Lọc và so sánh laptop, điện thoại, tai nghe chính hãng theo giá, đánh giá. Giao nhanh 2h, đổi trả 30 ngày.',
      '/san-pham',
    );
  }

  protected setParams(patch: Record<string, unknown>): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { page: null, ...patch }, queryParamsHandling: 'merge' });
  }

  protected toggleCategory(slug: string): void {
    const cur = this.state().categories;
    this.setParams({ category: cur.includes(slug) ? cur.filter((c) => c !== slug) : [...cur, slug] });
  }

  protected clearAll(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  protected goto(page: number): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { page: page || null }, queryParamsHandling: 'merge' });
  }

  protected add(p: ProductCard): void {
    if (!p.defaultVariantId) return;
    this.cart.add(p.defaultVariantId).subscribe({
      next: () => this.toast.show(`Đã thêm ${p.name} vào giỏ`, { label: 'Xem giỏ', href: '/gio-hang' }),
      error: (e) => this.toast.show(e.error?.detail ?? 'Không thêm được vào giỏ'),
    });
  }

  private toFilter(): ProductFilter {
    const s = this.state();
    const range = PRICE_RANGES[s.price] ?? PRICE_RANGES['any'];
    return {
      q: s.q || undefined,
      category: s.categories,
      priceMin: range.min,
      priceMax: range.max,
      minRating: s.top ? 4.7 : undefined,
      onSale: s.sale || undefined,
      sort: s.sort,
      page: s.page,
    };
  }
}
