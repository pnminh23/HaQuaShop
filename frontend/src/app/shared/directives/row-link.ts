import { Directive, HostListener, inject, input } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Quy tắc chung cho mọi bảng quản trị: nhấp đúp vào hàng → sang trang chi tiết.
 * Nhấp đúp trên nút/liên kết/ô nhập bên trong hàng không kích hoạt (để nút Sửa… vẫn dùng popup như cũ).
 * Dùng: `<tr [rowLink]="['/admin/san-pham', r.id]">`.
 */
@Directive({
  selector: 'tr[rowLink]',
  host: { class: 'row-link', title: 'Nhấp đúp để xem chi tiết' },
})
export class RowLink {
  readonly rowLink = input.required<string | unknown[]>();
  private readonly router = inject(Router);

  @HostListener('dblclick', ['$event'])
  protected onDblClick(e: MouseEvent): void {
    if ((e.target as HTMLElement).closest('button, a, input, select, textarea, label')) return;
    window.getSelection()?.removeAllRanges(); // nhấp đúp hay bôi đen chữ
    const link = this.rowLink();
    void this.router.navigate(typeof link === 'string' ? [link] : link);
  }
}
