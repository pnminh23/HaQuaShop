import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Yêu cầu đăng nhập; chưa đăng nhập thì chuyển sang /dang-nhap và nhớ trang đích. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/dang-nhap'], { queryParams: { returnUrl: state.url } });
};

/** Chỉ nhân viên (STAFF) hoặc quản trị viên (ADMIN). */
export const staffGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return router.createUrlTree(['/dang-nhap'], { queryParams: { returnUrl: state.url } });
  return auth.isStaff() ? true : router.createUrlTree(['/']);
};

/** Chỉ quản trị viên. Nhân viên CSKH bị đưa về mục Đơn hàng. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAdmin() ? true : inject(Router).createUrlTree(['/admin/don-hang']);
};
