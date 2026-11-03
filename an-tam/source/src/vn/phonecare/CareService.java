package vn.phonecare;
import android.app.*;import android.app.usage.*;import android.content.*;import android.graphics.PixelFormat;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;
public class CareService extends Service{
 static volatile boolean running=false;
 Handler handler=new Handler(Looper.getMainLooper());WindowManager wm;LinearLayout cover;TextView countdown;String active="";long lastPoll,used=0,restUntil=0;boolean warned=false;
 public void onCreate(){super.onCreate();wm=getSystemService(WindowManager.class);Data.channels(this);startForeground(1,Data.notice(this,"monitor","An Tâm đang theo dõi","Chạm để xem thời gian, điều chỉnh hoặc dừng theo dõi."));running=true;lastPoll=System.currentTimeMillis()-1500;handler.post(loop);}
 final Runnable loop=new Runnable(){public void run(){try{tick();}catch(Exception e){hide();}handler.postDelayed(this,1000);}};
 void tick(){long now=System.currentTimeMillis();if(!Usage.allowed(this)){hide();stopSelf();return;}UsageEvents events=getSystemService(UsageStatsManager.class).queryEvents(lastPoll,now);UsageEvents.Event e=new UsageEvents.Event();while(events.hasNextEvent()){events.getNextEvent(e);if(e.getEventType()==1)active=e.getPackageName();else if(e.getEventType()==2&&active.equals(e.getPackageName()))active="";}
 boolean screen=getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();boolean selected=screen&&Data.prefs(this).getStringSet("apps",java.util.Collections.emptySet()).contains(active);
 long step=Math.min(2000,Math.max(0,now-lastPoll));lastPoll=now;
 if(restUntil>now){if(selected&&Data.prefs(this).getBoolean("strict",false)&&Settings.canDrawOverlays(this)){show();long seconds=(restUntil-now+999)/1000;countdown.setText(String.format(java.util.Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60));}else hide();return;}
 hide();if(restUntil!=0){used=0;warned=false;restUntil=0;}
 if(!screen){if(used>0){long off=Data.prefs(this).getLong("off",now);if(!Data.prefs(this).contains("off"))Data.prefs(this).edit().putLong("off",now).apply();if(now-off>=Data.prefs(this).getInt("rest",5)*60000L){used=0;warned=false;}}return;}Data.prefs(this).edit().remove("off").apply();
 if(!selected)return;used+=step;long limit=Data.prefs(this).getInt("limit",30)*60000L;
 if(!warned&&used>=Math.max(0,limit-60000)){warned=true;notifyUser("Sắp đến giờ nghỉ","Bạn còn khoảng 1 phút trong phiên sử dụng này.");}
 if(used>=limit){restUntil=now+Data.prefs(this).getInt("rest",5)*60000L;notifyUser("Đến giờ nghỉ ngơi","Tạm dừng sử dụng ứng dụng đã chọn. Nghỉ "+Data.prefs(this).getInt("rest",5)+" phút.");}
 }
 void notifyUser(String t,String b){try{getSystemService(NotificationManager.class).notify(2,Data.notice(this,"remind",t,b));}catch(SecurityException e){}}
 int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 android.graphics.drawable.GradientDrawable rounded(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 TextView overlayText(String text,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setFontFeatureSettings("tnum");if(bold)t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));return t;}
 void spaced(LinearLayout parent,View child,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(child,p);}
 void show(){
  if(cover!=null)return;
  cover=new LinearLayout(this);cover.setOrientation(1);cover.setGravity(Gravity.CENTER);cover.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF123F3A,0xFF235D4F}));
  cover.setPadding(dp(24),dp(32),dp(24),dp(32));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
  LinearLayout stage=new LinearLayout(this);stage.setOrientation(1);stage.setGravity(Gravity.CENTER);scroll.addView(stage,new ScrollView.LayoutParams(-1,-1));
  int width=Math.min(getResources().getDisplayMetrics().widthPixels-dp(48),dp(420));
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setGravity(Gravity.CENTER_HORIZONTAL);card.setPadding(dp(24),dp(30),dp(24),dp(28));card.setBackground(rounded(0xFFF6F7F3,28));stage.addView(card,new LinearLayout.LayoutParams(width,-2));
  ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.ic_brand);brand.setContentDescription("An Tâm");card.addView(brand,new LinearLayout.LayoutParams(dp(68),dp(68)));
  TextView badge=overlayText("MỘT CHÚT NGHỈ NGƠI",11,0xFF256451,true);badge.setLetterSpacing(.12f);badge.setPadding(dp(14),dp(8),dp(14),dp(8));badge.setBackground(rounded(0xFFD9EEE5,20));LinearLayout.LayoutParams badgeParams=new LinearLayout.LayoutParams(-2,-2);badgeParams.topMargin=dp(22);card.addView(badge,badgeParams);
  spaced(card,overlayText("Chậm lại, thở nhẹ.",26,0xFF143F37,true),18);
  TextView subtitle=overlayText("Bạn đã dành đủ thời gian cho màn hình.\nGiờ là lúc chăm sóc bản thân.",14,0xFF60746B,false);subtitle.setLineSpacing(dp(3),1);spaced(card,subtitle,12);
  countdown=overlayText("00:00",52,0xFF143F37,true);spaced(card,countdown,24);
  TextView timerLabel=overlayText("THỜI GIAN NGHỈ CÒN LẠI",10,0xFF60746B,true);timerLabel.setLetterSpacing(.12f);spaced(card,timerLabel,2);
  LinearLayout tips=new LinearLayout(this);tips.setOrientation(1);tips.setPadding(dp(16),dp(14),dp(16),dp(14));tips.setBackground(rounded(0xFFE9EFE9,16));TextView tipText=overlayText("Nhìn xa  •  Đứng dậy  •  Uống nước",13,0xFF256451,false);tipText.setLineSpacing(dp(3),1);tips.addView(tipText,new LinearLayout.LayoutParams(-1,-2));spaced(card,tips,26);
  TextView hint=overlayText("Nhấn Home để gọi điện hoặc mở ứng dụng khác.\nBạn có thể dừng theo dõi trong An Tâm hoặc thu hồi quyền trong Cài đặt Android.",12,0xFF60746B,false);hint.setLineSpacing(dp(3),1);spaced(card,hint,20);
  cover.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);wm.addView(cover,p);
 }
 void hide(){if(cover!=null){wm.removeView(cover);cover=null;}}
 public int onStartCommand(Intent i,int flags,int id){return START_NOT_STICKY;}
 public IBinder onBind(Intent i){return null;}
 public void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);hide();super.onDestroy();}
}
