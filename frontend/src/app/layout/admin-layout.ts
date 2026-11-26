import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

interface NavItem { label: string; link: string; adminOnly?: boolean }

@Component({
  selector: 'app-admin-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  protected readonly auth = inject(AuthService);
  /** Sidebar thu gọn thành thanh icon; nhớ lựa chọn giữa các lần mở (localStorage có thể không dùng được → bỏ qua). */
  protected readonly collapsed = signal(AdminLayout.readCollapsed());

  protected toggle(): void {
    const next = !this.collapsed();
    this.collapsed.set(next);
    try { localStorage.setItem('admin.sidebar.collapsed', next ? '1' : '0'); } catch { /* không lưu được thì thôi */ }
  }

  private static readCollapsed(): boolean {
    try { return typeof localStorage !== 'undefined' && localStorage.getItem('admin.sidebar.collapsed') === '1'; } catch { return false; }
  }

  private readonly groups: { title: string; items: NavItem[] }[] = [
    { title: 'Bán hàng', items: [
      { label: 'Tổng quan', link: '/admin', adminOnly: true },
      { label: 'Đơn hàng', link: '/admin/don-hang' },
      { label: 'Sản phẩm & SEO', link: '/admin/san-pham', adminOnly: true },
      { label: 'Thương hiệu', link: '/admin/thuong-hieu', adminOnly: true },
      { label: 'Banner trang chủ', link: '/admin/banner', adminOnly: true },
      { label: 'Khuyến mãi', link: '/admin/khuyen-mai', adminOnly: true },
      { label: 'Khách hàng', link: '/admin/khach-hang', adminOnly: true },
    ] },
    { title: 'Trợ lý', items: [{ label: 'Chatbot AI', link: '/admin/chatbot' }] },
  ];

  /** Chữ cái đầu hiển thị khi sidebar thu gọn. */
  protected readonly initial = (label: string) => label.trim().charAt(0).toUpperCase();

  protected readonly visibleGroups = computed(() =>
    this.groups
      .map((g) => ({ ...g, items: g.items.filter((i) => !i.adminOnly || this.auth.isAdmin()) }))
      .filter((g) => g.items.length),
  );
}
