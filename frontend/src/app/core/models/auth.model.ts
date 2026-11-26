export type Role = 'CUSTOMER' | 'STAFF' | 'ADMIN';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  userId: number;
  fullName: string;
  role: Role;
}

export interface SessionUser {
  userId: number;
  fullName: string;
  role: Role;
}

export interface Address {
  id?: number;
  receiverName: string;
  phone: string;
  province: string;
  ward: string;
  detail: string;
  defaultAddress: boolean;
}

export interface Profile {
  id: number;
  email: string;
  phone: string;
  fullName: string;
  role: Role;
  greenPoints: number;
  addresses: Address[];
}
