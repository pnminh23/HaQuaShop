import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, signal, untracked } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subject, catchError, filter, map, of, startWith, switchMap, timer } from 'rxjs';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../core/services/auth.service';
import { CartService } from '../core/services/cart.service';
import { CatalogService, Suggestion } from '../core/services/catalog.service';
import { assetUrl } from '../core/config/api.config';
import { VndPipe } from '../shared/pipes/vnd.pipe';

@Component({
  selector: 'app-site-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, VndPipe],
  templateUrl: './site-header.html',
  styleUrl: './site-header.scss',
  host: { '(document:pointerdown)': 'outside($event)' },
})
export class SiteHeader {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
  private readonly catalog = inject(CatalogService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly bump = signal(false);
  protected readonly query = signal('');
  /** Khung kết quả nhanh: `null` = đóng; `items: null` = đang tìm. */
  protected readonly quick = signal<{ items: Suggestion[] | null; failed?: boolean } | null>(null);
  /** Dòng đang chọn bằng phím ↑ ↓; -1 = chưa chọn. */
  protected readonly active = signal(-1);
  /** Từ khóa đang chờ debounce; `null` = đóng khung, hủy lượt chờ. */
  private readonly typed$ = new Subject<string | null>();
  protected readonly asset = assetUrl;

  /** Menu chính: các hãng máy tính (cấu hình tại Quản trị → Thương hiệu). */
  protected readonly computerBrands = toSignal(this.catalog.computerBrands().pipe(catchError(() => of([]))), { initialValue: [] });
  constructor() {
    // UC03: khách ngừng gõ 300 ms thì hiện tối đa 6 kết quả ngay dưới ô tìm kiếm, không cần Enter
    this.typed$
      .pipe(
        switchMap((q) =>
          q === null || q.length < 2
            ? of(null)
            : timer(300).pipe(
                switchMap(() =>
                  this.catalog.suggest(q).pipe(
                    map((items) => ({ items, failed: false })),
                    // bắt lỗi ở đây để một lần gọi hỏng không làm chết cả luồng gõ
                    catchError(() => of({ items: [] as Suggestion[], failed: true })),
                    startWith({ items: null, failed: false }),
                  ),
                ),
              ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((state) => {
        this.active.set(-1);
        this.quick.set(state);
      });

    // ô tìm kiếm bám theo URL: bấm menu, xóa chip lọc, back/forward…
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => {
        this.close();
        this.syncFromUrl();
      });
    this.syncFromUrl();

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
    if (value === this.query()) return; // bộ gõ chốt chữ có thể phát lại cùng giá trị
    this.query.set(value);
    this.typed$.next(value.trim());
  }

  /** Bấm lại vào ô đang có chữ thì mở lại khung kết quả. */
  protected reopen(): void {
    if (!this.quick()) this.typed$.next(this.query().trim());
  }

  /** Enter / nút Tìm kiếm: xem đầy đủ kết quả ở trang sản phẩm. */
  protected search(): void {
    const picked = this.quick()?.items?.[this.active()];
    if (picked) {
      this.pick(picked.slug);
      return;
    }
    const q = this.query().trim();
    this.close();
    if (q) void this.router.navigate(['/san-pham'], { queryParams: { q } });
  }

  protected pick(slug: string): void {
    this.close();
    void this.router.navigate(['/san-pham', slug]);
  }

  protected move(delta: number, event: Event): void {
    const n = this.quick()?.items?.length ?? 0;
    if (!n) return;
    event.preventDefault();
    this.active.update((i) => (i < 0 ? (delta > 0 ? 0 : n - 1) : (i + delta + n) % n));
  }

  protected close(): void {
    this.typed$.next(null);
  }

  protected outside(event: Event): void {
    const form = this.host.nativeElement.querySelector('form.search');
    if (form && !form.contains(event.target as Node)) this.close();
  }

  private syncFromUrl(): void {
    const onList = this.router.url.split(/[?#]/)[0] === '/san-pham';
    this.query.set(onList ? String(this.router.parseUrl(this.router.url).queryParams['q'] ?? '') : '');
  }
}
