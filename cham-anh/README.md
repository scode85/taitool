# Chạm Ảnh 0.2.1

Ứng dụng Android tạo kịch bản tự động bằng các thẻ thao tác. Giao diện và tên riêng, có quy trình làm macro tương tự các công cụ tự động hóa; không thuộc Macrofy. Android 11 trở lên, không cần root.

## Mới trong 0.2.1: thử từng bước
Mỗi thẻ có nút **▶ Thử**. Chọn bước → xác nhận **Thử sau 5 giây** → chuyển sang ứng dụng đích. Phiên thử chỉ thực hiện hành động của bước đó đúng một lần, không chạy các bước khác và không dùng số vòng lặp của kịch bản. Thời gian quét, độ khớp và hạn chờ ảnh vẫn giữ theo kịch bản.

Thanh nổi ghi **THỬ BƯỚC n**, có tạm dừng/tiếp tục và dừng. Kết thúc hiển thị **Đã thực hiện thử bước n** hoặc nguyên nhân lỗi. Kịch bản đã lưu không bị biến thành một bước hoặc đổi số vòng lặp. Muốn thử thay đổi trong màn hình sửa, hãy lưu bước trước.

Đây là thao tác thật trên thiết bị, không phải mô phỏng. Thời gian nghỉ sau bước cuối không được chạy vì không có bước tiếp theo. Lệnh DỪNG ngăn các thao tác tiếp theo; một cử chỉ đã gửi cho Android có thể vẫn hoàn tất. Thông báo đã thực hiện nghĩa là Android đã chấp nhận/hoàn tất thao tác, không xác nhận kết quả bên trong ứng dụng đích.

## Thao tác hỗ trợ
- Nhấn ảnh, nhấn giữ ảnh, nhấn đúp ảnh, chỉ chờ thấy ảnh.
- Nhấn tọa độ, nhấn giữ, nhấn đúp: nhập X/Y hoặc chọn trực tiếp trên ảnh chụp màn hình ứng dụng đích.
- Vuốt: nhập hai điểm hoặc kéo đường vuốt trên ảnh chụp; đặt thời lượng 100–5000 ms.
- Về màn hình chính (Home), quay lại (Back).
- Mở thanh thông báo, cài đặt nhanh, danh sách ứng dụng gần đây.
- Mở một ứng dụng đã cài đặt từ danh sách ứng dụng có launcher.
- Chờ 100 ms đến 10 phút.

## Trình tạo kịch bản
- Tối đa 100 bước, 1–1000 vòng lặp; đặt thời gian nghỉ sau mỗi bước 0–60000 ms.
- Các thẻ có tên, biểu tượng, thông số và nút Sửa bước.
- Menu ⋯ để sao chép bước, chuyển lên/xuống, xóa bước.
- Bộ chọn Thêm thao tác chia thành các ô lớn, dễ tìm.
- Kịch bản mẫu Home → Chờ → Thông báo → Chờ → Quay lại để làm quen, không tự chạy.
- Thanh nổi có **TẠM DỪNG / TIẾP** và **DỪNG**; kéo phần chữ trạng thái để di chuyển.
- Đếm ngược 5 giây trước khi chạy. Thời gian chờ/đếm ngược dừng tính khi tạm dừng hoặc khóa màn hình.
- Kịch bản 0.1.0 tiếp tục dùng được. Cài đè APK để giữ dữ liệu, không gỡ bản cũ.

## Tải vào Download bằng Termux
```sh
termux-setup-storage
pkg install curl -y
curl -fL -o ~/storage/downloads/ChamAnh.apk https://raw.githubusercontent.com/scode85/taitool/main/cham-anh/ChamAnh-0.2.1.apk
termux-open --view --content-type application/vnd.android.package-archive ~/storage/downloads/ChamAnh.apk
```
Cho phép bộ nhớ sau lệnh đầu; cho phép Termux cài ứng dụng nếu Android yêu cầu.

## Dùng nhanh
1. Mở Chạm Ảnh, bật dịch vụ trong **Cài đặt → Trợ năng → Chạm Ảnh**. Bật quyền không tự chạy kịch bản.
2. Tạo kịch bản, bấm **Thêm thao tác**, chọn việc cần thực hiện.
3. **Nhấn ảnh:** chụp ảnh ứng dụng đích hoặc chọn từ bộ nhớ, cắt vùng có chữ/biểu tượng. Trong Sửa bước, chọn nhấn, giữ, đúp hoặc chỉ chờ ảnh.
4. **Tọa độ / Vuốt:** nhập số pixel hoặc bấm **Chọn trên ảnh màn hình**. Chuyển sang app đích, bấm Chụp trên thanh nổi. Chạm một điểm hoặc kéo đường vuốt trên ảnh; thao tác chọn điểm không gửi chạm thật. Xác nhận thông số rồi lưu bước.
5. **Home / Back / Thông báo / Cài đặt nhanh / Đa nhiệm:** thêm bước, đặt thời gian nghỉ và lưu. Mở ứng dụng thì chọn app trong danh sách.
6. Dùng ⋯ để sắp xếp và sao chép; đặt số vòng lặp và bấm Lưu thiết lập.
7. Bấm Chạy sau 5 giây. Chuyển sang app đích nếu kịch bản không có bước mở app. Thanh nổi luôn hiện khi chạy.
8. Tạm dừng để nghỉ, TIẾP để tiếp tục, DỪNG để kết thúc. Mỗi lần Chạy mới bắt đầu lại từ bước đầu.

## Ghi chú hoạt động
- Ảnh phải cùng tỷ lệ gốc; chưa hỗ trợ tự đổi tỷ lệ hoặc OCR.
- Tọa độ lưu theo vị trí tương đối và kích thước màn hình nguồn; từ chối khi đổi hướng/tỷ lệ quá 5%. Bố cục app vẫn phải phù hợp với khi chọn điểm.
- Thanh thông báo có thể được mở rồi thao tác bằng bước tọa độ/ảnh tiếp theo. Chụp màn hình phụ thuộc Android và hãng máy.
- Ảnh nguồn/màn hình tối đa 8 megapixel; vùng mẫu 12×12–1024×1024; tổng các mẫu khác nhau tối đa 4 megapixel.
- Kéo thanh nổi khỏi vùng thao tác. Ảnh ở vùng thanh nổi không được nhận diện; tọa độ/vuốt qua thanh nổi sẽ dừng và yêu cầu chọn lại vị trí thanh.
- Độ khớp là điểm tương đồng, không bảo đảm đúng 100%. Hình trùng lặp và chuyển trang nhanh có thể làm vị trí không như ý.
- Bước ảnh chờ 1–120 giây, quét mỗi 1000–60000 ms; hết thời gian thì dừng. Các bước hệ thống và tọa độ không cần có ảnh mẫu.
- Hệ thống có thể hủy cử chỉ đang diễn ra khi bạn chạm màn hình; lúc đó kịch bản dừng. Tạm dừng hiệu quả nhất giữa các bước.
- Ứng dụng bảo vệ ảnh chụp không hỗ trợ bước ảnh. Không gửi chạm tự động trong chính Chạm Ảnh hoặc màn hình cấp quyền/cài đặt; các bước Home/Back vẫn dùng để điều hướng ra ngoài.
- Chưa có OCR, nhập văn bản, rẽ nhánh, xuất/nhập kịch bản hoặc ghi toàn bộ cử chỉ tự động. Đây là APK thử nghiệm, chưa phải bản phát hành Google Play.

## Dữ liệu và quyền
Trợ năng được dùng để chụp màn hình, gửi cử chỉ và thực hiện điều hướng trong kịch bản bạn tạo. Không bật đọc cây nội dung cửa sổ, không có quyền Internet. Ảnh và kịch bản nằm trong vùng dữ liệu riêng trên máy; gỡ ứng dụng sẽ xóa dữ liệu, sao lưu Android đã tắt.

Nếu Android 13+ chặn bật Trợ năng cho ứng dụng cài ngoài, xem **Thông tin ứng dụng → menu ⋮ → Cho phép cài đặt bị hạn chế** (tên tùy hãng), sau đó quay về Trợ năng để bật. Có thể tắt dịch vụ để thu hồi quyền bất cứ lúc nào.

## Biên dịch
Java thuần và API Android35; Android SDK platform35/build-tools35.0.0, Java runtime21, zip, ECJ3.40.0.
```sh
curl -fL -o ecj.jar https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar
ANDROID_SDK_ROOT=/duong/dan/sdk BUILD_TOOLS_PATH=/duong/dan/sdk/build-tools/35.0.0 ./build.sh
```
ECJ được kèm trong source ZIP; Eclipse Public License2.0: https://www.eclipse.org/legal/epl-2.0/. Khóa thử nghiệm sinh cục bộ trong build/local-test.jks; không đưa khóa lên GitHub và cần giữ khóa để cập nhật cài đè.

## Kiểm tra
91 kiểm tra JVM đã qua: 26 về nhận diện ảnh, 42 về loại bước/tham số/đồng hồ và 23 về phiên thử từng bước, ảnh cũ, tính độc lập dữ liệu và loại trừ bước khác. Biên dịch APK, kiểm tra tài nguyên/manifest, căn chỉnh và chữ ký; đối chiếu chứng chỉ ký với bản cũ để hỗ trợ cập nhật giữ dữ liệu.

```sh
mkdir -p tests/lib
curl -fL -o tests/lib/json.jar https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar
./tests/run.sh
```
JSON-java là phụ thuộc chỉ dùng trong kiểm thử JVM (public domain), không đóng gói vào APK. Source ZIP đã kèm jar kiểm thử.

Chưa chạy trên điện thoại/giả lập trong môi trường này. Cần thử trên máy thật: từng thao tác hệ thống, nhập/chọn tọa độ, vuốt, ảnh, nhiều bước/vòng, tạm dừng khi đang chờ, nút DỪNG, khóa/đổi hướng màn hình, từ chối quyền và mở thanh thông báo theo hãng máy.
