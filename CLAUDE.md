# CLAUDE.md — UniReview Project Guidelines

Tài liệu này hướng dẫn các AI assistant hiểu cách phát triển, biên dịch, chạy thử và tuân thủ quy trình phát triển cho dự án UniReview.

---

## 🛠️ Stack & Commands (Công nghệ & Lệnh)

### Frontend (React + Vite)
- **Directory**: `unireview/`
- **Commands**:
  - Khởi chạy dev server: `npm run dev`
  - Build phiên bản production: `npm run build`
  - Xem trước bản build: `npm run preview`

### Backend (Spring Boot 3 + Java 17 + Maven)
- **Directory**: `unireview-backend/`
- **Commands**:
  - Chạy ứng dụng: `.\mvnw.cmd spring-boot:run`
  - Biên dịch: `.\mvnw.cmd compile`
  - Đóng gói JAR: `.\mvnw.cmd clean package -DskipTests`
  - Khởi động Database: `docker-compose up -d`
  - Dừng Database: `docker-compose down`

---

## 🔄 Quy trình Phát triển: Speckit Workflow Rules
Mọi AI khi làm việc trên dự án này bắt buộc phải tuân thủ nghiêm ngặt quy trình **Speckit** qua các bước sau:

1. **Domain spec / speckit-specify**: Đặc tả kỹ thuật rõ ràng trước khi code.
2. **speckit-clarify**: Chủ động đặt câu hỏi làm rõ các điểm mơ hồ về nghiệp vụ với người dùng.
3. **speckit-play**: Thử nghiệm nhỏ hoặc dùng Mock DB trước khi viết code thật nếu chưa chắc chắn.
4. **speckit-tasks**: Luôn cập nhật danh sách việc cần làm vào file `task.md` trước khi triển khai.
5. **speckit-analyze**: Phân tích cấu trúc file và kiến trúc hiện tại để tránh viết code trùng lặp.
6. **speckit-implement**: Tiến hành triển khai code một cách sạch sẽ, mô-đun hóa.
7. **speckit-converge**: Chạy build thử nghiệm, chạy test và sửa toàn bộ lỗi biên dịch cho đến khi sạch lỗi 100%.

---

## 🎨 Quy tắc Thiết kế & Viết Code (Coding & Design Rules)

### Frontend (React)
- **Aesthetic**: Minimalist Light Theme (Sạch sẽ, độ tương phản cao, phẳng, không màu mè hoặc gradient nặng).
- **CSS**: Dùng biến CSS (`var(--color-...)`) từ [index.css](file:///d:/ReviewTeacher/unireview/src/index.css) để đồng bộ màu sắc.
- **JSX**: Các file chứa JSX bắt buộc phải có phần mở rộng là `.jsx` (không dùng `.js`).

### Backend (Spring Boot)
- **Architecture**: Mô hình 3 lớp Layered Architecture (`Controller` -> `Service` -> `Repository`).
- **Database**: Sử dụng PostgreSQL 18. Quản lý schema bằng Flyway migrations tại `src/main/resources/db/migration`.
- **Error Handling**: Sử dụng `@RestControllerAdvice` trả về JSON `ErrorResponse` thống nhất.
