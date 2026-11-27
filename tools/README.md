# tools/

## Lấy dữ liệu sản phẩm (fptshop_crawl.py)
    python3 tools/fptshop_crawl.py dien-thoai --limit 30
    python3 tools/fptshop_crawl.py may-tinh-xach-tay --limit 30
    python3 tools/fptshop_crawl.py tai-nghe --sitemap phu-kien --match /phu-kien/tai-nghe --limit 30
    python3 tools/fptshop_crawl.py chuot --sitemap phu-kien,phu-kien-1,phu-kien-2 --match /phu-kien/chuot- --limit 30
    python3 tools/fptshop_crawl.py ban-phim --sitemap phu-kien,phu-kien-1,phu-kien-2 --match /phu-kien/ban-phim- --limit 30

Thêm `--append` để giữ dữ liệu cũ và lấy thêm `--limit` sản phẩm mới.
Kết quả: `tools/out/<danh-muc>.csv` + ảnh trong `tools/out/images/`.

## Nhập vào shop
Đặt biến `HAQUA_IMPORT_DIR=../tools/out` (đường dẫn tính từ thư mục `backend`) rồi chạy backend:

    cd backend && HAQUA_IMPORT_DIR=../tools/out ./mvnw spring-boot:run

- Sản phẩm nhập vào ở trạng thái **Nháp**, giá 0, tồn kho 0; vào Admin → Sản phẩm đặt giá/tồn kho rồi đăng bán.
- Thương hiệu tự tạo trong bảng `brands`; ảnh được chép vào thư mục uploads.
- Chạy lại nhiều lần an toàn (trùng slug thì bỏ qua). Dev dùng H2 trong bộ nhớ nên mỗi lần khởi động dữ liệu được tạo lại.
