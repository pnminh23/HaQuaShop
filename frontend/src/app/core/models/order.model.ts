import { PaymentMethod } from './cart.model';

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PACKING' | 'SHIPPING' | 'COMPLETED' | 'CANCELLED';
export type PaymentStatus = 'UNPAID' | 'PAID' | 'REFUNDED';
export type OrderChannel = 'WEBSITE' | 'CHATBOT' | 'STORE';

export interface OrderItem {
  productName: string;
  variantLabel: string;
  unitPrice: number;
  qty: number;
  lineTotal: number;
}

export interface OrderHistory {
  fromStatus: string | null;
  toStatus: string;
  note: string | null;
  changedAt: string;
}

export interface OrderTracking {
  code: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  paymentMethod: PaymentMethod;
  receiverName: string;
  maskedPhone: string;
  maskedAddress: string;
  total: number;
  createdAt: string;
  cancellable: boolean;
  items: OrderItem[];
  timeline: OrderHistory[];
}

export interface OrderRow {
  id: number;
  code: string;
  receiverName: string;
  channel: OrderChannel;
  total: number;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  createdAt: string;
}

export interface OrderDetail extends Omit<OrderRow, 'receiverName'> {
  paymentMethod: PaymentMethod;
  receiverName: string;
  phone: string;
  email: string;
  shippingAddress: string;
  subtotal: number;
  discount: number;
  shippingFee: number;
  note: string;
  items: OrderItem[];
  history: OrderHistory[];
  canAdvance: boolean;
  nextAction: string | null;
}
