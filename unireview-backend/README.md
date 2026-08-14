# 🚀 UniReview Backend — Full Specification & Guide

Tài liệu hợp nhất toàn bộ **Hướng dẫn khởi chạy**, **Đặc tả kỹ thuật (Specification)**, **Cơ sở dữ liệu (Database Schema)** và **REST API Specification** của dự án **UniReview Backend (Spring Boot 3 + PostgreSQL)**.

---

## 🛠️ 1. Công nghệ Sử dụng (Tech Stack)
- **Java 17** + **Spring Boot 3.2.3**
- **Spring Data JPA** & **Hibernate**
- **PostgreSQL 18** (Database)
- **Flyway** (SQL Database Migrations)
- **Spring Security** + **JWT** (Quản trị Admin `/api/admin/**`)
- **Google Perspective API** (AI Auto-Moderation) & **reCAPTCHA v3** (Chống Spam)
- **Lombok**, **Jackson**, **Apache Commons CSV**
- **Springdoc OpenAPI / Swagger UI**

---

## 🚀 2. Hướng dẫn Khởi chạy Dự án (Quick Start)

### Bước 1: Khởi động Cơ sở dữ liệu PostgreSQL
Chạy PostgreSQL container qua Docker Compose:
```bash
cd unireview-backend
docker-compose up -d
```
> Database `unireview_db` sẽ chạy tại port `5432` với user `postgres` / pass `postgrespassword`.

### Bước 2: Build & Chạy Spring Boot App
```bash
# Biên dịch và đóng gói JAR
mvnw clean package -DskipTests

# Chạy ứng dụng
java -jar target/unireview-backend-1.0.0.jar
```
Hoặc chạy trực tiếp từ Maven plugin:
```bash
mvnw spring-boot:run
```

Server sẽ khởi chạy tại cổng **`http://localhost:8080`**. Flyway sẽ tự động tạo bảng và thêm dữ liệu khởi tạo.

### Bước 3: Xem API Documentation (Swagger UI)
Truy cập trình duyệt tại: **`http://localhost:8080/swagger-ui.html`**

---

## 🔄 3. Luồng Nghiệp vụ chính (Core Workflows)

### 3.1 Luồng Tích lũy & Sử dụng Credit
```
[User vào Web]
      │
      ▼
[Click Giảng viên A] ──(Đã unlock?)──► Có ──► [Xem đầy đủ review]
      │
     Chưa
      │
      ▼
(Còn Credit không?) ──► Có (≥1) ──► [Trừ 1 Credit] ──► [Unlock GV A] ──► [Xem review]
      │
     Không (0)
      │
      ▼
[Mở GateModal] ──► [Viết review GV B khác]
      │
      ▼
[AI Auto-Moderate] ──(Hợp lệ?)
      │
  ┌───┴──────────┐
 Pass          Flag/Reject
  │              │
  ▼              ▼
[ APPROVED ]   [ FLAGGED ] ──► Chờ Admin duyệt
[Credit +1]
[Auto-Unlock GV B]
```

### 3.2 Quy tắc Credit
- **Viết 1 Review được duyệt (APPROVED)**: Cộng 1 credit.
- **Tự động unlock giảng viên mình đã review**: Review giảng viên B thành công thì tự động mở khóa xem giảng viên B miễn phí (không tốn credit).
- **Xem giảng viên mới**: Trừ 1 credit nếu chưa unlock. Lần xem tiếp theo về giảng viên này hoàn toàn miễn phí.
- **Admin ẩn review xấu**: Trừ 1 credit của reviewer tương ứng.

---

## 👤 4. Định danh Ẩn danh (Anonymous Identity Strategy)

Hệ thống nhận diện người dùng qua **Reviewer Token** (UUIDv4) được tạo ra khi người dùng đăng review lần đầu tiên.

### Vị trí lưu trữ ở Client:
- **Cookie (`reviewer_token`)**: `Max-Age` 1 năm, `SameSite=Lax`, `HttpOnly=false`.
- **Web Storage (`localStorage.getItem('reviewer_token_backup')`)**: Dự phòng khôi phục cookie khi bị xóa cache.

### Header Yêu cầu (Request Headers):
Mỗi API yêu cầu dữ liệu gated (như `/api/teachers/{id}/reviews`) hoặc thao tác cá nhân phải đính kèm header:
`X-Reviewer-Token: <UUID>` (hoặc tự động lấy từ Cookie).

---

## 🗄️ 5. Cơ sở Dữ liệu (Database Schema)

Hệ thống sử dụng **PostgreSQL** với Flyway migration tại `src/main/resources/db/migration/V1__initial_schema.sql`.

### Các Bảng chính:
- **`teachers`**: Giảng viên (`id`, `full_name`, `title`, `faculty`, `department`, `avatar_url`, `avg_rating`, `total_reviews`).
- **`subjects`**: Môn học (`id`, `code`, `name`, `faculty`, `credits`).
- **`teacher_subjects`**: Phân công giảng dạy (`id`, `teacher_id`, `subject_id`, `semester`).
- **`reviewers`**: Định danh token ẩn danh (`id`, `token`, `review_count`, `credit_balance`, `ip_hash`).
- **`reviews`**: Nhận xét (`id`, `reviewer_token`, `teacher_id`, `subject_id`, `ratings`, `difficulty`, `attendance`, `materials_allowed`, `would_recommend`, `workload`, `content`, `semester`, `status`, `toxicity_score`).
  - *Ràng buộc*: `UNIQUE (reviewer_token, teacher_id)` -> Ngăn 1 reviewer review 1 giảng viên 2 lần.
- **`unlocked_teachers`**: Giảng viên đã được token mở khóa (`id`, `reviewer_token`, `teacher_id`, `unlocked_at`).
  - *Ràng buộc*: `UNIQUE (reviewer_token, teacher_id)`
- **`review_votes`**: Upvote/Downvote review (`id`, `voter_token`, `review_id`, `vote_type`).
  - *Ràng buộc*: `UNIQUE (voter_token, review_id)`
- **`review_reports`**: Báo cáo vi phạm (`id`, `review_id`, `reason`, `description`, `status`).
- **`admin_users`**: Quản trị viên (`id`, `username`, `password_hash`, `role`).

---

## 🤖 6. Cơ chế AI Auto-Moderation (Kiểm duyệt Tự động)

```
Nội dung review -> [Lớp 1: Keyword & Pattern Filter] ──(Vi phạm?)──► Có ──► [REJECTED (400 Bad Request)]
                               │
                             Không
                               │
                               ▼
                    [Lớp 2: Google Perspective API]
                               │
                       ┌───────┴───────┐
                 Score < 0.7      Score >= 0.7
                       │               │
                       ▼               ▼
                 [ APPROVED ]     [ FLAGGED ]
               (Hiển thị ngay,   (Chờ admin xét,
                cộng credit)    chưa có credit)
```

1. **Lớp 1: Keyword & Pattern Filter (Local Regex, instant)**
   - Chặn từ thô tục tiếng Việt (`banned_words.txt`).
   - Chặn số điện thoại (`09x...`), email và link website quảng cáo.
   - Nếu vi phạm -> Trả về lỗi `400 Bad Request` (`CONTENT_VIOLATION`).
2. **Lớp 2: Google Perspective API (AI toxicity check, ~200ms)**
   - Score < 0.7 -> Status `APPROVED` -> Cộng 1 credit cho reviewer ngay lập tức.
   - Score >= 0.7 -> Status `FLAGGED` -> Chưa cộng credit, chuyển về trang Admin hậu kiểm.
   - Nếu API lỗi/timeout -> Fail-open thành `APPROVED` (admin kiểm tra qua báo cáo report).

---

## 📡 7. Danh sách REST API Specification

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| GET | `/api/teachers` | Danh sách giảng viên (tìm kiếm, lọc khoa, rating, phân trang) | Public |
| GET | `/api/teachers/{id}` | Chi tiết 1 giảng viên + thống kê trắc nghiệm public | Public |
| GET | `/api/teachers/{id}/reviews` | Danh sách reviews chi tiết (**cần token & credit để mở khóa**) | Cookie / Token |
| POST | `/api/reviews` | Đăng review mới (qua AI moderation -> cộng credit) | Cookie / Token |
| POST | `/api/reviews/{id}/vote` | Upvote/Downvote review | Cookie / Token |
| POST | `/api/reviews/{id}/report` | Report review vi phạm | Cookie / Token |
| GET | `/api/gate/status` | Xem số dư credit & danh sách giảng viên đã unlock | Cookie / Token |
| POST | `/api/admin/login` | Đăng nhập Admin -> Nhận JWT | Public |
| GET | `/api/admin/reviews/flagged` | Xem danh sách reviews bị AI cờ nghi vấn | Admin JWT |
| PUT | `/api/admin/reviews/{id}/approve` | Admin duyệt review nghi vấn -> cộng credit | Admin JWT |
| PUT | `/api/admin/reviews/{id}/reject` | Admin từ chối review nghi vấn | Admin JWT |
| POST | `/api/admin/teachers/import-csv` | Import danh sách giảng viên từ file CSV | Admin JWT |

---

## 🛡️ 8. Bảo mật & Chống Spam
- **reCAPTCHA v3**: Verify token ở backend cho tất cả request ghi dữ liệu.
- **Rate Limiting theo IP**: Tối đa 3 reviews/ngày/IP.
- **Mã hóa IP**: Lưu dạng hash SHA-256 (`ip_hash`) bảo vệ thông tin riêng tư.
- **CORS Config**: `allowCredentials(true)` hỗ trợ truyền nhận cookie cross-origin an toàn với `http://localhost:5173`.
