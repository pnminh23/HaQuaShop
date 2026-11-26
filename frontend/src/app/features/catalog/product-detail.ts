import { ChangeDetectionStrategy, Component, Signal, computed, effect, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, catchError, filter, map, of, switchMap } from 'rxjs';
import { ProductCard, ProductDetail as Detail, ReviewList } from '../../core/models/catalog.model';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { CatalogService } from '../../core/services/catalog.service';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductCardComponent } from '../../shared/components/product-card';
import { ProductThumb } from '../../shared/components/product-thumb';
import { StarRating } from '../../shared/components/star-rating';
import { VndPipe } from '../../shared/pipes/vnd.pipe';
import { kindOf, toneOf } from '../../shared/utils/product-kind';
import { DatePipe } from '@angular/common';
import { assetUrl } from '../../core/config/api.config';

type Tab = 'desc' | 'specs' | 'reviews';
type Loaded = { product: Detail } | { notFound: true } | null;

/** UC05 – chi tiết sản phẩm, chọn biến thể; UC10 – đánh giá. */
@Component({
  selector: 'app-product-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, VndPipe, ProductThumb, StarRating, ProductCardComponent, DatePipe],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly seo = inject(SeoService);
  protected readonly auth = inject(AuthService);

  protected readonly data = toSignal<Loaded>(
    this.route.paramMap.pipe(
      map((p) => p.get('slug') ?? ''),
      switchMap((slug) =>
        this.catalog.detail(slug).pipe(
          map((product): Loaded => ({ product })),
          catchError(() => of<Loaded>({ notFound: true })),
        ),
      ),
    ),
    { initialValue: null },
  );

  protected readonly product = computed(() => {
    const d = this.data();
    return d && 'product' in d ? d.product : null;
  });
  protected readonly notFound = computed(() => !!this.data() && 'notFound' in this.data()!);

  // --- lựa chọn của khách ---
  protected readonly imageIndex = signal(0);
  protected readonly color = signal<string | null>(null);
  protected readonly storage = signal<string | null>(null);
  protected readonly qty = signal(1);
  protected readonly tab = signal<Tab>('desc');
  protected readonly added = signal(false);
  protected readonly review = { stars: 5, content: '' };
  protected readonly reviewError = signal('');

  protected readonly colors = computed(() => [...new Set((this.product()?.variants ?? []).map((v) => v.color).filter((c): c is string => !!c))]);
  protected readonly storages = computed(() => [...new Set((this.product()?.variants ?? []).map((v) => v.storage).filter((s): s is string => !!s))]);

  /** Biến thể đang chọn theo (màu, dung lượng); giá, giá gốc, mã SP và trả góp đều lấy từ đây. */
  protected readonly variant = computed(() => {
    const vs = this.product()?.variants ?? [];
    return vs.find((v) => v.color === this.color() && v.storage === this.storage()) ?? vs.find((v) => v.storage === this.storage()) ?? vs[0] ?? null;
  });
  protected readonly monthly = computed(() => Math.round((this.variant()?.price ?? 0) / 12 / 1000) * 1000);
  protected readonly kind = computed(() => kindOf(this.product()?.categorySlug ?? ''));
  protected readonly tone = computed(() => toneOf(this.product()?.id ?? 0));
  protected readonly storagePrice = (s: string) =>
    this.product()?.variants.filter((v) => v.storage === s).reduce((min, v) => Math.min(min, v.price), Infinity) ?? 0;

  protected readonly reviews = toSignal<ReviewList | null>(
    toObservableId(this.data, (id) => this.catalog.reviews(id)),
    { initialValue: null },
  );
  protected readonly stars = [5, 4, 3, 2, 1];

  constructor() {
    effect(() => {
      const p = this.product();
      if (!p) return;
      this.color.set(p.variants[0]?.color ?? null);
      this.storage.set(p.variants[0]?.storage ?? null);
      this.imageIndex.set(0);
      this.qty.set(1);
      this.applySeo(p);
    });
  }

  protected image(i: number): string | null {
    return assetUrl(this.product()?.images[i]?.url);
  }

  protected inc(): void {
    this.qty.update((q) => Math.min(Math.min(10, this.variant()?.stock ?? 10), q + 1));
  }

  protected dec(): void {
    this.qty.update((q) => Math.max(1, q - 1));
  }

  protected addToCart(thenCheckout = false): void {
    const v = this.variant();
    if (!v) return;
    this.cart.add(v.id, this.qty()).subscribe({
      next: () => {
        if (thenCheckout) {
          void this.router.navigateByUrl('/thanh-toan');
          return;
        }
        this.added.set(true);
        setTimeout(() => this.added.set(false), 2200);
        this.toast.show('Đã thêm vào giỏ', { label: 'Xem giỏ', href: '/gio-hang' });
      },
      error: (e) => this.toast.show(e.error?.detail ?? 'Không thêm được vào giỏ'),
    });
  }

  protected addRelated(p: ProductCard): void {
    if (p.defaultVariantId) this.cart.add(p.defaultVariantId).subscribe(() => this.toast.show(`Đã thêm ${p.name} vào giỏ`, { label: 'Xem giỏ', href: '/gio-hang' }));
  }

  protected submitReview(): void {
    const p = this.product();
    if (!p) return;
    this.reviewError.set('');
    this.catalog.writeReview(p.id, this.review.stars, this.review.content).subscribe({
      next: () => {
        this.toast.show('Cảm ơn bạn đã đánh giá!');
        this.review.content = '';
        this.catalog.reviews(p.id).subscribe((r) => (this.reviewsOverride.set(r)));
      },
      error: (e) => this.reviewError.set(e.error?.detail ?? 'Không gửi được đánh giá'),
    });
  }

  protected readonly reviewsOverride = signal<ReviewList | null>(null);
  protected readonly reviewData = computed(() => this.reviewsOverride() ?? this.reviews());

  private applySeo(p: Detail): void {
    this.seo.set(p.seo?.metaTitle || `${p.name} | HaQua`, p.seo?.metaDescription || p.description, `/san-pham/${p.slug}`);
    const v = p.variants[0];
    this.seo.setJsonLd([
      {
        '@context': 'https://schema.org',
        '@type': 'Product',
        name: p.name,
        description: p.description,
        brand: { '@type': 'Brand', name: p.brand },
        sku: v?.sku,
        aggregateRating: p.reviewCount ? { '@type': 'AggregateRating', ratingValue: p.rating, reviewCount: p.reviewCount } : undefined,
        offers: {
          '@type': 'Offer',
          priceCurrency: 'VND',
          price: v?.price,
          availability: p.status === 'OUT_OF_STOCK' ? 'https://schema.org/OutOfStock' : 'https://schema.org/InStock',
        },
      },
      {
        '@context': 'https://schema.org',
        '@type': 'BreadcrumbList',
        itemListElement: [
          { '@type': 'ListItem', position: 1, name: 'Trang chủ', item: '/' },
          { '@type': 'ListItem', position: 2, name: p.categoryName, item: `/san-pham?category=${p.categorySlug}` },
          { '@type': 'ListItem', position: 3, name: p.name },
        ],
      },
    ]);
  }
}

/** Gọi API theo id sản phẩm mỗi khi dữ liệu chi tiết đổi. */
function toObservableId<T>(data: Signal<Loaded>, call: (id: number) => Observable<T>): Observable<T> {
  return toObservable(data).pipe(
    filter((d): d is { product: Detail } => !!d && 'product' in d),
    switchMap((d) => call(d.product.id)),
  );
}
