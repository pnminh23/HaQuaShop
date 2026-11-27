import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { CatalogService } from '../core/services/catalog.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-site-footer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <footer class="footer">
      <div class="inner cols">
        <div>
          <strong class="brand">HaQua</strong>
          <p>Thiết bị điện tử chính hãng: laptop, điện thoại, tai nghe. Giá minh bạch, giao nhanh, bảo hành tận tâm.</p>
        </div>
        <nav aria-label="Mua sắm">
          <h2>Mua sắm</h2>
          @for (c of categories(); track c.id) {
            <a routerLink="/san-pham" [queryParams]="{ category: c.slug }">{{ c.name }}</a>
          } @empty {
            <a routerLink="/san-pham">Tất cả sản phẩm</a>
          }
        </nav>
        <nav aria-label="Hỗ trợ">
          <h2>Hỗ trợ</h2>
          <a routerLink="/tra-cuu-don-hang">Tra cứu đơn hàng</a>
          <a routerLink="/">Chính sách đổi trả 30 ngày</a>
          <a routerLink="/">Bảo hành chính hãng 12 tháng</a>
        </nav>
        <div>
          <h2>Liên hệ</h2>
          <p>Hotline: [SỐ HOTLINE]</p>
          <p>Email: [EMAIL HỖ TRỢ]</p>
          <p>Địa chỉ: [ĐỊA CHỈ CỬA HÀNG]</p>
        </div>
      </div>
      <div class="inner copy">© 2026 HaQua. Mọi quyền được bảo lưu.</div>
    </footer>
  `,
  styles: `
    .footer { background: var(--green-700); color: var(--green-50); margin-top: 48px; padding: 48px 0 24px; }
    .inner { max-width: 1560px; margin: 0 auto; padding: 0 24px; }
    .cols { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 32px; }
    .brand { font-size: 26px; display: block; margin-bottom: 12px; }
    h2 { font-size: 15px; margin-bottom: 12px; }
    p { font-size: 14px; line-height: 1.7; color: #cfdcc9; }
    nav { display: flex; flex-direction: column; gap: 8px; }
    a { color: #cfdcc9; text-decoration: none; font-size: 14px; &:hover { color: #fff; } }
    .copy { margin-top: 32px; padding-top: 20px; border-top: 1px solid rgba(255,255,255,.15); font-size: 13px; color: #a9bfa0; }
  `,
})
export class SiteFooter {
  /** Chỉ gồm danh mục đang hiện trên cửa hàng. */
  protected readonly categories = toSignal(inject(CatalogService).categories().pipe(catchError(() => of([]))), { initialValue: [] });
}
