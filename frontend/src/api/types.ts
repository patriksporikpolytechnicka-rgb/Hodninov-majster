// Types shared with the backend API contract in docs/api-types.ts.
export type Role = 'CUSTOMER' | 'PROVIDER' | 'ADMIN';
export type PriceUnit = 'HOUR' | 'JOB' | 'SQUARE_METER';
export type OrderStatus = 'PENDING' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'REJECTED';
export interface RegisterRequest { email: string; password: string; role: 'CUSTOMER' | 'PROVIDER'; }
export interface LoginRequest { email: string; password: string; }
export interface UserResponse { id: number; email: string; role: Role; }
export interface AuthResponse { accessToken: string; tokenType: 'Bearer'; expiresIn: number; user: UserResponse; }
export interface CategoryRequest { name: string; }
export interface CategoryResponse { id: number; name: string; }
export interface ProviderRequest {
  name: string; description: string; location: string; phone: string;
  profileImage?: string | null; availability: boolean;
}
export interface ProviderResponse extends Omit<ProviderRequest, 'profileImage'> { id: number; profileImage: string | null; }
export interface ServiceRequest {
  categoryId: number; name: string; description: string; price: number; priceUnit: PriceUnit; active: boolean;
}
export interface ServiceResponse {
  id: number; provider: ProviderResponse; category: CategoryResponse; name: string;
  description: string; price: number; priceUnit: PriceUnit; active: boolean;
}
export interface OrderRequest {
  serviceId: number; scheduledAt: string; quantity: number; address: string; note?: string | null;
}
export interface StatusRequest { status: OrderStatus; }
export interface OrderResponse {
  id: number; customerId: number; providerId: number; serviceId: number; serviceName: string;
  scheduledAt: string; status: OrderStatus; unitPrice: number; priceUnit: PriceUnit;
  quantity: number; totalPrice: number; address: string; note: string | null; createdAt: string;
}
export interface PageResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface ApiError { status: number; message: string; fieldErrors: Record<string, string>; }
export interface SubscriptionResponse {
  status: 'TRIAL' | 'ACTIVE' | 'EXPIRED'; subscriptionStart: string | null;
  subscriptionEnd: string | null; monthlyFee: number; currency: 'EUR';
  canReceiveOrders: boolean; demoPayment: boolean;
}
export interface AppInfo { name: string; demo: boolean; demoAccounts: boolean; }
