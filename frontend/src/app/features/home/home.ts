import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ProductCard } from '../../core/models/catalog.model';
import { CartService } from '../../core/services/cart.service';
import { CatalogService } from '../../core/services/catalog.service';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductCardComponent } from '../../shared/components/product-card';
import { HeroSlider } from '../../shared/components/hero-slider';
import { ProductThumb } from '../../shared/components/product-thumb';
import { kindOf } from '../../shared/utils/product-kind';
import { DatePipe } from '@angular/common';
import { catchError, map, of } from 'rxjs';
import { Banner } from '../../core/models/banner.model';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [HeroSlider, RouterLink, ProductCardComponent, ProductThumb, DatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);

  protected readonly categories = toSignal(this.catalog.categories(), { initialValue: [] });
  protected readonly banners = toSignal(this.catalog.banners().pipe(catchError(() => of([]))), { initialValue: null });
  protected readonly fallbackBanners: Banner[] = [{
    id: 0, title: 'Công nghệ tinh gọn cho nhịp sống xanh', subtitle: 'Máy tính, điện thoại và tai nghe chính hãng được HaQua chọn lọc kỹ — giá minh bạch, giao nhanh, bảo hành tận tâm.',
    imageUrl: '', altText: 'HaQua', linkUrl: '/san-pham', ctaLabel: 'Mua sắm ngay', sortOrder: 0, active: true,
  }];
  /** Sản phẩm nổi bật chỉ lấy máy tính, xếp theo độ phổ biến. */
  protected readonly featured = toSignal(
    this.catalog.products({ category: ['may-tinh'], sort: 'popular' }, 8).pipe(map((r) => r.page.content)),
    { initialValue: [] },
  );
  protected readonly posts = toSignal(this.catalog.posts(3), { initialValue: null });
  protected readonly faqs = toSignal(this.catalog.faq(), { initialValue: [] });
  protected readonly openFaq = signal<number | null>(0);
  protected readonly kindOf = kindOf;

  protected readonly perks = [
    { title: 'Giao nhanh 2 giờ', desc: 'Nội thành Hà Nội và TP.HCM' },
    { title: 'Đổi trả 30 ngày', desc: 'Lỗi do nhà sản xuất đổi mới' },
    { title: 'Bảo hành chính hãng', desc: '12 tháng tại trung tâm ủy quyền' },
    { title: 'Trả góp 0%', desc: 'Qua thẻ tín dụng, kỳ hạn 3–12 tháng' },
  ];

  constructor() {
    inject(SeoService).set(
      'HaQua – Laptop, điện thoại, tai nghe chính hãng',
      'Mua laptop, điện thoại, tai nghe chính hãng tại HaQua: giá minh bạch, giao nhanh 2h, đổi trả 30 ngày, trả góp 0%.',
      '/',
    );
  }

  protected add(p: ProductCard): void {
    if (!p.defaultVariantId) return;
    this.cart.add(p.defaultVariantId).subscribe({
      next: () => this.toast.show(`Đã thêm ${p.name} vào giỏ`, { label: 'Xem giỏ', href: '/gio-hang' }),
      error: (e) => this.toast.show(e.error?.detail ?? 'Không thêm được vào giỏ'),
    });
  }
}
