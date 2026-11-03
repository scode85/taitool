package vn.chamanh;
import org.json.JSONObject;
/** Shared validation for the editor and runtime; legacy image steps remain valid. */
public final class StepSpec {
 public final String action,label,image,packageName;
 public final double x,y,x2,y2;
 public final int refWidth,refHeight,duration,waitMs,delay;
 private StepSpec(JSONObject o){
  action=o.optString("action","tap");label=o.optString("label",title(action));
  if(!known(action))throw new IllegalArgumentException("Loại bước không được hỗ trợ: "+action);
  image=o.optString("image","");packageName=o.optString("package","");
  delay=range(o,"delay",500,0,60000);
  duration=range(o,"duration",action.contains("long_press")?800:400,100,5000);
  waitMs="sleep".equals(action)?range(o,"waitMs",1000,100,600000):1000;
  refWidth=o.optInt("refWidth",0);refHeight=o.optInt("refHeight",0);
  x=coordinateAction(action)?unit(o,"x"):0;y=coordinateAction(action)?unit(o,"y"):0;
  x2="swipe".equals(action)?unit(o,"x2"):x;y2="swipe".equals(action)?unit(o,"y2"):y;
  if(imageAction(action)&&!image.matches("[A-Za-z0-9_-]+\\.png"))throw new IllegalArgumentException("Bước ảnh thiếu mẫu hợp lệ");
  if(coordinateAction(action)&&(refWidth<1||refHeight<1||refWidth>20000||refHeight>20000))throw new IllegalArgumentException("Thiếu kích thước màn hình lấy tọa độ");
  if("swipe".equals(action)&&Math.hypot(x2-x,y2-y)<.002)throw new IllegalArgumentException("Điểm đầu và cuối của vuốt phải khác nhau");
  if("launch_app".equals(action)&&!packageName.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))throw new IllegalArgumentException("Chưa chọn ứng dụng cần mở");
 }
 public static StepSpec from(JSONObject o){if(o==null)throw new IllegalArgumentException("Bước trống");return new StepSpec(o);}
 private static int range(JSONObject o,String k,int def,int min,int max){double d=o.optDouble(k,def);if(Double.isNaN(d)||Double.isInfinite(d)||d!=Math.floor(d)||d<min||d>max)throw new IllegalArgumentException(k+" phải từ "+min+" đến "+max);return(int)d;}
 private static double unit(JSONObject o,String k){double n=o.optDouble(k,Double.NaN);if(Double.isNaN(n)||Double.isInfinite(n)||n<0||n>1)throw new IllegalArgumentException("Tọa độ "+k+" không hợp lệ");return n;}
 public static boolean imageAction(String a){return "tap".equals(a)||"wait".equals(a)||"image_long_press".equals(a)||"image_double_tap".equals(a);}
 public static boolean coordinateAction(String a){return "coordinate".equals(a)||"long_press".equals(a)||"double_tap".equals(a)||"swipe".equals(a);}
 public static boolean known(String a){return imageAction(a)||coordinateAction(a)||"home".equals(a)||"back".equals(a)||"notifications".equals(a)||"quick_settings".equals(a)||"recents".equals(a)||"sleep".equals(a)||"launch_app".equals(a);}
 public static String title(String a){switch(a){case"tap":return"Nhấn ảnh";case"wait":return"Chờ thấy ảnh";case"image_long_press":return"Nhấn giữ ảnh";case"image_double_tap":return"Nhấn đúp ảnh";case"coordinate":return"Nhấn tọa độ";case"long_press":return"Nhấn giữ";case"double_tap":return"Nhấn đúp";case"swipe":return"Vuốt màn hình";case"home":return"Về màn hình chính";case"back":return"Quay lại";case"notifications":return"Mở thanh thông báo";case"quick_settings":return"Mở cài đặt nhanh";case"recents":return"Ứng dụng gần đây";case"sleep":return"Chờ theo thời gian";case"launch_app":return"Mở ứng dụng";default:return"Thao tác";}}
 public static String symbol(String a){if(imageAction(a))return"▧";switch(a){case"coordinate":return"⊕";case"long_press":return"◎";case"double_tap":return"②";case"swipe":return"↗";case"home":return"⌂";case"back":return"↶";case"notifications":return"↓";case"quick_settings":return"☷";case"recents":return"▣";case"sleep":return"◷";case"launch_app":return"↗";default:return"＋";}}
 /** Source coordinates map proportionally, but do not cross orientations or aspect ratios. */
 public boolean fitsScreen(int width,int height){return !coordinateAction(action)||width>0&&height>0&&Math.abs((width/(double)height)/(refWidth/(double)refHeight)-1)<=.05;}
 public static int pixel(double unit,int length){if(length<1)throw new IllegalArgumentException("Kích thước màn hình không hợp lệ");return Math.max(0,Math.min(length-1,(int)Math.round(unit*(length-1))));}
}
