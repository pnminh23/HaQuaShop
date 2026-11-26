import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config/api.config';
import { CheckoutRequest, CheckoutResponse } from '../models/cart.model';
import { OrderTracking } from '../models/order.model';
import { CartService } from './cart.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly cart = inject(CartService);

  checkout(req: CheckoutRequest): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${API_URL}/checkout`, req).pipe(tap(() => this.cart.load().subscribe()));
  }

  lookup(code: string, phone: string): Observable<OrderTracking> {
    return this.http.get<OrderTracking>(`${API_URL}/orders/lookup`, { params: { code, phone } });
  }

  cancel(code: string, phone: string): Observable<OrderTracking> {
    return this.http.post<OrderTracking>(`${API_URL}/orders/cancel`, { code, phone });
  }

  mine(): Observable<OrderTracking[]> {
    return this.http.get<OrderTracking[]>(`${API_URL}/orders/mine`);
  }
}
