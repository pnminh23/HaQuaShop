import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { SeoService } from '../../core/services/seo.service';
import { ProductEditor } from './product-editor';

/** Trang chi tiết sản phẩm (mở bằng nhấp đúp hàng ở bảng). Nút Lưu / Quay lại ghim ở đáy màn hình. */
@Component({
  selector: 'app-admin-product-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ProductEditor],
  styleUrl: './admin.scss',
  template: `
    <nav class="breadcrumb" aria-label="Đường dẫn"><ol><li><a routerLink="/admin/san-pham">Sản phẩm</a></li><li aria-hidden="true">/</li><li aria-current="page">Chi tiết sản phẩm #{{ id() }}</li></ol></nav>
    <div class="page-head"><div><h1>Chi tiết sản phẩm</h1><p>Chỉnh sửa thông tin, biến thể, hình ảnh và SEO</p></div></div>
    <div class="card"><app-product-editor mode="page" [productId]="id()" (closed)="back()" /></div>
  `,
})
export class AdminProductDetail {
  private readonly router = inject(Router);
  protected readonly id = toSignal(inject(ActivatedRoute).paramMap.pipe(map((p) => Number(p.get('id')) || null)), { initialValue: null });

  constructor() { inject(SeoService).set('Quản trị · Chi tiết sản phẩm | HaQua'); }

  protected back(): void { void this.router.navigate(['/admin/san-pham']); }
}
