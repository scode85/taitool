# Chạm Ảnh 0.1.0

Ứng dụng Android tự tìm ảnh mẫu trên màn hình rồi thực hiện thao tác chạm tại tâm ảnh. Đây là bản thử nghiệm độc lập với tên và giao diện riêng, có quy trình tạo macro ảnh tương tự các công cụ tự động hóa hình ảnh. Không thuộc Macrofy.

## Yêu cầu
- Android 11 (API 30) trở lên, không cần root.
- Người dùng tự bật dịch vụ **Chạm Ảnh — tự động chạm** trong Cài đặt → Trợ năng.
- Ứng dụng đích cho phép chụp màn hình. Không dùng được trên màn hình khóa hoặc ứng dụng bật bảo vệ ảnh chụp.
- Cần giữ cùng độ phân giải, hướng màn hình, tỷ lệ giao diện với lúc lấy mẫu.

## Tính năng bản đầu
- Tạo, đổi tên, xóa nhiều kịch bản; lưu trên máy.
- Chọn ảnh từ bộ nhớ hoặc dùng thanh nổi chụp màn hình ứng dụng đích.
- Kéo chọn vùng mẫu, tự từ chối ảnh quá ít chi tiết.
- Tối đa 20 bước/kịch bản; sắp xếp lại, đặt tên và xóa bước.
- Mỗi bước: tìm ảnh rồi chạm tâm, hoặc chỉ chờ thấy ảnh; độ trễ sau bước 0–10000 ms.
- Độ khớp 70–99%; khoảng quét 1000–60000 ms; chờ ảnh 1–120 giây/bước; lặp 1–1000 vòng.
- Đếm ngược 5 giây trước khi chạy để chuyển ứng dụng.
- Nút **DỪNG** nổi luôn hiện khi chạy, kéo phần chữ trạng thái để di chuyển.
- Dừng khi hết thời gian, lỗi chụp, thao tác bị hủy hoặc hoàn thành các vòng.
- Tạm chờ khi khóa máy hoặc mở chính Chạm Ảnh/cài đặt quyền.

## Cài bằng Termux vào Download
```
termux-setup-storage
pkg install curl -y
curl -fL -o ~/storage/downloads/ChamAnh.apk https://raw.githubusercontent.com/scode85/taitool/main/cham-anh/ChamAnh.apk
termux-open --view --content-type application/vnd.android.package-archive ~/storage/downloads/ChamAnh.apk
```

Sau khi chạy `termux-setup-storage`, cho phép truy cập bộ nhớ trước khi tải. Cho phép Termux cài ứng dụng nếu Android yêu cầu.

## Hướng dẫn sử dụng
1. Mở Chạm Ảnh → Tạo kịch bản mới.
2. Bật quyền Trợ năng bằng nút trong ứng dụng; đọc nội dung giải thích, mở Cài đặt, bật đúng dịch vụ Chạm Ảnh. Bật quyền không tự chạy macro.
3. **Chụp mẫu:** bấm Chụp mẫu, ứng dụng về nền. Mở ứng dụng đích, di chuyển thanh nổi khỏi nút muốn chụp, bấm Chụp. Thanh nổi tạm ẩn khi lấy mẫu; Chạm Ảnh mở lại để cắt vùng.
4. Hoặc **Chọn ảnh:** chọn ảnh chụp màn hình cùng thiết bị và đúng kích thước gốc. Kéo một hình chữ nhật quanh nút có chữ hoặc biểu tượng; bấm Lưu ảnh mẫu.
5. Thêm các bước tiếp theo. Sửa bước để đặt tên, độ trễ, chọn chỉ chờ ảnh hoặc chạm. Dùng ↑/↓ để đổi thứ tự.
6. Đặt số vòng, độ khớp và thời gian chờ; lưu cấu hình.
7. Bấm Chạy sau 5 giây, chuyển sang ứng dụng đích. Giữ thanh nổi không che vùng cần tìm. Bấm DỪNG để kết thúc.

Nếu Android 13+ chặn bật Trợ năng cho ứng dụng cài ngoài, hãy xem Thông tin ứng dụng → menu ⋮ → Cho phép cài đặt bị hạn chế (tên mục tùy hãng máy), sau đó quay lại Trợ năng để bật. Bạn luôn có thể thu hồi quyền bằng cách tắt dịch vụ trong Cài đặt.

## Giới hạn nhận diện
- Nhận diện ở cùng kích thước gốc; chưa có tự đổi tỷ lệ, OCR, nhận diện AI hay ghi lại thao tác.
- Điểm khớp đo tương đồng ảnh, không phải xác suất chắc chắn chạm đúng.
- Ảnh giống nhau ở nhiều vị trí có thể chọn vùng khác ý định. Hãy lấy mẫu có thêm chi tiết phân biệt và quan sát lần chạy đầu.
- Chọn vùng 12×12 đến 1024×1024 pixel; tổng mẫu của một macro tối đa 4 megapixel. Ảnh nguồn và màn hình tối đa 8 megapixel.
- Kéo thanh điều khiển khỏi vùng mục tiêu; vùng thanh nổi bị loại khỏi nhận diện.
- Chưa hỗ trợ nhấn giữ, vuốt, nhập văn bản, điều kiện rẽ nhánh, xuất/nhập kịch bản hay chạy khi mở khóa tự động.
- Thay đổi trang/hình ảnh ngay giữa lúc chụp và chạm có thể làm vị trí không còn đúng. Có kiểm tra app, hướng và tuổi ảnh trước chạm nhưng không loại trừ mọi thay đổi giao diện.
- Một số hãng máy có thể dừng dịch vụ Trợ năng hoặc hạn chế chụp màn hình. Cần bật lại thủ công; không tự chạy khi dịch vụ kết nối.

## Dữ liệu và quyền
- Trợ năng chỉ dùng để lấy ảnh màn hình và gửi cử chỉ; không bật đọc cây nội dung cửa sổ.
- Ảnh được xử lý tại thiết bị, không yêu cầu quyền Internet, không có máy chủ.
- Kịch bản, ảnh mẫu và ảnh chụp tạm nằm trong dữ liệu riêng của ứng dụng; gỡ cài đặt sẽ xóa dữ liệu. Sao lưu Android đã tắt.
- Dừng chạy không tự tắt quyền Trợ năng; có thể thu hồi quyền trong Cài đặt.
- APK ký bằng khóa thử nghiệm, chưa phải bản phát hành Google Play.

## Mã nguồn và biên dịch
Mã Java thuần, không phụ thuộc AndroidX hoặc thư viện nhận diện bên ngoài. Cần Android SDK platform35/build-tools35.0.0, Java runtime21, zip, Eclipse ECJ3.40.0.

Trong source ZIP đã kèm ecj.jar. Nếu lấy từ GitHub:
```
curl -fL -o ecj.jar https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar
```
ECJ: Eclipse Public License2.0, https://www.eclipse.org/legal/epl-2.0/.

```
ANDROID_SDK_ROOT=/duong/dan/sdk BUILD_TOOLS_PATH=/duong/dan/sdk/build-tools/35.0.0 ./build.sh
./tests/run.sh
```
APK ở build/ChamAnh.apk. Khóa thử nghiệm sinh cục bộ trong build/local-test.jks, không đưa lên GitHub. Giữ khóa ký nếu muốn phát hành cập nhật cài đè.

## Kiểm tra
- 26 kiểm tra thuật toán ảnh trên JVM: có/không có mẫu, nhiễu, biên, màu/texture sai, ảnh trong suốt, kích thước không hợp lệ, ngắt luồng và tọa độ sau bước tìm thô/tinh.
- Biên dịch Java, DEX minAPI30, tài nguyên Android; kiểm tra căn chỉnh, manifest và chữ ký APK.
- Chưa chạy APK trên điện thoại hoặc giả lập trong môi trường này.

Cần kiểm thử trên thiết bị thật: cấp/tắt Trợ năng, chụp-cắt ảnh, chọn ảnh từ bộ nhớ, chạm mục tiêu và chỉ chờ, nhiều bước/vòng, dừng nổi, hết giờ, khóa màn hình, xoay máy, mất quyền, ứng dụng chặn ảnh, chạm khi ứng dụng chuyển trang.
