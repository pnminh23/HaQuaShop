export interface CartItem {
  id: number;
  variantId: number;
  productSlug: string;
  name: string;
  variantLabel: string;
  imageUrl: string | null;
  unitPrice: number;
  originalPrice: number;
  qty: number;
  selected: boolean;
  savedForLater: boolean;
  stock: number;
  lineTotal: number;
}

export interface CartSummary {
  subtotal: number;
  productDiscount: number;
  couponCode: string | null;
  couponDiscount: number;
  shippingFee: number;
  total: number;
  selectedCount: number;
}

export interface Cart {
  cartToken: string | null;
  items: CartItem[];
  summary: CartSummary;
  itemCount: number;
  notice: string | null;
}

export type ShippingMethod = 'STANDARD' | 'EXPRESS_2H' | 'STORE_PICKUP';
export type PaymentMethod = 'COD' | 'QR' | 'EWALLET' | 'CARD' | 'INSTALLMENT';

export interface CheckoutRequest {
  receiverName: string;
  phone: string;
  email?: string;
  province: string;
  ward: string;
  detail: string;
  note?: string;
  shippingMethod: ShippingMethod;
  paymentMethod: PaymentMethod;
  installmentMonths?: number;
  invoice?: { taxCode: string; company: string; email: string };
}

export interface CheckoutResponse {
  orderCode: string;
  total: number;
  estimatedDelivery: string;
  pointsEarned: number;
  qrData: string | null;
  transferContent: string | null;
  redirectUrl: string | null;
}
