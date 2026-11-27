import { OrderRow } from './order.model';
import { ProductStatus } from './common.model';
import { ConversationStatus, ChatMessage } from './chat.model';

export interface Kpi { label: string; value: number; changePercent: number }
/** Kỳ báo cáo: N ngày gần nhất, hoặc khoảng ngày tự chọn (yyyy-MM-dd). */
export type DashboardPeriod = { range: number } | { from: string; to: string };

export interface Dashboard {
  rangeDays: number;
  from: string;
  to: string;
  kpis: Kpi[];
  revenueByDay: { date: string; revenue: number }[];
  revenueByCategory: { category: string; revenue: number; percent: number }[];
  recentOrders: OrderRow[];
  topProducts: { productId: number; name: string; sold: number; revenue: number }[];
  lowStock: { variantId: number; productName: string; variant: string; stock: number }[];
}

export interface AdminProductRow {
  id: number;
  name: string;
  slug: string;
  categoryName: string;
  price: number;
  totalStock: number;
  status: ProductStatus;
  seoScore: number;
  imageUrl: string | null;
}

export interface VariantInput { id?: number; sku: string; color?: string; storage?: string; price: number; originalPrice: number; stock: number }
export interface AdminProductRequest {
  name: string;
  categoryId: number;
  brand?: string;
  description?: string;
  status: ProductStatus;
  slug?: string;
  variants: VariantInput[];
  images: { url: string; altText: string }[];
  specs: { key: string; value: string }[];
  seo: { focusKeyword?: string; metaTitle?: string; metaDescription?: string };
}

export interface SeoReport { score: number; criteria: { label: string; ok: boolean }[] }

export interface Coupon {
  id: number;
  code: string;
  type: 'PERCENT' | 'FIXED';
  value: number;
  maxDiscount: number;
  minOrder: number;
  startAt: string | null;
  endAt: string | null;
  usageLimit: number;
  usedCount: number;
}

export interface CustomerRow {
  id: number;
  fullName: string;
  phone: string;
  email: string;
  orderCount: number;
  totalSpent: number;
  greenPoints: number;
  status: 'ACTIVE' | 'LOCKED';
}

export interface ConversationRow {
  id: number;
  customer: string;
  status: ConversationStatus;
  tag: string;
  lastMessage: string;
  lastMessageAt: string;
  assignedStaff: string | null;
}

export interface ConversationDetail {
  id: number;
  status: ConversationStatus;
  tag: string;
  assignedStaff: string | null;
  messages: ChatMessage[];
}

export interface ChatStats { conversationsToday: number; aiHandledPercent: number; ordersFromChatbot: number; csat: number }

export interface ChatbotConfig {
  displayName: string;
  tone: 'FRIENDLY' | 'PROFESSIONAL' | 'SHORT';
  handoffRule: 'ASK_OR_UNSURE' | 'ASK_ONLY' | 'ORDER_OVER_30M';
  extraInstructions: string;
  enabled: boolean;
  sources: { id: number; type: string; name: string; enabled: boolean }[];
}

export interface Brand { id: number; name: string; slug: string; productCount: number; computerBrand: boolean; menuOrder: number; }

export interface BrandRequest { name: string; computerBrand?: boolean; menuOrder?: number; }
