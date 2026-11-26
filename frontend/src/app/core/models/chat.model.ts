import { ProductCard } from './catalog.model';

export type ConversationStatus = 'AI' | 'WAITING_STAFF' | 'STAFF' | 'CLOSED';
export type Sender = 'USER' | 'BOT' | 'STAFF';

export interface ChatMessage {
  id: number;
  sender: Sender;
  content: string;
  products: ProductCard[];
  createdAt: string;
}

export interface ChatState {
  sessionToken: string;
  status: ConversationStatus;
  botEnabled: boolean;
  botName: string;
  messages: ChatMessage[];
}
