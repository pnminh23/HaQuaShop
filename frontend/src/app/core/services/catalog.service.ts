import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import { Banner } from '../models/banner.model';
import { Page } from '../models/common.model';
import {
  Category,
  Faq,
  MenuBrand,
  Post,
  ProductCard,
  ProductDetail,
  ProductFilter,
  ProductListResponse,
  ReviewList,
} from '../models/catalog.model';

export interface Suggestion { slug: string; name: string; price: number; imageUrl: string | null }

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  products(filter: ProductFilter, size = 12): Observable<ProductListResponse> {
    let params = new HttpParams().set('size', size).set('page', filter.page ?? 0).set('sort', filter.sort ?? 'popular');
    if (filter.q) params = params.set('q', filter.q);
    filter.category?.forEach((c) => (params = params.append('category', c)));
    filter.brand?.forEach((b) => (params = params.append('brand', b)));
    if (filter.priceMin != null) params = params.set('priceMin', filter.priceMin);
    if (filter.priceMax != null) params = params.set('priceMax', filter.priceMax);
    if (filter.minRating != null) params = params.set('minRating', filter.minRating);
    if (filter.onSale) params = params.set('onSale', true);
    if (filter.inStock) params = params.set('inStock', true);
    return this.http.get<ProductListResponse>(`${API_URL}/products`, { params });
  }

  computerBrands(): Observable<MenuBrand[]> {
    return this.http.get<MenuBrand[]>(`${API_URL}/brands/computers`);
  }

  banners(): Observable<Banner[]> {
    return this.http.get<Banner[]>(`${API_URL}/banners`);
  }

  /** Gợi ý ngắn cho ô tìm kiếm ở header. */
  suggest(q: string): Observable<Suggestion[]> {
    return this.http.get<Suggestion[]>(`${API_URL}/products/suggest`, { params: { q } });
  }

  detail(slug: string): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${API_URL}/products/slug/${slug}`);
  }

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${API_URL}/categories`);
  }

  reviews(productId: number): Observable<ReviewList> {
    return this.http.get<ReviewList>(`${API_URL}/products/${productId}/reviews`);
  }

  writeReview(productId: number, stars: number, content: string): Observable<unknown> {
    return this.http.post(`${API_URL}/products/${productId}/reviews`, { stars, content });
  }

  posts(size = 3): Observable<Page<Post>> {
    return this.http.get<Page<Post>>(`${API_URL}/posts`, { params: { size } });
  }

  faq(): Observable<Faq[]> {
    return this.http.get<Faq[]>(`${API_URL}/faq`);
  }
}
