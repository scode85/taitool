# An Tâm 0.2.0

Ứng dụng Android 8+ với giao diện mới tông kem/xanh ngọc, icon khiên–chiếc lá, ghi chú, nhắc lịch, thống kê sử dụng và nhắc nghỉ/che ứng dụng được chọn.

## Tải và cập nhật bằng Termux

```sh
pkg install curl -y
curl -fL -o ~/AnTam.apk https://raw.githubusercontent.com/scode85/taitool/main/an-tam/AnTam-0.2.0.apk
termux-open --view --content-type application/vnd.android.package-archive ~/AnTam.apk
```

Cài đè lên bản 0.1.0 để giữ dữ liệu. Bản 0.2.0 giữ nguyên tên gói `vn.phonecare` và chứng chỉ ký, tăng versionCode lên 2. Không gỡ bản cũ trước khi cập nhật.

SHA-256 của APK: `5ece63141b52528af8aef79eafdf81759c28f385307043cf8db13b21a4d97d80`

## Thay đổi

- Bố cục mới với các thẻ, thanh điều hướng có biểu tượng, màn hình trống có hướng dẫn.
- Icon launcher adaptive, icon theo màu hệ thống Android 13+, icon thông báo và nhận diện trong ứng dụng.
- Màn hình nghỉ có đếm ngược mm:ss và gợi ý nghỉ ngơi.
- Trạng thái theo dõi và quyền dễ xem, giữ bản nháp thời gian khi quay về từ Cài đặt.
- Xác nhận trước khi xóa ghi chú/lịch.

## Sử dụng

Trong An Tâm, mở Nghỉ ngơi để cấp quyền sử dụng, thông báo và hiển thị trên ứng dụng khác. Chọn ứng dụng cần giới hạn, đặt thời gian, bật theo dõi. Quyền hiển thị chỉ cần khi bật chế độ che.

Đây là bản thử nghiệm, không phải phần mềm diệt virus. Không có quyền Internet. Android có thể trì hoãn lịch và dừng dịch vụ nền; mở ứng dụng để bật lại theo dõi sau khi khởi động máy. Chế độ nghỉ vẫn cho phép về Home, gọi điện và thu hồi quyền.

Đã biên dịch Java/DEX, đóng gói tài nguyên, kiểm tra căn chỉnh và chữ ký v2/v3, đối chiếu chứng chỉ với 0.1.0. Chưa chạy trên thiết bị hoặc trình giả lập.

[Mã nguồn và hướng dẫn](source/README.md). Không đưa khóa ký vào kho.
