import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * SEO: trang công khai (trang chủ, danh sách, chi tiết sản phẩm) render phía server để bot Google đọc được nội dung.
 * Trang theo phiên người dùng (giỏ, thanh toán, tài khoản, quản trị) chỉ render ở trình duyệt.
 */
export const serverRoutes: ServerRoute[] = [
  { path: 'gio-hang', renderMode: RenderMode.Client },
  { path: 'thanh-toan', renderMode: RenderMode.Client },
  { path: 'tai-khoan', renderMode: RenderMode.Client },
  { path: 'tra-cuu-don-hang', renderMode: RenderMode.Client },
  { path: 'dang-nhap', renderMode: RenderMode.Client },
  { path: 'dang-ky', renderMode: RenderMode.Client },
  { path: 'admin/**', renderMode: RenderMode.Client },
  { path: '**', renderMode: RenderMode.Server },
];
