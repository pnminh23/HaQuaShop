import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../core/services/auth.service';
import { CartService } from '../core/services/cart.service';
import { CatalogService } from '../core/services/catalog.service';
import { VndPipe } from '../shared/pipes/vnd.pipe';

@Component({
  selector: 'app-site-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, FormsModule, VndPipe],
  templateUrl: './site-header.html',
  styleUrl: './site-header.scss',
})
export class SiteHeader {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
  private readonly catalog = inject(CatalogService);
  private readonly router = inject(Router);

  protected readonly bump = signal(false);
  protected readonly query = signal('');
  protected readonly suggestions = signal<{ slug: string; name: string; price: number }[]>([]);
  private readonly typed$ = new Subject<string>();

  protected readonly nav = [
    { label: 'Trang chủ', link: '/', exact: true, query: {} },
    { label: 'Tất cả sản phẩm', link: '/san-pham', exact: true, query: {} },
    { label: 'Máy tính', link: '/san-pham', exact: false, query: { category: 'may-tinh' } },
    { label: 'Điện thoại', link: '/san-pham', exact: false, query: { category: 'dien-thoai' } },
    { label: 'Tai nghe', link: '/san-pham', exact: false, query: { category: 'tai-nghe' } },
    { label: 'Khuyến mãi', link: '/san-pham', exact: false, query: { onSale: true } },
  ];

  constructor() {
    // UC03: sau 300 ms khách ngừng gõ mới gọi gợi ý (tối đa 6)
    this.typed$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => (q.trim().length >= 2 ? this.catalog.suggest(q) : of([]))),
        takeUntilDestroyed(),
      )
      .subscribe((list) => this.suggestions.set(list));
    this.cart.load().subscribe({ error: () => undefined });

    // số trên biểu tượng giỏ nảy lên mỗi khi thay đổi
    let previous = this.cart.count();
    effect(() => {
      const now = this.cart.count();
      if (now === previous) return;
      previous = now;
      untracked(() => {
        this.bump.set(true);
        setTimeout(() => this.bump.set(false), 450);
      });
    });
  }

  protected onType(value: string): void {
    this.query.set(value);
    this.typed$.next(value);
  }

  protected search(): void {
    const q = this.query().trim();
    this.suggestions.set([]);
    if (q) void this.router.navigate(['/san-pham'], { queryParams: { q } });
  }

  protected pick(slug: string): void {
    this.suggestions.set([]);
    this.query.set('');
    void this.router.navigate(['/san-pham', slug]);
  }
}
