# 📝 UniReview — Technical Specification (Tài liệu Tả kỹ thuật)

Tài liệu này đặc tả chi tiết toàn bộ kiến trúc, cơ chế nghiệp vụ, cơ sở dữ liệu và API của dự án **UniReview** phục vụ cho việc phát triển backend (Spring Boot) và kết nối frontend (React).

---

## 1. Tổng quan Dự án (System Overview)
UniReview là một nền tảng đánh giá giảng viên ẩn danh dành cho sinh viên trong cùng một trường đại học.
- **Mục tiêu**: Giúp sinh viên lựa chọn giảng viên phù hợp bằng cách xem nhận xét thực chất về cách giảng dạy, chấm điểm, thi cử.
- **Mô hình cốt lõi**: **Credit-Based Gate System (Glassdoor Model)** — Sinh viên cần đóng góp 1 review chất lượng (được duyệt bằng AI) để đổi lấy 1 credit. Dùng 1 credit để mở khóa xem nhận xét chi tiết của 1 giảng viên mới.
- **Tính riêng tư**: Hoàn toàn ẩn danh, không cần tài khoản cho sinh viên để tối đa hóa số lượng review trung thực và bảo vệ sinh viên.

---

## 2. Luồng Nghiệp vụ chính (Core Workflows)

### 2.1 Luồng Tích lũy & Sử dụng Credit
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

### 2.2 Quy tắc Trừ/Cộng Credit
- **Viết 1 Review được duyệt (APPROVED)**: Cộng 1 credit.
- **Tự động unlock giảng viên mình đã review**: Review giảng viên B thành công thì tự động mở khóa xem giảng viên B miễn phí (không tốn credit).
- **Xem giảng viên mới**: Trừ 1 credit nếu chưa unlock. Lần xem tiếp theo về giảng viên này hoàn toàn miễn phí.
- **Admin ẩn review xấu**: Trừ 1 credit của reviewer tương ứng (nếu số dư > 0).

---

## 3. Quản lý Định danh Ẩn danh (Anonymous Identity Strategy)

Hệ thống nhận diện người dùng qua **Reviewer Token** (UUIDv4) được tạo ra khi người dùng đăng review lần đầu tiên.

### 3.1 Vị trí lưu trữ ở Client
1. **Cookie (`reviewer_token`)**:
   - `Max-Age`: 1 năm (31,536,000 giây)
   - `SameSite`: `Lax` (cho phép gửi cookie ở các request thông thường)
   - `HttpOnly`: `false` (để JavaScript ở frontend có thể đọc và khôi phục khi cần)
   - `Secure`: `true` (chỉ gửi qua HTTPS khi deploy môi trường production)
2. **Web Storage (`localStorage.getItem('reviewer_token_backup')`)**:
   - Dùng để dự phòng khôi phục cookie khi người dùng xóa cache trình duyệt.

### 3.2 Header Yêu cầu (Request Headers)
Mỗi API yêu cầu dữ liệu gated (như `/api/teachers/{id}/reviews`) hoặc thực hiện thao tác cá nhân (upvote, review) phải đính kèm header:
`X-Reviewer-Token: <UUID>`

---

## 4. Cơ sở Dữ liệu (Database Schema)

Hệ thống sử dụng **PostgreSQL**. Dưới đây là đặc tả chi tiết các bảng:

### 4.1 Bảng `teachers` (Giảng viên)
Lưu thông tin giảng viên.
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | ID tự tăng |
| `full_name` | VARCHAR(100) | NOT NULL | Họ và tên giảng viên |
| `title` | VARCHAR(20) | | Học hàm/học vị (TS, ThS, PGS, GS) |
| `faculty` | VARCHAR(100) | NOT NULL | Khoa quản lý |
| `department` | VARCHAR(100) | | Bộ môn |
| `avatar_url` | VARCHAR(255) | | Link ảnh chân dung |
| `avg_rating` | NUMERIC(3,2) | Default `0.00` | Điểm đánh giá trung bình (1.00 - 5.00) |
| `total_reviews` | INT | Default `0` | Tổng số reviews đã được APPROVED |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo bản ghi |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày cập nhật |

### 4.2 Bảng `reviewers` (Định danh Người đánh giá)
Lưu trữ thông tin số dư credit và lượt đóng góp của mỗi token.
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | ID tự tăng |
| `token` | VARCHAR(50) | UNIQUE, NOT NULL | UUIDv4 định danh lưu ở client |
| `review_count` | INT | Default `0` | Tổng số review đã viết |
| `credit_balance` | INT | Default `0`, CHECK `credit_balance >= 0` | Số dư credit còn lại để xem |
| `ip_hash` | VARCHAR(64) | | Mã SHA-256 hash của IP đăng ký đầu tiên |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |
| `last_active_at`| TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày hoạt động cuối cùng |

### 4.3 Bảng `reviews` (Nhận xét giảng viên)
Lưu thông tin nhận xét và điểm rating chi tiết.
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | ID tự tăng |
| `reviewer_token`| VARCHAR(50) | FK (`reviewers.token`) | Người viết nhận xét |
| `teacher_id` | BIGINT | FK (`teachers.id`) | Giảng viên được nhận xét |
| `subject_id` | BIGINT | FK (`subjects.id`), Nullable | Thuộc môn học nào |
| `rating_overall` | INT | CHECK `between 1 and 5` | Điểm đánh giá tổng quan |
| `rating_teaching`| INT | CHECK `between 1 and 5` | Điểm chất lượng giảng dạy |
| `rating_grading` | INT | CHECK `between 1 and 5` | Điểm chấm điểm dễ/khó |
| `rating_personality`| INT | CHECK `between 1 and 5`| Điểm tính cách |
| `difficulty` | VARCHAR(20) | | Độ khó (VERY_EASY, EASY, MEDIUM, HARD, VERY_HARD) |
| `attendance` | VARCHAR(20) | | Điểm danh (NEVER, SOMETIMES, OFTEN, STRICT) |
| `materials_allowed`| VARCHAR(20)| | Cho phép tài liệu (YES, NO, DEPENDS) |
| `would_recommend`| VARCHAR(20)| | Recommend không (YES, NO, MAYBE) |
| `workload` | VARCHAR(20) | | Khối lượng bài tập (LIGHT, MODERATE, HEAVY, VERY_HEAVY) |
| `content` | TEXT | Min 50 ký tự | Nội dung bài viết nhận xét |
| `semester` | VARCHAR(20) | NOT NULL | Học kỳ đã học giảng viên đó |
| `upvote_count` | INT | Default `0` | Số lượt đồng tình |
| `downvote_count`| INT | Default `0` | Số lượt phản đối |
| `status` | VARCHAR(20) | Default `PENDING` | Trạng thái (APPROVED, FLAGGED, REJECTED, HIDDEN) |
| `toxicity_score`| NUMERIC(3,2) | | Điểm độ độc hại từ AI (0.00 - 1.00) |
| `ip_hash` | VARCHAR(64) | | SHA-256 hash IP của người viết |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày đăng |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày sửa |

> **Ràng buộc duy nhất (Unique Constraints)**:
> `ALTER TABLE reviews ADD CONSTRAINT uq_reviewer_teacher UNIQUE (reviewer_token, teacher_id);`
> (Ngăn chặn 1 người nhận xét 1 giảng viên nhiều lần)

### 4.4 Bảng `unlocked_teachers` (Giảng viên đã mở khóa)
Lưu trữ thông tin các giảng viên mà mỗi token đã dùng credit mở khóa.
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | ID tự tăng |
| `reviewer_token`| VARCHAR(50) | FK (`reviewers.token`) | Người mở khóa |
| `teacher_id` | BIGINT | FK (`teachers.id`) | Giảng viên được mở khóa |
| `unlocked_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời điểm mở khóa |

> **Ràng buộc duy nhất (Unique Constraints)**:
> `ALTER TABLE unlocked_teachers ADD CONSTRAINT uq_reviewer_unlock UNIQUE (reviewer_token, teacher_id);`

### 4.5 Bảng `admin_users` (Người quản trị)
| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | ID tự tăng |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | Tên tài khoản |
| `password_hash` | VARCHAR(255) | NOT NULL | Mật khẩu mã hóa BCrypt |
| `role` | VARCHAR(20) | Default `MODERATOR` | Quyền (ADMIN, MODERATOR) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

---

## 5. Cơ chế AI Auto-Moderation (Kiểm duyệt Tự động)

Để review của sinh viên được đăng ngay lập tức và họ nhận được credit xem luôn, hệ thống sử dụng cơ chế kiểm duyệt tự động 2 lớp:

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

### 5.1 Lớp 1: Keyword & Pattern Filter (Local, instant)
- **Từ cấm (Banned words)**: Chứa danh sách các từ tục tĩu, chửi thề tiếng Việt.
- **Pattern Matching (Regex)**:
  - Số điện thoại: Chặn spam số cá nhân `.*\b0[0-9]{9}\b.*`
  - Email: Chặn spam liên hệ cá nhân `.*\b[\w.-]+@[\w.-]+\.[a-z]{2,}\b.*`
  - Website link: Chặn spam link quảng cáo `.*https?://\S+.*`
- **Hành vi**: Nếu vi phạm bất kỳ bộ lọc nào, API lập tức từ chối (`400 Bad Request` kèm mã lỗi `CONTENT_VIOLATION`), không ghi nhận vào DB.

### 5.2 Lớp 2: Google Perspective API (AI check, ~200ms)
Hệ thống gọi API phân tích độ độc hại (Toxicity) của Google (Perspective API):
- **Toxicity Score < 0.7**: Đánh giá an toàn → Status tự động chuyển sang `APPROVED`, cộng ngay 1 credit cho token người gửi và hiển thị công khai.
- **Toxicity Score >= 0.7**: Đánh giá có yếu tố xúc phạm, xúc xiểm cá nhân → Status chuyển sang `FLAGGED`. Review tạm thời không được hiển thị công khai và chưa được cộng credit. Chuyển hồ sơ về hàng đợi quản trị viên duyệt thủ công.
- **Khi API lỗi hoặc timeout**: Fail-open → Đánh dấu là `APPROVED` để đảm bảo trải nghiệm sinh viên không bị gián đoạn, admin sẽ hậu kiểm sau thông qua các báo cáo (report) của người dùng.

---

## 6. Danh sách API Backend cần phát triển (REST API Specification)

Tất cả các API công khai của hệ thống bắt đầu bằng tiền tố `/api`. Trạng thái lỗi trả về dạng JSON chuẩn:
```json
{
  "status": 403,
  "error": "INSUFFICIENT_CREDIT",
  "message": "Bạn cần viết thêm review để mở khóa xem.",
  "timestamp": "2026-08-13T11:10:17Z"
}
```

### 6.1 Nhóm API Giảng viên (Teachers)
- **`GET /api/teachers`**
  - **Mục đích**: Lấy danh sách giảng viên phân trang, hỗ trợ tìm kiếm và lọc.
  - **Auth**: Public
  - **Query Params**:
    - `search` (String): Từ khóa tìm kiếm theo tên hoặc khoa.
    - `faculty` (String): Lọc theo khoa quản lý.
    - `minRating` (Number): Điểm trung bình tối thiểu (1.0 - 5.0).
    - `sortBy` (String): Tiêu chí xếp (name, rating, reviews).
    - `sortDir` (String): Hướng xếp (asc, desc).
    - `page` (Int): Trang hiện tại (0-based index).
    - `size` (Int): Số bản ghi trên trang (Default: 12).
  - **Response**: Trả về danh sách giảng viên kèm metadata phân trang.

- **`GET /api/teachers/{id}`**
  - **Mục đích**: Xem chi tiết 1 giảng viên và thống kê trắc nghiệm.
  - **Auth**: Public
  - **Response**: Trả về thông tin cơ bản + `multipleChoiceStats` và `ratingDistribution`.

- **`GET /api/teachers/{id}/reviews`**
  - **Mục đích**: Lấy danh sách nhận xét chi tiết của giảng viên.
  - **Auth**: Yêu cầu header `X-Reviewer-Token`.
  - **Cơ chế kiểm soát (Gate Control)**:
    1. Kiểm tra giảng viên này đã được unlock bởi token này chưa (`unlocked_teachers` table) hoặc người này có chính là tác giả của 1 review về giảng viên này không (`reviews` table). Nếu rồi → Trả về danh sách review (`200 OK`).
    2. Nếu chưa unlock, kiểm tra `credit_balance` của token này trong bảng `reviewers`.
       - Nếu `credit_balance >= 1`: Tự động trừ đi 1 credit của người dùng, thêm bản ghi vào `unlocked_teachers`, và trả về danh sách review (`200 OK`).
       - Nếu `credit_balance == 0`: Trả về lỗi `403 Forbidden` với mã lỗi `INSUFFICIENT_CREDIT`.
       - Nếu không tìm thấy token hoặc chưa truyền token: Trả về lỗi `403 Forbidden` với mã lỗi `NO_REVIEWER_TOKEN`.

### 6.2 Nhóm API Đánh giá (Reviews)
- **`POST /api/reviews`**
  - **Mục đích**: Gửi nhận xét mới về giảng viên.
  - **Auth**: Public (Client tự đính kèm `X-Reviewer-Token` nếu có).
  - **Cơ chế xử lý**:
    1. Verify reCAPTCHA token gửi kèm từ Google.
    2. Nếu client chưa có token trong header, backend tạo mới 1 `reviewer_token` (UUID) và lưu vào bảng `reviewers`.
    3. Kiểm tra xem token này đã từng review giảng viên này chưa. Nếu rồi → `400 Bad Request` (`DUPLICATE_REVIEW`).
    4. Chạy dịch vụ AI Moderation:
       - Nếu APPROVED: Lưu review với status `APPROVED`, cộng 1 credit cho reviewer, tạo bản ghi auto-unlock giảng viên được review, tính toán lại `avg_rating` của giảng viên. Trả về token và credit mới.
       - Nếu FLAGGED: Lưu review với status `FLAGGED`. Trả về thông tin review kèm thông báo chờ duyệt.
       - Nếu REJECTED: Trả về lỗi `400 Bad Request` (`CONTENT_VIOLATION`).

- **`POST /api/reviews/{id}/vote`**
  - **Mục đích**: Đồng tình/phản đối review.
  - **Auth**: Yêu cầu header `X-Reviewer-Token`.
  - **Body**: `{ "voteType": "UPVOTE" | "DOWNVOTE" }`

- **`POST /api/reviews/{id}/report`**
  - **Mục đích**: Báo cáo review vi phạm nội dung lên hệ thống.
  - **Auth**: Yêu cầu header `X-Reviewer-Token`.
  - **Body**: `{ "reason": "...", "description": "..." }`

### 6.3 Nhóm API Cổng kiểm soát (Gate Status)
- **`GET /api/gate/status`**
  - **Mục đích**: Lấy số dư credit và danh sách các giảng viên đã unlock của token hiện tại để đồng bộ giao diện.
  - **Auth**: Yêu cầu header `X-Reviewer-Token`.

---

## 7. Các biện pháp chống Spam và Bảo mật (Security & Anti-Spam Strategy)

Do dự án cho phép hoạt động ẩn danh không cần tài khoản, việc chống phá hoại dữ liệu (spam) được đặt lên hàng đầu:

1. **reCAPTCHA v3**: Tất cả các hành động ghi dữ liệu (`POST /api/reviews`, `/vote`, `/report`) bắt buộc phải được verify token reCAPTCHA ở backend trước khi thực hiện logic nghiệp vụ.
2. **Rate Limiting theo IP**: Giới hạn mỗi địa chỉ IP chỉ được phép gửi tối đa 3 reviews/ngày để chống spam bot.
3. **Mã hóa địa chỉ IP**: Không lưu trực tiếp địa chỉ IP của sinh viên để tránh lộ thông tin nhạy cảm. Lưu trữ dạng mã hóa một chiều SHA-256 (`ip_hash`).
4. **Unique Database Key**: Ràng buộc duy nhất `uq_reviewer_teacher` ngăn chặn việc spam rating ảo cho một giảng viên cụ thể từ một máy duy nhất.
5. **Profanity & AI Moderation**: Tránh tối đa các từ ngữ thô tục, bôi nhọ giảng viên làm ảnh hưởng uy tín dự án.
