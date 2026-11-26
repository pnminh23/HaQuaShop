import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { API_URL } from '../config/api.config';
import { AuthService, CART_TOKEN } from '../services/auth.service';
import { StorageService } from '../services/storage.service';

const NEEDS_CART_TOKEN = ['/cart', '/checkout', '/chat/cart'];

/** Gắn Bearer token + X-Cart-Token (khách vãng lai); tự làm mới token khi gặp 401. */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API_URL)) return next(req);

  const auth = inject(AuthService);
  const storage = inject(StorageService);
  const isAuthCall = req.url.includes('/auth/');

  const withHeaders = (token: string | null) => {
    let headers = req.headers;
    if (token && !isAuthCall) headers = headers.set('Authorization', `Bearer ${token}`);
    const cartToken = storage.get(CART_TOKEN);
    if (cartToken && NEEDS_CART_TOKEN.some((p) => req.url.includes(p))) headers = headers.set('X-Cart-Token', cartToken);
    return req.clone({ headers });
  };

  return next(withHeaders(auth.accessToken)).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !isAuthCall && auth.refreshToken) {
        return auth.refresh().pipe(
          switchMap((t) => next(withHeaders(t.accessToken))),
          catchError(() => {
            // phiên hết hạn hẳn: dọn token rồi gọi lại như khách. Trang công khai vẫn dùng được,
            // trang cần đăng nhập sẽ trả 401 và guard chuyển sang /dang-nhap.
            auth.logout(false);
            return next(withHeaders(null));
          }),
        );
      }
      return throwError(() => err);
    }),
  );
};
