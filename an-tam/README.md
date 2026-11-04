# An Tâm — Phone Care 0.2.1

Bản sửa theo dõi và chặn nghỉ, đặc biệt cho tình huống máy Android/iQOO làm chậm hoặc dừng dịch vụ nền. Android 8 trở lên. Không có quyền Internet.

## Sửa trong bản này
- Bỏ giới hạn cộng tối đa 2 giây mỗi lần kiểm tra: không còn tính thiếu chỉ vì lần kiểm tra bị Android trì hoãn.
- Nhận biết ứng dụng qua Trợ năng, đối chiếu với các sự kiện sử dụng mới hơn để tránh mắc kẹt ở tên ứng dụng cũ.
- Theo dõi riêng lớp Activity; việc đóng Activity cũ không xóa ứng dụng mới vừa mở.
- Thêm **Đưa về màn hình chính khi hết giờ** qua Trợ năng, kể cả khi game không cho lớp phủ thông thường hiển thị. Đây là về Home, không buộc dừng tiến trình ứng dụng.
- Lớp phủ nghỉ ưu tiên cửa sổ Trợ năng; nếu không có thì dùng quyền hiển thị trên ứng dụng khác.
- Lưu mốc nghỉ và bộ đếm mỗi 5 giây, khôi phục khi hệ thống khởi động lại dịch vụ. Không tự xóa giờ nghỉ đang còn hiệu lực.
- Không nuốt lỗi chặn trong im lặng; hiển thị trạng thái và lỗi quyền, cùng bộ đếm trực tiếp trong Nghỉ ngơi.
- Chọn ứng dụng từ danh sách launcher, không bắt buộc phải dùng trước trong ngày.
- Giữ ghi chú, nhắc lịch, giao diện và icon. Dùng cùng gói/chứng chỉ ký: cài đè để giữ dữ liệu.

## Cài bản mới bằng Termux vào Download
```sh
termux-setup-storage
curl -fL -o ~/storage/downloads/AnTam.apk https://raw.githubusercontent.com/scode85/taitool/main/an-tam/AnTam-0.2.1.apk
termux-open --view --content-type application/vnd.android.package-archive ~/storage/downloads/AnTam.apk
```
Cho phép bộ nhớ sau lệnh đầu. Cài đè, không gỡ bản cũ.

## Thiết lập trên iQOO Z9 / Android 16
1. Mở An Tâm → **Nghỉ ngơi**.
2. Bật **Trợ năng An Tâm** qua nút trong app: Cài đặt → Trợ năng → **An Tâm — nhắc nghỉ và về Home**. Dịch vụ này không đọc nội dung cửa sổ, không chụp màn hình, không gửi thao tác chạm. Nó nhận biết app và thực hiện Home/hiển thị màn hình nghỉ trong phiên bạn bật.
3. Nếu Android chặn quyền vì cài APK bên ngoài: Thông tin ứng dụng An Tâm → menu ⋮ → **Cho phép cài đặt bị hạn chế** (tên tùy hệ điều hành), rồi quay lại bật dịch vụ.
4. Cho phép thông báo. Cấp dữ liệu sử dụng để có thống kê và nguồn theo dõi dự phòng. Có thể cấp hiển thị trên ứng dụng khác để có thêm phương án che dự phòng.
5. Chọn đúng game/app cần giới hạn. Bật **Che ứng dụng khi đến giờ nghỉ** và **Đưa về màn hình chính khi hết giờ**. Nếu chỉ bật về Home thì không bắt buộc phải bật che.
6. Nhập số phút dùng/nghỉ, bấm **Bắt đầu theo dõi**. Bật Trợ năng chưa tự bắt đầu phiên; cần nhấn nút này.
7. Trong cài đặt pin/quản lý chạy nền của iQOO, cho phép An Tâm chạy nền và đặt **Không hạn chế / Cho phép sử dụng pin nền cao**, bật tự khởi động nếu máy có. Tên menu khác nhau giữa Funtouch OS và OriginOS. Có nút mở cài đặt pin trong ứng dụng.

## Kiểm tra trên máy trong một phút
- Đặt sử dụng **1 phút**, nghỉ **1 phút**, chọn một game và bật cả hai công tắc.
- Bấm Bắt đầu, mở game. Sau vài giây, về Nghỉ ngơi kiểm tra **Đã dùng** đang tăng; quay lại game.
- Đến hạn, app sẽ đưa về Home nếu quyền Trợ năng đang kết nối. Mở lại game trong giờ nghỉ sẽ bị đưa về Home lại. Nếu chỉ bật che, sẽ thấy lớp phủ đếm ngược.
- Mở An Tâm để xem trạng thái. Nếu báo chưa chặn được/thiếu quyền, kiểm tra hàng Trợ năng, quyền nền/pin và cấu hình chế độ trò chơi của iQOO.
- Nhấn Dừng theo dõi để kết thúc bất cứ lúc nào. Home, cài đặt hệ thống và ứng dụng gọi điện mặc định được loại khỏi chặn.

## Cách tính phiên
Cộng thời gian giữa các ứng dụng đã chọn, không đặt lại mỗi khi đổi app. Dùng đồng hồ đơn điệu; không cộng thời gian màn hình tắt. Tắt/khóa màn hình liên tục đủ thời gian nghỉ sẽ làm mới phiên. Cảnh báo khi còn khoảng một phút; đến hạn thì bắt đầu đủ thời gian nghỉ. Khi hạn rất ngắn hoặc lượt kiểm tra bị trễ có thể chuyển thẳng sang nghỉ.

Bắt đầu/lưu phiên mới đặt lại bộ đếm theo lựa chọn của bạn. Dịch vụ dùng foreground notification và đề nghị hệ thống khôi phục nếu bị thu hồi; không thể bảo đảm chạy khi máy buộc dừng app hoặc iQOO cấm hoạt động nền. Sau khởi động điện thoại cần mở app và bật theo dõi lại. Khoảng thời gian dịch vụ không thực sự chạy không được suy đoán thành thời gian dùng.

## Giới hạn
- Chưa kiểm thử trên iQOO/điện thoại thật trong môi trường này. Hãng máy và game vẫn có thể hạn chế lớp phủ hoặc dịch vụ; không thể cam kết chặn trên mọi máy.
- Về Home không đóng hẳn hay buộc dừng game. Người dùng có thể thu hồi quyền, dừng theo dõi hoặc gỡ ứng dụng.
- Tắt cả hai công tắc chặn là chế độ chỉ cảnh báo. Không phải trình diệt virus, không khóa từ xa, không phải quản trị thiết bị.
- Dữ liệu chỉ lưu trên máy, tắt sao lưu Android. Gỡ app sẽ xóa ghi chú và lịch. Ghi chú chưa có khóa sinh trắc học riêng.
- Nhắc lịch một lần có thể đến trễ do Android tiết kiệm pin; lịch tương lai được đặt lại khi máy khởi động.
- Thời gian sử dụng vẫn là ước tính; đa cửa sổ, sự kiện bị thiếu và chế độ nền đặc biệt có thể làm sai lệch.

## Biên dịch và kiểm tra
Cần Java runtime 21, Android SDK platform35/build-tools35.0.0, zip và ECJ3.40.0. Source ZIP kèm ecj.jar; nếu lấy từ GitHub:
```sh
curl -fL -o ecj.jar https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar
ANDROID_SDK_ROOT=/duong/dan/sdk BUILD_TOOLS_PATH=/duong/dan/sdk/build-tools/35.0.0 ./build.sh
./tests/run.sh
```
ECJ: Eclipse Public License2.0, https://www.eclipse.org/legal/epl-2.0/. APK thử nghiệm ký bằng khóa phát triển, không phải bản Google Play; giữ khóa cục bộ để cập nhật, không đưa khóa lên GitHub.

115 kiểm tra JVM đã qua (68 bộ đếm phiên, 27 phân giải Activity, 20 đối chiếu nguồn). Kiểm tra gồm bộ đếm phiên, lượt kiểm tra bị trì hoãn, hết giờ/nghỉ, chuyển app, tắt màn hình, khôi phục, nhận biết Activity và đối chiếu nguồn foreground. Biên dịch Java/DEX, kiểm tra tài nguyên/manifest, căn chỉnh và chữ ký APK; chứng chỉ khớp với bản trước.
