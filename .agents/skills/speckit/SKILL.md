---
name: speckit
description: Software engineering workflow skill implementing domain spec, specify, clarify, play, tasks, analyze, implement, and converge steps.
---

# Speckit Software Engineering Workflow Skill

Skill này hướng dẫn Antigravity (Gemini) thực hiện quy trình phát triển phần mềm chuẩn mực Speckit theo các bước tuần tự và chặt chẽ.

## 🔄 Các bước thực hiện trong chu trình Speckit

### 1. Domain spec / speckit-specify
- AI cần thu thập yêu cầu từ User, viết hoặc cập nhật tài liệu đặc tả kỹ thuật thiết kế kiến trúc và API.

### 2. speckit-clarify
- Trước khi thực hiện bất cứ chỉnh sửa lớn nào, AI phải hỏi lại người dùng để làm rõ các điểm mơ hồ về yêu cầu hoặc thiết kế.

### 3. speckit-play
- AI tiến hành viết các đoạn mã chạy thử nghiệm hoặc tạo mock data để thử nghiệm các ý tưởng kỹ thuật trước khi code thật.

### 4. speckit-tasks
- AI bắt buộc phải lập danh sách công việc cần làm chi tiết tại `task.md` ở thư mục lưu trữ của Agent.

### 5. speckit-analyze
- Phân tích cấu trúc thư mục và mã nguồn hiện có để đảm bảo tính nhất quán của thiết kế.

### 6. speckit-implement / implement
- Thực hiện viết code sạch, dễ bảo trì, tuân thủ các quy tắc thiết kế trong `CLAUDE.md`.

### 7. speckit-converge / converge
- Chạy thử nghiệm biên dịch dự án (ví dụ: `npm run build` hoặc `mvnw compile`), kiểm tra và sửa đổi code liên tục cho đến khi sạch lỗi 100%. Lặp lại vòng lặp này cho đến khi hệ thống ổn định hoàn toàn.
