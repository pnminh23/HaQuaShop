import { Routes } from '@angular/router';
import { adminGuard, authGuard, staffGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ---- cửa hàng (header + footer + chatbot) ----
  {
    path: '',
    loadComponent: () => import('./layout/store-layout').then((m) => m.StoreLayout),
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./features/home/home').then((m) => m.Home) },
      { path: 'san-pham', loadComponent: () => import('./features/catalog/product-list').then((m) => m.ProductList) },
      { path: 'san-pham/:slug', loadComponent: () => import('./features/catalog/product-detail').then((m) => m.ProductDetail) },
      { path: 'gio-hang', loadComponent: () => import('./features/cart/cart-page').then((m) => m.CartPage) },
      { path: 'tra-cuu-don-hang', loadComponent: () => import('./features/orders/order-lookup').then((m) => m.OrderLookup) },
      { path: 'dang-nhap', loadComponent: () => import('./features/auth/auth-pages').then((m) => m.LoginPage) },
      { path: 'dang-ky', loadComponent: () => import('./features/auth/auth-pages').then((m) => m.RegisterPage) },
      { path: 'tai-khoan', canActivate: [authGuard], loadComponent: () => import('./features/account/account-page').then((m) => m.AccountPage) },
    ],
  },

  // ---- thanh toán: khung riêng, tập trung vào việc đặt hàng ----
  { path: 'thanh-toan', loadComponent: () => import('./features/checkout/checkout-page').then((m) => m.CheckoutPage) },

  // ---- quản trị: STAFF xem đơn + chatbot; ADMIN toàn quyền ----
  {
    path: 'admin',
    canActivate: [staffGuard],
    loadComponent: () => import('./layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      { path: '', pathMatch: 'full', canActivate: [adminGuard], loadComponent: () => import('./features/admin/dashboard').then((m) => m.AdminDashboard) },
      { path: 'don-hang', loadComponent: () => import('./features/admin/admin-orders').then((m) => m.AdminOrders) },
      { path: 'don-hang/:id', loadComponent: () => import('./features/admin/admin-order-detail').then((m) => m.AdminOrderDetail) },
      { path: 'san-pham', canActivate: [adminGuard], loadComponent: () => import('./features/admin/admin-products').then((m) => m.AdminProducts) },
      { path: 'san-pham/:id', canActivate: [adminGuard], loadComponent: () => import('./features/admin/admin-product-detail').then((m) => m.AdminProductDetail) },
      { path: 'thuong-hieu', canActivate: [adminGuard], loadComponent: () => import('./features/admin/admin-brands').then((m) => m.AdminBrands) },
      { path: 'banner', canActivate: [adminGuard], loadComponent: () => import('./features/admin/admin-banners').then((m) => m.AdminBanners) },
      { path: 'khuyen-mai', canActivate: [adminGuard], loadComponent: () => import('./features/admin/admin-coupons').then((m) => m.AdminCoupons) },
      { path: 'khach-hang', canActivate: [adminGuard], loadComponent: () => import('./features/admin/admin-customers').then((m) => m.AdminCustomers) },
      { path: 'chatbot', loadComponent: () => import('./features/admin/admin-chatbot').then((m) => m.AdminChatbot) },
    ],
  },

  { path: '**', redirectTo: '' },
];
