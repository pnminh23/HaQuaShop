import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import {
  AdminProductRequest,
  AdminProductRow,
  Brand,
  ChatbotConfig,
  ChatStats,
  ConversationDetail,
  ConversationRow,
  Coupon,
  CustomerRow,
  Dashboard,
  SeoReport,
} from '../models/admin.model';
import { Category, ProductDetail } from '../models/catalog.model';
import { Banner, BannerRequest } from '../models/banner.model';
import { Page } from '../models/common.model';
import { OrderDetail, OrderRow } from '../models/order.model';

/** Tất cả API trang quản trị gom một chỗ để component không rải URL. */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_URL}/admin`;

  dashboard(range: number): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.base}/dashboard`, { params: { range } });
  }

  exportDashboard(range: number): Observable<Blob> {
    return this.http.get(`${this.base}/dashboard/export`, { params: { range }, responseType: 'blob' });
  }

  // --- sản phẩm & SEO ---
  products(status: string | null, categoryId: number | null, q: string, page: number): Observable<Page<AdminProductRow>> {
    let params = new HttpParams().set('page', page).set('size', 10);
    if (status) params = params.set('status', status);
    if (categoryId) params = params.set('categoryId', categoryId);
    if (q) params = params.set('q', q);
    return this.http.get<Page<AdminProductRow>>(`${this.base}/products`, { params });
  }

  product(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.base}/products/${id}`);
  }

  saveProduct(id: number | null, body: AdminProductRequest): Observable<ProductDetail> {
    return id ? this.http.put<ProductDetail>(`${this.base}/products/${id}`, body) : this.http.post<ProductDetail>(`${this.base}/products`, body);
  }

  hideProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/products/${id}`);
  }

  showProduct(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/products/${id}/show`, {});
  }

  evaluateSeo(body: AdminProductRequest): Observable<SeoReport> {
    return this.http.post<SeoReport>(`${this.base}/seo/evaluate`, body);
  }

  upload(file: File): Observable<{ url: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ url: string }>(`${this.base}/uploads`, form);
  }

  // --- banner trang chủ ---
  banners(): Observable<Banner[]> {
    return this.http.get<Banner[]>(`${this.base}/banners`);
  }

  saveBanner(id: number | null, body: BannerRequest): Observable<Banner> {
    return id ? this.http.put<Banner>(`${this.base}/banners/${id}`, body) : this.http.post<Banner>(`${this.base}/banners`, body);
  }

  deleteBanner(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/banners/${id}`);
  }

  reorderBanners(ids: number[]): Observable<Banner[]> {
    return this.http.put<Banner[]>(`${this.base}/banners/order`, { ids });
  }

  brands(): Observable<Brand[]> {
    return this.http.get<Brand[]>(`${this.base}/brands`);
  }

  renameBrand(id: number, name: string): Observable<Brand> {
    return this.http.put<Brand>(`${this.base}/brands/${id}`, { name });
  }

  deleteBrand(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/brands/${id}`);
  }

  createBrand(name: string): Observable<Brand> {
    return this.http.post<Brand>(`${this.base}/brands`, { name });
  }

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${API_URL}/categories`);
  }

  // --- đơn hàng ---
  orders(tab: string, q: string, page: number): Observable<Page<OrderRow>> {
    return this.http.get<Page<OrderRow>>(`${this.base}/orders`, { params: { tab, q, page, size: 10 } });
  }

  order(id: number): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${this.base}/orders/${id}`);
  }

  advanceOrder(id: number): Observable<OrderDetail> {
    return this.http.post<OrderDetail>(`${this.base}/orders/${id}/advance`, {});
  }

  cancelOrder(id: number, reason: string): Observable<OrderDetail> {
    return this.http.post<OrderDetail>(`${this.base}/orders/${id}/cancel`, { reason });
  }

  // --- chatbot ---
  chatStats(): Observable<ChatStats> {
    return this.http.get<ChatStats>(`${this.base}/chatbot/stats`);
  }

  conversations(): Observable<ConversationRow[]> {
    return this.http.get<ConversationRow[]>(`${this.base}/chatbot/conversations`);
  }

  conversation(id: number): Observable<ConversationDetail> {
    return this.http.get<ConversationDetail>(`${this.base}/chatbot/conversations/${id}`);
  }

  conversationAction(id: number, action: 'takeover' | 'give-back' | 'close'): Observable<ConversationDetail> {
    return this.http.post<ConversationDetail>(`${this.base}/chatbot/conversations/${id}/${action}`, {});
  }

  staffReply(id: number, content: string): Observable<ConversationDetail> {
    return this.http.post<ConversationDetail>(`${this.base}/chatbot/conversations/${id}/messages`, { content });
  }

  chatbotConfig(): Observable<ChatbotConfig> {
    return this.http.get<ChatbotConfig>(`${this.base}/chatbot/config`);
  }

  saveChatbotConfig(cfg: ChatbotConfig): Observable<ChatbotConfig> {
    return this.http.put<ChatbotConfig>(`${this.base}/chatbot/config`, cfg);
  }

  // --- khuyến mãi, khách hàng ---
  coupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(`${this.base}/coupons`);
  }

  saveCoupon(id: number | null, body: Partial<Coupon>): Observable<Coupon> {
    return id ? this.http.put<Coupon>(`${this.base}/coupons/${id}`, body) : this.http.post<Coupon>(`${this.base}/coupons`, body);
  }

  suspendCoupon(id: number): Observable<Coupon> {
    return this.http.post<Coupon>(`${this.base}/coupons/${id}/suspend`, {});
  }

  customers(q: string, status: string, page: number): Observable<Page<CustomerRow>> {
    let params = new HttpParams().set('q', q).set('page', page).set('size', 10);
    if (status) params = params.set('status', status);
    return this.http.get<Page<CustomerRow>>(`${this.base}/customers`, { params });
  }

  setCustomerLock(id: number, lock: boolean): Observable<void> {
    return this.http.post<void>(`${this.base}/customers/${id}/${lock ? 'lock' : 'unlock'}`, {});
  }
}
