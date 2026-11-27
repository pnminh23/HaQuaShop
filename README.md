# HaQua – Website bán thiết bị điện tử

Code theo bản thiết kế và tài liệu nghiệp vụ (22 use case).

| Thành phần | Công nghệ |
|---|---|
| `backend/` | Java 21, Spring Boot 4.2, Spring Security + JWT (HS256), Spring Data JPA, H2 chế độ MySQL (dev) / MySQL 8 (prod) |
| `frontend/` | Angular 22 (standalone, signals, zoneless), SSR cho trang công khai |

## Chạy thử

Cần JDK 21, Node và MySQL đang chạy (cấu hình trong `backend/.env`, mẫu ở `backend/.env.example`).

```bash
# Cài thư viện frontend (chỉ lần đầu)
cd frontend && npm install && cd ..

# Chạy cả backend (profile dev, cổng 8080) và frontend (cổng 4200); Ctrl+C để tắt cả hai
npm run dev

# Đổi cổng khi cổng mặc định đang bận
BACKEND_PORT=8089 FRONTEND_PORT=4201 npm run dev
```

`npm run dev` (xem `scripts/dev.mjs`) tự tìm JDK 21 của SDKMAN nếu `java` mặc định cũ hơn, và dùng thư mục ảnh `backend/uploads` (đặt biến `HAQUA_UPLOAD_DIR` trong shell nếu muốn đổi).

Tài khoản mẫu (chỉ được nạp ở profile dev khi bảng người dùng còn trống): `admin@haqua.vn / Admin@123`, `staff@haqua.vn / Staff@123`, `khach@haqua.vn / Khach@123`. Mã giảm giá mẫu: `HAQUA10`.

Chạy với MySQL: `cd backend && cp .env.example .env && docker compose up -d db`, rồi `SPRING_PROFILES_ACTIVE=prod JWT_SECRET=<chuỗi ≥ 32 ký tự> ./mvnw spring-boot:run`.

## Cấu trúc backend – MVC theo tầng (`vn.haqua.shop`)

```
controller/   (C) REST controller: nhận request, validate, gọi service, trả DTO
service/      nghiệp vụ, @Transactional: Auth, Product, Cart, Order, Payment, Chatbot, Dashboard…
repository/   (truy cập dữ liệu) Spring Data JPA + ProductSpecifications
entity/       (M) mô hình miền / bảng CSDL: User, Product, ProductVariant, Order, Cart, Coupon…
dto/          đối tượng vào/ra của API (record): CatalogDtos, CartDtos, OrderDtos, AuthDtos…
enums/        Role, OrderStatus, PaymentMethod, ProductStatus…
mapper/       entity → DTO (ProductMapper, OrderMapper)
client/       cổng ra hệ thống ngoài: LlmClient, PaymentGatewayClient (+ bản mock)
event/        sự kiện nội bộ (đặt hàng, đổi trạng thái, thanh toán xong)
config/       SecurityConfig (JWT, CORS), WebConfig, AppProperties
exception/    ApiException + GlobalExceptionHandler (trả ProblemDetail)
security/     CurrentUser (lấy người dùng từ JWT)
util/         SlugUtil, RateLimiter
bootstrap/    DataSeeder (dữ liệu mẫu, chỉ profile dev)
```

Luồng gọi: `controller → service → repository → entity`. View (V) là ứng dụng Angular, giao tiếp qua JSON.
Phân quyền: `/api/admin/**` yêu cầu STAFF trở lên; sản phẩm, khuyến mãi, khách hàng, dashboard, cấu hình chatbot yêu cầu ADMIN.

## Cấu trúc frontend (`src/app`)

```
core/       models, services (auth, cart, catalog, order, chat, SEO, admin-api), interceptor (JWT + cart token + refresh), guards
shared/     product-card, product-thumb, star-rating, toast, vnd pipe
layout/     store-layout (header/footer/chat), admin-layout
features/   home, catalog (danh sách + chi tiết), cart, checkout, orders (tra cứu), auth, account, chat, admin/*
```

SSR bật cho trang chủ, danh sách và chi tiết sản phẩm (kèm title, meta, JSON-LD Product và BreadcrumbList); giỏ, thanh toán, tài khoản, quản trị chỉ render ở trình duyệt.

## Còn thiếu / cần làm tiếp (đã biết)

- **LLM**: `OpenAiCompatibleLlmClient` gọi Groq (chuẩn OpenAI). Cần `LLM_API_KEY` trong `backend/.env`; đổi `LLM_BASE_URL`/`LLM_MODEL` để dùng OpenRouter hoặc Ollama. Không có key thì chatbot trả câu xin lỗi và nút "Gặp nhân viên".
- **Cổng thanh toán thật**: `MockPaymentGatewayClient`. Webhook đã kiểm chữ ký HMAC (`X-Signature` trên chuỗi `txnId|amount|status`).
- **SMS/email**: `NotificationService` mới ghi log.
- **Schema prod**: đang `ddl-auto: update`; nên chuyển sang Flyway khi chốt schema.
- **Refresh token** không thu hồi phía server khi đăng xuất (stateless); cần bảng token nếu muốn thu hồi.
- **Chưa làm**: nhập sản phẩm từ Excel, in phiếu giao PDF, xuất hóa đơn điện tử, trang Cẩm nang và quản lý bài viết ở frontend (API đã có), sitemap.xml/robots.txt, thông số kỹ thuật trong form sản phẩm admin, xuất Excel (hiện xuất CSV).
- **Mục [cần chốt]** trong tài liệu dùng giá trị mặc định: tồn thấp ≤ 5, khóa 15 phút sau 5 lần sai, QR hết hạn 30 phút, phí giao nhanh cố định 30.000₫.
