Mini-Drive Storage Platform API
Thời gian thực hiện: 4 Tuần
Công nghệ: Java 17+, Spring Boot 3.x, Spring Security, Build Tool (Maven/Gradle), Database
(H2/MySQL/PostgreSQL)
Xây dựng một hệ thống RESTful API mô phỏng nền tảng lưu trữ và chia sẻ tệp tin (tương tự
Google Drive), hỗ trợ xác thực người dùng, phân quyền truy cập và xử lý tệp tin hiệu năng cao.
I. Yêu cầu chức năng (Functional Requirements)
Ứng dụng cần cung cấp các API endpoints chia thành các nhóm chức năng sau:
1. Xác thực & Quản lý người dùng (Authentication & User)
● Đăng ký / Đăng nhập:
○ Endpoint: POST /api/v1/auth/register, POST /api/v1/auth/login.
○ Logic: Sử dụng JWT (JSON Web Token) để xác thực. Password phải được mã hoá
(BCrypt).
2. Quản lý Tệp tin & Thư mục (Core Storage)
● Resource Uniform: Sử dụng chung endpoint /files cho cả Tệp và Thư mục (phân biệt
bằng Metadata type: FILE hoặc FOLDER).
● Tạo mới (Upload Files / Create Folder):
○ Endpoint: POST /api/v1/files
○ Trường hợp 1: Upload Multiple Files
■ Header: Content-Type: multipart/form-data
■ Input: files (List<MultipartFile>), parentId (String).
■ Logic:
■ Cho phép client gửi lên nhiều file trong một request (key là files).
■ Hệ thống xử lý lặp: Lưu từng file vật lý và tạo bản ghi metadata tương ứng
(type=FILE).
■ Trả về danh sách thông tin các file đã upload thành công.
○ Trường hợp 2: Tạo Thư mục
■ Header: Content-Type: application/json
■ Input: { "name": "New Folder", "parentId": "...", "type": "FOLDER" }
■ Logic: Chỉ tạo bản ghi metadata trong DB với type=FOLDER.
● Download File (Sync):
○ Endpoint: GET /api/v1/files/{id}/download
○ Logic:
■ Nếu là FILE: Kiểm tra quyền -> Stream nội dung file.
■ Header: Thiết lập Content-Type: application/octet-stream (hoặc MIME type

thực tế của file) để FE nhận biết đây là binary stream và kích hoạt trình duyệt
tải xuống (Blob).
■ Nếu là FOLDER: Kiểm tra quyền -> Tham chiếu mô tả bên dưới.
● Download Folder (Async Zip):
○ Trigger Endpoint: POST /api/v1/files/{id}/download
■ Logic: Nếu id là Folder -> Kích hoạt tác vụ nén bất đồng bộ (@Async).
■ Header: Thiết lập Content-Type: application/json.
■ Body: Trả về JSON chứa requestId. FE dựa vào header này để biết không
phải là file binary mà cần thực hiện logic Polling.
○ Polling Endpoint: GET /api/v1/files/downloads/{requestId}
■ Response: Trạng thái xử lý (PENDING, PROCESSING, READY, FAILED) và link
download (khi READY).

3. Chia sẻ & Phân quyền (Sharing & ACL)
● Chia sẻ Resource:
○ Endpoint: POST /api/v1/files/{id}/share
○ Input: email (người nhận), permission (VIEW hoặc EDIT).
○ Logic:
■ Áp dụng cho cả File và Folder.
■ Nếu là Folder, quyền sẽ được áp dụng đệ quy cho tất cả item bên trong.
■ Gửi email thông báo cho người nhận (Implement làm sao để có thể tuỳ chỉnh mail
provider, mặc định: Mock email provider).

● Danh sách được chia sẻ:
○ Endpoint: GET /api/v1/files/shared-with-me
○ Output: Danh sách file/folder mà người khác đã chia sẻ với user hiện tại.
4. Tìm kiếm & Liệt kê (Search & List)
● Endpoint: GET /api/v1/files
● Input (Query Params): * q: Từ khóa tìm kiếm tên file.
○ type: Lọc theo FILE, FOLDER, hoặc loại MIME cụ thể.
○ parentId: Nếu có -> Liệt kê danh sách con trong thư mục. Nếu không -> Tìm kiếm
toàn cục.
○ fromSize, toSize.
● Logic: Tìm kiếm trong phạm vi "My Drive" và "Shared with me".
5. Thống kê & Quản trị (Analytics & Admin)
● User Dashboard:
○ Endpoint: GET /api/v1/analytics/usage
○ Logic: Thống kê dung lượng cá nhân đã sử dụng.
● Xoá file và dọn dẹp nền định kỳ:
○ Endpoint: DELETE /api/v1/files/{id} (Soft Delete).
○ Logic: Scheduled Task chạy lúc 02:00 sáng xoá vĩnh viễn file trong thùng rác quá 30

ngày (sử dụng Multi-threading, retention day có thể tuỳ chỉnh).
II. Yêu cầu kỹ thuật (Technical Requirements)
1. Kiến trúc & Spring Boot
● Resource Handling: Controller cần xử lý linh hoạt MediaType (consumes) để phân biệt
request Upload (Multipart) và Create Folder (JSON) trên cùng 1 URL.
● Layered Architecture: Controller -> Service -> Repository -> Model/Entity.
● DTO & Validation: Validate input chặt chẽ.
● Exception Handling: Global Exception Handler trả về lỗi chuẩn.
2. Bảo mật (Security)
● Spring Security: Cấu hình Stateless Session (JWT Filter).
● Method Security: Sử dụng @PreAuthorize để kiểm tra quyền truy cập ở cấp độ method.
● Protection: Chống các lỗi bảo mật cơ bản (IDOR) bằng cách luôn check ownerId trong
mọi query.
3. Database Design
● Unified Table Strategy: Sử dụng một bảng chính Files (hoặc Items) cho cả file và folder.
○ Cột type: Enum (FILE, FOLDER).
○ Cột parent_id: Self-referencing Foreign Key.
○ Cột owner_id: Link tới bảng User.
● Bảng FilePermissions: file_id, user_id, permission_level.
4. Concurrency & Performance
● Async Processing: @Async cho tính năng Zip folder và Gửi email thông báo chia sẻ.
● Multi-threading: Dùng ExecutorService cho tác vụ dọn dẹp file rác.
● Transaction Management: Đảm bảo tính nhất quán dữ liệu (Rollback nếu lưu DB lỗi).
5. Testing
● Unit Test: Service layer (bao gồm cả logic check quyền).
● Integration Test: Test luồng đăng ký -> login -> upload -> share -> download.

III. Chi tiết logic sinh dữ liệu giả lập (Mock Data Spec)
Để test hiệu năng và tính năng chia sẻ, cập nhật logic sinh dữ liệu:
● Endpoint: POST /api/v1/debug/generate-system
● Logic:
1. Tạo 10 Users ngẫu nhiên.
2. Mỗi User tạo cấu trúc thư mục/file riêng (tổng cộng ~10.000 files toàn hệ thống).
3. Random Sharing: Chọn ngẫu nhiên 10% số lượng file để chia sẻ chéo giữa các user
với quyền VIEW hoặc EDIT.
● Mục đích: Kiểm tra query "Shared with me" và query "Search" có hoạt động nhanh khi
dữ liệu quyền hạn (permissions) lớn hay không.
IV. Tiêu chí đánh giá
1. RESTful Design: Sử dụng đúng HTTP Method và Resource URI (gộp chung /files).
2. Security Best Practices: Không hardcode secret key, password phải hash, API không lộ
thông tin user khác.
3. Authorization Logic: Chặn đúng quyền (User A không thể xem file riêng tư của User B
nếu không được share).
4. Clean Code & Architecture: Code dễ đọc, tách biệt logic Security và Business.
5. Performance: Các API Search và Listing không bị chậm khi số lượng record Permission
tăng cao.
6. Documentation & Tooling: Cung cấp Postman Collection (hoặc
Insomnia/Bruno/OpenAPI) để chạy thử ngay.
○ Yêu cầu sử dụng Environment Variables cho URL và Token (không hardcode trong
request).
○ Điểm cộng lớn nếu thiết lập Pre-request script để tự động gán Token vào biến môi
trường sau khi Login thành công.