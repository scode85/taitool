# An Tâm — Phone Care 0.2.0

Ứng dụng Android tiếng Việt, Android 8.0 (API 26) trở lên.

## Mới trong 0.2.0
- Giao diện kem/xanh ngọc, thẻ thống kê, điều hướng có biểu tượng và màn hình trống có hướng dẫn.
- Biểu tượng khiên–chiếc lá: launcher adaptive, themed icon Android 13+, biểu tượng thông báo và nhận diện trong ứng dụng.
- Màn hình nghỉ mới, đếm ngược mm:ss, trạng thái theo dõi và trạng thái quyền trực quan.
- Giữ bản nháp thời gian khi quay về từ Cài đặt; xác nhận trước khi xóa ghi chú/lịch.
- Bản cập nhật dùng cùng tên gói và khóa ký với 0.1.0: cài đè để giữ dữ liệu.

## Chức năng
- Kiểm tra điện thoại đã có khóa màn hình an toàn, mở cài đặt bảo mật.
- Tạo, sửa, xóa ghi chú lưu trong vùng dữ liệu riêng của ứng dụng.
- Đặt nhắc lịch một lần bằng ngày và giờ; xóa để hủy. Khôi phục lịch tương lai sau khi khởi động máy.
- Ước tính thời gian dùng từng ứng dụng hôm nay bằng UsageEvents.
- Chọn ứng dụng đã dùng hôm nay để giới hạn. Thời gian phiên cộng dồn giữa các ứng dụng được chọn, kể cả khi chuyển qua ứng dụng khác rồi quay lại.
- Cảnh báo trước một phút; nhắc nghỉ hoặc che ứng dụng đã chọn trong thời gian nghỉ.
- Tắt/khóa màn hình đủ thời gian nghỉ sẽ đặt lại phiên; dừng/bắt đầu theo dõi cũng đặt lại phiên.

## Cài đặt
1. Chuyển AnTam.apk sang điện thoại, mở và cho phép cài từ nguồn đó nếu Android yêu cầu.
2. Trong An Tâm, mở Nghỉ ngơi: cấp quyền truy cập dữ liệu sử dụng và thông báo.
3. Dùng game/ứng dụng muốn giới hạn một lần rồi quay về An Tâm để chọn ứng dụng đó.
4. Nếu muốn che ứng dụng khi hết giờ, cấp quyền hiển thị trên ứng dụng khác và bật công tắc tương ứng.
5. Nhập số phút sử dụng và số phút nghỉ, nhấn Bắt đầu theo dõi.
6. Có thể dừng theo dõi ngay trong An Tâm. Sau khi khởi động lại máy phải bật lại theo dõi thủ công.

## Giới hạn cần biết
- Đây là APK thử nghiệm ký bằng khóa phát triển, chưa phải bản phát hành Google Play.
- Không phải trình diệt virus, không quét mã độc hay khóa từ xa. Ghi chú chưa có khóa sinh trắc học riêng; dùng khóa màn hình để bảo vệ dữ liệu trên máy.
- Không yêu cầu quyền Internet; không truyền dữ liệu lên máy chủ; tắt sao lưu dữ liệu ứng dụng. Gỡ cài đặt sẽ xóa ghi chú và lịch đã lưu.
- Chế độ che màn hình mang tính tự nguyện: người dùng vẫn có thể dùng Home, thu hồi quyền, dừng hoặc gỡ ứng dụng. Không khóa toàn bộ máy. Một số ứng dụng bảo mật có thể không cho phép lớp phủ.
- Nhắc lịch dùng báo thức không chính xác; Android/tiết kiệm pin có thể trì hoãn thông báo. Không phù hợp với nhắc việc cần đúng từng giây.
- Hãng máy có thể dừng dịch vụ nền. Khi dịch vụ bị dừng, theo dõi dừng và cần bật lại. Phiên hiện tại không được lưu qua lần dịch vụ khởi động lại.
- Thống kê là ước tính dựa trên sự kiện Android; chia đôi màn hình, sự kiện thiếu hoặc ứng dụng mở từ trước nửa đêm có thể làm sai lệch.

## Biên dịch lại
Cần Java runtime 21, Python 3, zip, Android SDK platform 35 và build tools 35.0.0. Trình biên dịch Eclipse ECJ được kèm trong bản mã nguồn ZIP ở ecj.jar (Eclipse Public License 2.0; https://www.eclipse.org/legal/epl-2.0/).

Nếu lấy mã nguồn từ GitHub và chưa có `ecj.jar`, tải ECJ 3.40.0:
```sh
curl -fL -o ecj.jar https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar
```

Chạy:
```
ANDROID_SDK_ROOT=/duong/dan/sdk BUILD_TOOLS_PATH=/duong/dan/sdk/build-tools/35.0.0 ./build.sh
```
APK ở `build/AnTam.apk`. Build tạo khóa thử nghiệm cục bộ ở `build/local-test.jks`. Không sử dụng khóa/mật khẩu mẫu cho bản phát hành công khai.

## Kiểm tra đã thực hiện
- Biên dịch tất cả nguồn Java với Android API 35 và tạo DEX cho min API 26.
- Đóng gói, căn chỉnh APK và xác minh chữ ký v2/v3.
- Chưa chạy trên thiết bị hoặc trình giả lập trong môi trường này.

## Kiểm thử trên điện thoại
- Tạo/sửa/xóa ghi chú; đóng và mở lại ứng dụng để kiểm tra lưu dữ liệu.
- Cấp quyền thông báo, đặt lịch vài phút tới; kiểm tra thông báo, xóa lịch và khởi động lại máy để kiểm tra đặt lại lịch.
- Cấp quyền sử dụng; dùng ứng dụng và so sánh thống kê.
- Chọn một ứng dụng, đặt giới hạn 1 phút/nghỉ 1 phút; kiểm tra nhắc và chế độ che, đếm ngược, Home, cuộc gọi và dừng theo dõi.
- Kiểm tra khi từ chối quyền, thu hồi quyền, tắt màn hình, đổi ứng dụng và bật tiết kiệm pin.
