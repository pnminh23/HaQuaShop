import { Page, ProductStatus } from './common.model';

export interface ProductCard {
  id: number;
  slug: string;
  name: string;
  brand: string;
  categorySlug: string;
  categoryName: string;
  price: number;
  originalPrice: number;
  discountPercent: number;
  rating: number;
  reviewCount: number;
  soldCount: number;
  imageUrl: string | null;
  status: ProductStatus;
  defaultVariantId: number | null;
}

export interface ProductListResponse {
  page: Page<ProductCard>;
  categoryFacets: Record<string, number>;
}

export interface ProductFilter {
  q?: string;
  category?: string[];
  brand?: string[];
  priceMin?: number;
  priceMax?: number;
  minRating?: number;
  onSale?: boolean;
  inStock?: boolean;
  sort?: string;
  page?: number;
}

export interface Variant {
  id: number;
  sku: string;
  color: string | null;
  storage: string | null;
  price: number;
  originalPrice: number;
  discountPercent: number;
  stock: number;
}

export interface ProductDetail {
  id: number;
  slug: string;
  name: string;
  brand: string;
  categorySlug: string;
  categoryName: string;
  description: string;
  status: ProductStatus;
  rating: number;
  reviewCount: number;
  soldCount: number;
  variants: Variant[];
  images: { url: string; altText: string }[];
  specs: { key: string; value: string }[];
  seo: { focusKeyword: string; metaTitle: string; metaDescription: string; seoScore: number } | null;
  related: ProductCard[];
}

/** Một hãng máy tính trên menu cửa hàng. */
export interface MenuBrand {
  name: string;
  slug: string;
}

export interface CategoryRequest {
  name: string;
  slug?: string;
  seoDescription?: string;
  parentId?: number | null;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  seoDescription: string;
  parentId: number | null;
  productCount: number;
  /** Đang ẩn khỏi cửa hàng (chỉ trang quản trị nhận được danh mục ẩn). */
  hidden: boolean;
}

export interface Review {
  id: number;
  author: string;
  stars: number;
  content: string;
  verifiedPurchase: boolean;
  createdAt: string;
}

export interface ReviewList {
  summary: { average: number; count: number; percentByStar: Record<string, number> };
  reviews: Review[];
}

export interface Post {
  id: number;
  title: string;
  slug: string;
  tag: string;
  content: string;
  coverUrl: string | null;
  publishedAt: string | null;
}

export interface Faq {
  question: string;
  answer: string;
}
