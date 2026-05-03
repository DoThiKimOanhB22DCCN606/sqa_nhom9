# Communications Service

Dịch vụ thông báo và lịch phỏng vấn cho hệ thống tuyển dụng.

## Tính năng chính

### 1. Thông báo (Notifications)

- Gửi email, SMS, thông báo trong ứng dụng
- Template thông báo có thể tùy chỉnh
- Gửi hàng loạt
- Theo dõi trạng thái gửi

### 2. Lịch phỏng vấn (Interviews)

- Tạo, cập nhật, hủy lịch phỏng vấn
- Quản lý người tham gia
- Gửi lời mời và nhắc nhở
- Hỗ trợ nhiều loại phỏng vấn (phone, video, in-person)

### 3. Email Service

- Gửi email đơn giản và HTML
- Template engine với Thymeleaf
- Gửi hàng loạt
- Tích hợp với Gmail SMTP

### 4. SMS Service

- Gửi SMS qua Twilio
- Gửi hàng loạt
- Theo dõi trạng thái

## API Endpoints

### Notifications

- `POST /api/notifications` - Tạo thông báo mới
- `POST /api/notifications/template` - Tạo thông báo từ template
- `POST /api/notifications/bulk` - Gửi thông báo hàng loạt
- `GET /api/notifications/recipient/{recipientId}/{recipientType}` - Lấy thông báo theo người nhận
- `PUT /api/notifications/{notificationId}/read` - Đánh dấu đã đọc

### Candidate Notifications

- `POST /api/candidate-notifications/new-candidate` - Thông báo ứng viên mới
- `POST /api/candidate-notifications/application-status` - Thông báo thay đổi trạng thái ứng tuyển
- `POST /api/candidate-notifications/interview-reminder` - Nhắc nhở phỏng vấn
- `GET /api/candidate-notifications/candidate/{candidateId}` - Lấy thông báo của ứng viên

### Interviews

- `POST /api/interviews` - Tạo lịch phỏng vấn
- `GET /api/interviews/{id}` - Lấy thông tin phỏng vấn
- `PUT /api/interviews/{id}` - Cập nhật lịch phỏng vấn
- `DELETE /api/interviews/{id}` - Xóa lịch phỏng vấn
- `POST /api/interviews/{id}/cancel` - Hủy phỏng vấn
- `POST /api/interviews/{id}/reschedule` - Đổi lịch phỏng vấn
- `GET /api/interviews/candidate/{candidateId}` - Lấy lịch theo ứng viên
- `GET /api/interviews/interviewer/{interviewerId}` - Lấy lịch theo người phỏng vấn
- `GET /api/interviews/upcoming` - Lấy lịch sắp tới

### Emails

- `POST /api/emails/send` - Gửi email đơn giản
- `POST /api/emails/send-html` - Gửi email HTML
- `POST /api/emails/send-template` - Gửi email từ template
- `POST /api/emails/bulk` - Gửi email hàng loạt
- `POST /api/emails/candidate-welcome` - Gửi email chào mừng ứng viên

## Cấu hình

### Database

- MySQL database: `communications_db`
- Port: 3306
- Username: root
- Password: password

### Email (Gmail)

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### SMS (Twilio)

```properties
twilio.account.sid=your-account-sid
twilio.auth.token=your-auth-token
twilio.phone.number=+1234567890
```

### Kafka

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=communications-service
```

## Ví dụ sử dụng

### 1. Thông báo ứng viên mới

```bash
curl -X POST http://localhost:8084/api/candidate-notifications/new-candidate \
  -H "Content-Type: application/json" \
  -d '{
    "candidateId": 1,
    "candidateName": "Nguyễn Văn A",
    "candidateEmail": "nguyenvana@email.com"
  }'
```

### 2. Tạo lịch phỏng vấn

```bash
curl -X POST http://localhost:8084/api/interviews \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Phỏng vấn vị trí Developer",
    "description": "Phỏng vấn kỹ thuật cho vị trí Java Developer",
    "interviewType": "TECHNICAL",
    "startTime": "2024-01-15T10:00:00",
    "endTime": "2024-01-15T11:00:00",
    "candidateId": 1,
    "interviewerId": 2,
    "jobPositionId": 1,
    "location": "Phòng họp A"
  }'
```

### 3. Gửi email hàng loạt

```bash
curl -X POST http://localhost:8084/api/emails/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "recipients": ["user1@email.com", "user2@email.com"],
    "subject": "Thông báo quan trọng",
    "message": "Nội dung thông báo"
  }'
```

## Templates

### Email Templates

- `interview-invitation.html` - Mời phỏng vấn
- `candidate-welcome.html` - Chào mừng ứng viên mới

### Template Variables

- `candidateName` - Tên ứng viên
- `jobTitle` - Tên vị trí
- `interviewDate` - Ngày phỏng vấn
- `interviewTime` - Giờ phỏng vấn
- `location` - Địa điểm
- `meetingLink` - Link phòng họp

## Monitoring

### Health Check

- `GET /actuator/health` - Kiểm tra sức khỏe service
- `GET /actuator/info` - Thông tin service
- `GET /actuator/metrics` - Metrics

### Logs

- Log level: DEBUG cho communications service
- Log level: DEBUG cho Spring Mail

## Dependencies chính

- Spring Boot 3.5.6
- Spring Mail (Gmail SMTP)
- Twilio SDK (SMS)
- Spring Kafka (Event-driven)
- Spring Quartz (Scheduling)
- Thymeleaf (Template engine)
- MySQL (Database)
- Flyway (Database migration)
- Resilience4j (Circuit breaker)
- Spring Validation


