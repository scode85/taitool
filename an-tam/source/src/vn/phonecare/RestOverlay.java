package vn.phonecare;
import android.content.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.os.SystemClock;import android.view.*;import android.widget.*;
/** Owns one rest window. A failed add never leaves a fake attached view behind. */
public final class RestOverlay {
 private final Context context;private final WindowManager windows;private final int type;
 private LinearLayout root;private TextView counter;private boolean attached;private String failure="";
 public RestOverlay(Context c,WindowManager w,int type){context=c;windows=w;this.type=type;}
 int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
 GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 TextView text(String value,int size,int color){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setLineSpacing(dp(3),1);return t;}
 void add(LinearLayout c,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);c.addView(v,p);}
 public boolean show(long untilElapsed){
  if(untilElapsed<=SystemClock.elapsedRealtime()){hide();return true;}
  if(!attached){
   LinearLayout next=new LinearLayout(context);next.setOrientation(1);next.setGravity(Gravity.CENTER);next.setPadding(dp(18),dp(26),dp(18),dp(26));next.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF123F3A,0xFF235D4F}));
   ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);LinearLayout stage=new LinearLayout(context);stage.setOrientation(1);stage.setGravity(Gravity.CENTER);scroll.addView(stage,new ScrollView.LayoutParams(-1,-2));
   LinearLayout card=new LinearLayout(context);card.setOrientation(1);card.setGravity(Gravity.CENTER_HORIZONTAL);card.setPadding(dp(22),dp(26),dp(22),dp(26));card.setBackground(shape(0xFFF6F7F3,26));int width=Math.max(dp(120),Math.min(context.getResources().getDisplayMetrics().widthPixels-dp(36),dp(420)));stage.addView(card,new LinearLayout.LayoutParams(width,-2));
   ImageView logo=new ImageView(context);logo.setImageResource(R.drawable.ic_brand);logo.setContentDescription("An Tâm");card.addView(logo,new LinearLayout.LayoutParams(dp(64),dp(64)));
   add(card,text("ĐÃ ĐẾN GIỜ NGHỈ",12,0xFF256451),18);TextView title=text("Chậm lại, thở nhẹ.",26,0xFF143F37);title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);add(card,title,14);
   counter=text("00:00",50,0xFF143F37);counter.setFontFeatureSettings("tnum");add(card,counter,22);add(card,text("THỜI GIAN NGHỈ CÒN LẠI",10,0xFF60746B),3);
   TextView tips=text("Nhìn xa · Đứng dậy · Uống nước",14,0xFF256451);tips.setPadding(dp(12),dp(14),dp(12),dp(14));tips.setBackground(shape(0xFFE2EFE7,16));add(card,tips,22);
   add(card,text("Nút Home vẫn hoạt động để gọi điện hoặc mở ứng dụng khác. Mở An Tâm để dừng theo dõi; có thể thu hồi quyền trong Cài đặt Android.",12,0xFF60746B),20);
   next.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
   WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
   try{windows.addView(next,p);root=next;attached=true;failure="";}catch(RuntimeException e){root=null;counter=null;attached=false;failure="Android từ chối hiển thị màn hình nghỉ. Kiểm tra quyền hoặc bật Trợ năng.";return false;}
  }
  long seconds=Math.max(0,(untilElapsed-SystemClock.elapsedRealtime()+999)/1000);if(counter!=null)counter.setText(String.format(java.util.Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60));return true;
 }
 public void hide(){if(attached&&root!=null){try{windows.removeViewImmediate(root);}catch(RuntimeException ignored){}}attached=false;root=null;counter=null;}
 public String error(){return failure;}
}
