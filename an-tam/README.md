# An Tâm 0.1.0

APK thử nghiệm cho Android 8 trở lên: ghi chú, nhắc lịch, thống kê sử dụng và nhắc nghỉ/che ứng dụng được chọn. Chưa kiểm thử trên điện thoại thật. Không phải phần mềm diệt virus.

Tải và cài bằng Termux:

```sh
curl -fL -o AnTam.apk https://raw.githubusercontent.com/scode85/taitool/main/an-tam/AnTam.apk
termux-open --view --content-type application/vnd.android.package-archive AnTam.apk
```

SHA-256: `f462b924a00f80124cba5a4d85935d3a3452153460889257a0e884d7ceb88037`

Trong ứng dụng, mở Nghỉ ngơi để cấp quyền dữ liệu sử dụng, thông báo và hiển thị trên ứng dụng khác; chọn ứng dụng cần giới hạn và bắt đầu theo dõi. Có thể dừng theo dõi hoặc thu hồi quyền bất cứ lúc nào. Cần bật lại theo dõi sau khi khởi động máy. Nhắc lịch có thể trễ do Android tiết kiệm pin.
