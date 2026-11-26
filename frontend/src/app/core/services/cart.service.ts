import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config/api.config';
import { Cart } from '../models/cart.model';
import { CART_TOKEN } from './auth.service';
import { StorageService } from './storage.service';

const EMPTY: Cart = {
  cartToken: null,
  items: [],
  summary: { subtotal: 0, productDiscount: 0, couponCode: null, couponDiscount: 0, shippingFee: 0, total: 0, selectedCount: 0 },
  itemCount: 0,
  notice: null,
};

/** Trạng thái giỏ hàng dùng chung (badge trên header, trang giỏ, thanh toán). Giá luôn do server tính. */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(StorageService);

  private readonly _cart = signal<Cart>(EMPTY);
  readonly cart = this._cart.asReadonly();
  readonly count = computed(() => this._cart().itemCount);

  load(): Observable<Cart> {
    return this.http.get<Cart>(`${API_URL}/cart`).pipe(tap((c) => this.apply(c)));
  }

  add(variantId: number, qty = 1): Observable<Cart> {
    return this.http.post<Cart>(`${API_URL}/cart/items`, { variantId, qty }).pipe(tap((c) => this.apply(c)));
  }

  /** Thêm từ khung chat AI để đơn được ghi kênh Chatbot. */
  addFromChat(variantId: number, qty = 1): Observable<Cart> {
    return this.http.post<Cart>(`${API_URL}/chat/cart`, { variantId, qty }).pipe(tap((c) => this.apply(c)));
  }

  update(itemId: number, patch: { qty?: number; selected?: boolean; savedForLater?: boolean }): Observable<Cart> {
    return this.http.patch<Cart>(`${API_URL}/cart/items/${itemId}`, patch).pipe(tap((c) => this.apply(c)));
  }

  remove(itemId: number): Observable<Cart> {
    return this.http.delete<Cart>(`${API_URL}/cart/items/${itemId}`).pipe(tap((c) => this.apply(c)));
  }

  removeSelected(): Observable<Cart> {
    return this.http.delete<Cart>(`${API_URL}/cart/items`).pipe(tap((c) => this.apply(c)));
  }

  applyCoupon(code: string): Observable<Cart> {
    return this.http.post<Cart>(`${API_URL}/cart/coupon`, { code }).pipe(tap((c) => this.apply(c)));
  }

  removeCoupon(): Observable<Cart> {
    return this.http.delete<Cart>(`${API_URL}/cart/coupon`).pipe(tap((c) => this.apply(c)));
  }

  reset(): void {
    this._cart.set(EMPTY);
  }

  private apply(cart: Cart): void {
    if (cart.cartToken) this.storage.set(CART_TOKEN, cart.cartToken);
    this._cart.set(cart);
  }
}
