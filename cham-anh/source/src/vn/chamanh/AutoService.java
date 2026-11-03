package vn.chamanh;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicit user-started automation. All execution stops when the service disconnects. */
public class AutoService extends AccessibilityService {
 public static volatile AutoService instance;
 public static volatile boolean running,paused;
 public static volatile String status="Sẵn sàng";
 public static volatile long activeMacroId=-1;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private volatile long generation;
 private long visionEpoch;private boolean wasLocked;
 private Future<?> work;
 private WindowManager windows;
 private LinearLayout controls;
 private TextView statusView,pauseView;
 private WindowManager.LayoutParams controlParams;
 private boolean captureMode,screenshotPending,prepared,busy;
 private String foregroundPackage="",pointMode="";
 private ArrayList<Step> steps;
 private int stepIndex,repeats,intervalMs,timeoutMs,trialStep;
 private long cycle;
 private double threshold;
 private long startAt,stepDeadline,dueAt,sleepUntil;
 private RunClock clock;
 private static final long MAX_SCREEN_PIXELS=8000000L,MAX_TEMPLATE_PIXELS=2000000L,MAX_ALL_TEMPLATE_PIXELS=4000000L;
 private static final class Step {StepSpec spec;int[] pixels;int width,height;}
 @Override protected void onServiceConnected(){super.onServiceConnected();windows=getSystemService(WindowManager.class);instance=this;running=paused=false;activeMacroId=-1;status="Trợ năng đã kết nối · Sẵn sàng";}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){if(e.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||e.getPackageName()==null)return;String pkg=e.getPackageName().toString(),name=e.getClassName()==null?"":e.getClassName().toString();if(pkg.equals(getPackageName())&&!name.endsWith("MainActivity")&&!e.isFullScreen())return;foregroundPackage=pkg;}
 private boolean unlocked(){return getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();}
 private boolean protectedPackage(){return foregroundPackage.isEmpty()||foregroundPackage.equals(getPackageName())||foregroundPackage.equals("android")||foregroundPackage.equals("com.android.settings")||foregroundPackage.contains("permissioncontroller");}
 private boolean valid(long token){return running&&generation==token&&instance==this;}
 private int bounded(int n,int lo,int hi,String label){if(n<lo||n>hi)throw new IllegalArgumentException(label+" không hợp lệ");return n;}
 private long now(){long real=SystemClock.elapsedRealtime();boolean locked=!unlocked();if(locked&&!wasLocked)visionEpoch++;wasLocked=locked;clock.setPaused(paused||locked,real);return clock.now(real);}
 private void setStatus(String s){status=trialStep>0?"THỬ BƯỚC "+trialStep+" · "+s:s;if(statusView!=null)statusView.setText(status);}
 public void startMacro(JSONObject input){if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->startMacro(input));return;}stopInternal(null,false);try{
  JSONObject macro=new JSONObject(input.toString());trialStep=macro.optInt("trialStep",0);if(trialStep<0||trialStep>100)throw new IllegalArgumentException("Chỉ số bước thử không hợp lệ");JSONArray raw=macro.optJSONArray("steps");if(raw==null||raw.length()<1||raw.length()>100)throw new IllegalArgumentException("Kịch bản cần từ 1 đến 100 bước");
  if(trialStep>0&&raw.length()!=1)throw new IllegalArgumentException("Phiên thử chỉ được chứa một bước");repeats=LoopPolicy.limit(macro);intervalMs=bounded(macro.optInt("interval",1500),1000,60000,"Khoảng quét");timeoutMs=bounded(macro.optInt("timeout",15),1,120,"Thời gian chờ")*1000;threshold=bounded(macro.optInt("threshold",92),70,99,"Độ khớp")/100.0;
  long token=++generation;clock=new RunClock();running=true;paused=false;prepared=busy=false;activeMacroId=macro.optLong("id");foregroundPackage=getPackageName();cycle=stepIndex=0;stepDeadline=sleepUntil=0;startAt=now()+5000;dueAt=startAt;status=trialStep>0?"Thử bước "+trialStep+" sau 5 giây":"Chuẩn bị · Chạy sau 5 giây";
  if(!showControls(false)){stopInternal("Không hiển thị được nút DỪNG",true);return;}
  work=worker.submit(()->{try{ArrayList<Step> loaded=loadSteps(raw);main.post(()->{if(valid(token)){steps=loaded;prepared=true;}});}catch(Exception e){main.post(()->fail(token,e.getMessage()));}catch(OutOfMemoryError e){main.post(()->fail(token,"Ảnh mẫu quá lớn. Hãy cắt nhỏ hơn."));}});
  schedule(token,0);
 }catch(Exception e){stopInternal(e.getMessage(),true);}}
 private ArrayList<Step> loadSteps(JSONArray source)throws Exception {ArrayList<Step> loaded=new ArrayList<>();HashMap<String,Step> cache=new HashMap<>();long total=0;for(int i=0;i<source.length();i++){
  if(Thread.currentThread().isInterrupted())throw new InterruptedException();Step step=new Step();try{step.spec=StepSpec.from(source.optJSONObject(i));}catch(IllegalArgumentException e){throw new IllegalArgumentException("Bước "+(i+1)+": "+e.getMessage());}
  if(StepSpec.imageAction(step.spec.action)&&cache.containsKey(step.spec.image)){Step old=cache.get(step.spec.image);step.width=old.width;step.height=old.height;step.pixels=old.pixels;}else if(StepSpec.imageAction(step.spec.action)){File f=MacroStore.image(this,step.spec.image);BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),b);long size=(long)b.outWidth*b.outHeight;if(b.outWidth<8||b.outHeight<8||size>MAX_TEMPLATE_PIXELS||size<1)throw new IllegalArgumentException("Ảnh bước "+(i+1)+" thiếu hoặc quá lớn");total+=size;if(total>MAX_ALL_TEMPLATE_PIXELS)throw new IllegalArgumentException("Tổng ảnh mẫu vượt 4 megapixel");BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap bitmap=BitmapFactory.decodeFile(f.getPath(),o);if(bitmap==null)throw new IllegalArgumentException("Không đọc được ảnh mẫu");try{step.width=bitmap.getWidth();step.height=bitmap.getHeight();step.pixels=new int[step.width*step.height];bitmap.getPixels(step.pixels,0,step.width,0,0,step.width,step.height);}finally{bitmap.recycle();}if(!ImageMatcher.hasUsableDetail(step.pixels,step.width,step.height))throw new IllegalArgumentException("Ảnh bước "+(i+1)+" ít chi tiết");cache.put(step.spec.image,step);}
  if("launch_app".equals(step.spec.action)&&getPackageManager().getLaunchIntentForPackage(step.spec.packageName)==null)throw new IllegalArgumentException("Ứng dụng cần mở không còn trên máy");loaded.add(step);
 }return loaded;}
 private void schedule(long token,long ms){main.postDelayed(()->tick(token),ms);}
 private void tick(long token){if(!valid(token))return;long n=now();schedule(token,200);
  if(paused){setStatus("Đã tạm dừng · Bấm TIẾP để chạy");return;}if(!unlocked()){setStatus("Đang chờ mở khóa màn hình");return;}
  if(n<startAt){setStatus("Bắt đầu sau "+((startAt-n+999)/1000)+" giây");return;}if(!prepared){setStatus("Đang nạp kịch bản…");return;}if(busy)return;
  if(n<dueAt){setStatus("Chờ "+((dueAt-n+999)/1000)+" giây · Bước "+(stepIndex+1)+"/"+steps.size());return;}
  Step s=steps.get(stepIndex);StepSpec p=s.spec;String a=p.action;setStatus(LoopPolicy.progress(cycle,repeats)+" · Bước "+(stepIndex+1)+"/"+steps.size()+" · "+p.label);
  if((StepSpec.imageAction(a)||StepSpec.coordinateAction(a))&&protectedPackage()){setStatus("Mở ứng dụng đích để thực hiện bước "+(stepIndex+1));stepDeadline=0;return;}
  if(StepSpec.imageAction(a)){if(stepDeadline==0)stepDeadline=n+timeoutMs;if(n>=stepDeadline){fail(token,"Không thấy ảnh ở bước "+(stepIndex+1)+" trong thời gian chờ");return;}findImage(token,s);return;}
  if(StepSpec.coordinateAction(a)){android.util.DisplayMetrics dm=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(dm);if(!p.fitsScreen(dm.widthPixels,dm.heightPixels)){fail(token,"Tọa độ khác hướng hoặc tỷ lệ màn hình. Hãy chọn lại điểm.");return;}gesture(token,p,StepSpec.pixel(p.x,dm.widthPixels),StepSpec.pixel(p.y,dm.heightPixels),StepSpec.pixel(p.x2,dm.widthPixels),StepSpec.pixel(p.y2,dm.heightPixels));return;}
  if("sleep".equals(a)){if(sleepUntil==0)sleepUntil=n+p.waitMs;if(n<sleepUntil){setStatus("Chờ "+((sleepUntil-n+999)/1000)+" giây · Bước "+(stepIndex+1)+"/"+steps.size());return;}sleepUntil=0;advance(token,0);return;}
  if("launch_app".equals(a)){try{Intent launch=getPackageManager().getLaunchIntentForPackage(p.packageName);if(launch==null)throw new IllegalStateException();launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(launch);advance(token,0);}catch(Exception e){fail(token,"Không mở được ứng dụng ở bước "+(stepIndex+1));}return;}
  int global="home".equals(a)?GLOBAL_ACTION_HOME:"back".equals(a)?GLOBAL_ACTION_BACK:"notifications".equals(a)?GLOBAL_ACTION_NOTIFICATIONS:"quick_settings".equals(a)?GLOBAL_ACTION_QUICK_SETTINGS:"recents".equals(a)?GLOBAL_ACTION_RECENTS:0;
  if(global==0||!performGlobalAction(global)){fail(token,"Android không thực hiện được "+StepSpec.title(a));return;}advance(token,0);
 }
 private void findImage(long token,Step step){
  busy=screenshotPending=true;final int rotation=windows.getDefaultDisplay().getRotation();final String pkg=foregroundPackage;final long captured=SystemClock.elapsedRealtime();final long vision=visionEpoch;final Rect excluded=controlRect();
  try{takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
   @Override public void onSuccess(ScreenshotResult result){if(!valid(token)){result.getHardwareBuffer().close();return;}if(paused||vision!=visionEpoch){result.getHardwareBuffer().close();busy=screenshotPending=false;return;}final Bitmap image;try{image=copyScreenshot(result);}catch(Exception e){busy=screenshotPending=false;fail(token,e.getMessage());return;}catch(OutOfMemoryError e){busy=screenshotPending=false;fail(token,"Không đủ bộ nhớ để chụp màn hình");return;}
    work=worker.submit(()->{try{int w=image.getWidth(),h=image.getHeight();int[] pix=new int[w*h];image.getPixels(pix,0,w,0,0,w,h);image.recycle();if(excluded!=null)for(int y=Math.max(0,excluded.top);y<Math.min(h,excluded.bottom);y++)for(int x=Math.max(0,excluded.left);x<Math.min(w,excluded.right);x++)pix[y*w+x]=Color.MAGENTA;ImageMatcher.Result match=ImageMatcher.find(pix,w,h,step.pixels,step.width,step.height,threshold);
     main.post(()->{if(!valid(token))return;busy=screenshotPending=false;if(paused||vision!=visionEpoch||!unlocked()||protectedPackage()||!pkg.equals(foregroundPackage)||rotation!=windows.getDefaultDisplay().getRotation()||SystemClock.elapsedRealtime()-captured>2500)return;if(match==null||(excluded!=null&&excluded.contains(match.centerX,match.centerY))){dueAt=now()+intervalMs;return;}if(now()>=stepDeadline){fail(token,"Hết thời gian chờ ảnh");return;}if("wait".equals(step.spec.action))advance(token,0);else gesture(token,step.spec,match.centerX,match.centerY,match.centerX,match.centerY);});
    }catch(Exception e){if(!image.isRecycled())image.recycle();main.post(()->fail(token,"Không xử lý được ảnh màn hình"));}catch(OutOfMemoryError e){if(!image.isRecycled())image.recycle();main.post(()->fail(token,"Không đủ bộ nhớ nhận diện ảnh"));}});
   }
   @Override public void onFailure(int error){if(!valid(token))return;busy=screenshotPending=false;if(error==ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT){dueAt=now()+1000;return;}fail(token,"Không chụp được màn hình ("+error+"). Ứng dụng có thể chặn ảnh chụp.");}
  });}catch(Exception e){busy=screenshotPending=false;fail(token,"Chưa có quyền chụp màn hình qua Trợ năng");}
 }
 private void gesture(long token,StepSpec s,int x,int y,int x2,int y2){if(!valid(token)||paused||!unlocked())return;Rect overlay=controlRect();if(overlay!=null){for(int i=0;i<=100;i++){int tx=x+(x2-x)*i/100,ty=y+(y2-y)*i/100;if(overlay.contains(tx,ty)){fail(token,"Thanh nổi đang che thao tác. Kéo thanh nổi ra ngoài vùng chạm rồi chạy lại.");return;}}}
  Path path=new Path();path.moveTo(x,y);boolean swipe="swipe".equals(s.action);if(swipe)path.lineTo(x2,y2);boolean twice=s.action.contains("double_tap");int duration=swipe||s.action.contains("long_press")?s.duration:70;GestureDescription.Builder builder=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,duration));if(twice)builder.addStroke(new GestureDescription.StrokeDescription(path,180,70));busy=true;
  try{boolean ok=dispatchGesture(builder.build(),new GestureResultCallback(){@Override public void onCompleted(GestureDescription g){if(valid(token)){busy=false;advance(token,0);}}@Override public void onCancelled(GestureDescription g){if(valid(token))fail(token,"Cử chỉ bị hủy. Kịch bản đã dừng.");}},main);if(!ok)fail(token,"Android từ chối thao tác chạm");}catch(Exception e){fail(token,"Tọa độ hoặc cử chỉ không hợp lệ");}
 }
 private void advance(long token,long extra){if(!valid(token))return;int delay=steps.get(stepIndex).spec.delay;stepIndex++;stepDeadline=sleepUntil=0;if(stepIndex>=steps.size()){stepIndex=0;cycle=LoopPolicy.next(cycle);if(LoopPolicy.finished(cycle,repeats)){stopInternal(trialStep>0?"Đã thực hiện thử bước "+trialStep:"Hoàn thành "+repeats+" vòng",true);return;}delay=Math.max(delay,intervalMs);}dueAt=now()+extra+Math.max(250,delay);}
 public void setPaused(boolean value){if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->setPaused(value));return;}if(!running)return;paused=value;visionEpoch++;now();if(pauseView!=null)pauseView.setText(paused?"TIẾP":"TẠM DỪNG");setStatus(paused?"Đã tạm dừng":"Tiếp tục kịch bản");}
 public void stopMacro(String reason){if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->stopMacro(reason));return;}stopInternal(reason==null?"Đã dừng":reason,true);}
 private void stopInternal(String reason,boolean toast){++generation;trialStep=0;running=paused=false;captureMode=screenshotPending=prepared=busy=false;activeMacroId=-1;main.removeCallbacksAndMessages(null);if(work!=null){work.cancel(true);work=null;}steps=null;removeControls();if(reason!=null&&!reason.isEmpty()){status=reason;if(toast)Toast.makeText(this,reason,Toast.LENGTH_LONG).show();}}
 private void fail(long token,String reason){if(valid(token)){String message=reason==null?"Không thực hiện được kịch bản":reason;stopInternal(trialStep>0?"Thử bước "+trialStep+": "+message:message,true);}}
 private Bitmap copyScreenshot(ScreenshotResult screenshot){HardwareBuffer buffer=screenshot.getHardwareBuffer();Bitmap hardware=null;try{if((long)buffer.getWidth()*buffer.getHeight()>MAX_SCREEN_PIXELS)throw new IllegalArgumentException("Màn hình vượt 8 megapixel");hardware=Bitmap.wrapHardwareBuffer(buffer,screenshot.getColorSpace());if(hardware==null)throw new IllegalStateException("Không đọc được ảnh màn hình");Bitmap copy=hardware.copy(Bitmap.Config.ARGB_8888,false);if(copy==null)throw new IllegalStateException("Không sao chép được ảnh");return copy;}finally{if(hardware!=null)hardware.recycle();buffer.close();}}
    public void showCaptureTool(){
        if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->showCaptureTool());return;}
        stopInternal(null,false);pointMode="";MacroStore.prefs(this).edit().putString("pointMode","").apply();captureMode=true;
        status="Mở màn hình mẫu rồi bấm Chụp";
        if(!showControls(true)){captureMode=false;status="Không mở được công cụ chụp";Toast.makeText(this,status,Toast.LENGTH_LONG).show();}
    }

    public void showPointTool(String mode){showCaptureTool();pointMode=mode;MacroStore.prefs(this).edit().putString("pointMode",mode).apply();}

    private void capture(){
        if(!captureMode||screenshotPending)return;
        if(!unlocked()||protectedPackage()){Toast.makeText(this,"Hãy mở ứng dụng có hình ảnh cần chọn trước",Toast.LENGTH_LONG).show();return;}
        final long token=generation;screenshotPending=true;removeControls();
        main.postDelayed(()->{
            if(!captureMode||generation!=token)return;
            try{takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult screenshot){
                    if(!captureMode||generation!=token){screenshot.getHardwareBuffer().close();return;}
                    final Bitmap bitmap;
                    try{bitmap=copyScreenshot(screenshot);}catch(Exception e){captureError(token,"Không đọc được ảnh màn hình");return;}
                    catch(OutOfMemoryError e){captureError(token,"Màn hình quá lớn để chụp mẫu");return;}
                    work=worker.submit(()->{
                        try{
                            File temporary=new File(getFilesDir(),"capture.tmp");
                            try(FileOutputStream out=new FileOutputStream(temporary)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IllegalStateException();}
                            bitmap.recycle();
                            if(Thread.currentThread().isInterrupted()){temporary.delete();return;}
                            File output=new File(getFilesDir(),"capture.png");
                            if(!temporary.renameTo(output))throw new IllegalStateException();
                            main.post(()->{
                                if(!captureMode||generation!=token)return;
                                captureMode=false;screenshotPending=false;status="Đã chụp mẫu · Chọn vùng ảnh";
                                MacroStore.prefs(AutoService.this).edit().putBoolean("captureReady",true).putString("pointMode",pointMode).apply();
                                Intent intent=new Intent().setClassName(getPackageName(),getPackageName()+".MainActivity").putExtra("capture",true).putExtra("pointMode",pointMode).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                try{startActivity(intent);}catch(Exception e){Toast.makeText(AutoService.this,"Mở Chạm Ảnh để chọn vùng mẫu đã chụp",Toast.LENGTH_LONG).show();}
                            });
                        }catch(Exception e){if(!bitmap.isRecycled())bitmap.recycle();main.post(()->captureError(token,"Không lưu được ảnh mẫu"));}
                    });
                }
                @Override public void onFailure(int error){captureError(token,"Không chụp được màn hình ("+error+"). Ứng dụng có thể chặn chụp ảnh.");}
            });}catch(Exception e){captureError(token,"Không có quyền chụp màn hình qua Trợ năng");}
        },350);
    }
    private void captureError(long token,String message){
        if(!captureMode||generation!=token)return;screenshotPending=false;status=message;Toast.makeText(this,message,Toast.LENGTH_LONG).show();showControls(true);
    }

    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView controlText(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);return t;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private boolean showControls(boolean capture){
        removeControls();if(windows==null)windows=getSystemService(WindowManager.class);
        controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);controls.setPadding(dp(12),dp(8),dp(8),dp(8));controls.setBackground(rounded(0xFF202946,18));controls.setElevation(dp(12));
        statusView=controlText(status,11,0xFFE8ECFF);statusView.setMaxLines(3);statusView.setContentDescription("Trạng thái; kéo để di chuyển thanh điều khiển");
        controls.addView(statusView,new LinearLayout.LayoutParams(0,-2,1));
        if(capture){TextView shoot=controlText("Chụp",13,0xFF2535A4);shoot.setGravity(Gravity.CENTER);shoot.setPadding(dp(12),dp(10),dp(12),dp(10));shoot.setMinHeight(dp(48));shoot.setBackground(rounded(0xFFE9EDFF,12));shoot.setOnClickListener(v->capture());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=dp(8);controls.addView(shoot,p);}
        if(!capture){pauseView=controlText(paused?"TIẾP":"TẠM DỪNG",11,Color.WHITE);pauseView.setGravity(Gravity.CENTER);pauseView.setPadding(dp(8),dp(10),dp(8),dp(10));pauseView.setMinHeight(dp(48));pauseView.setBackground(rounded(0xFF414A69,12));pauseView.setOnClickListener(v->setPaused(!paused));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-2,-2);pp.leftMargin=dp(6);controls.addView(pauseView,pp);}
        TextView stop=controlText(capture?"Đóng":"DỪNG",13,Color.WHITE);stop.setGravity(Gravity.CENTER);stop.setPadding(dp(13),dp(10),dp(13),dp(10));stop.setMinHeight(dp(48));stop.setBackground(rounded(capture?0xFF414A69:0xFFC74256,12));stop.setOnClickListener(v->stopMacro(capture?"Đã đóng công cụ chụp":"Đã dừng macro"));LinearLayout.LayoutParams stopLayout=new LinearLayout.LayoutParams(-2,-2);stopLayout.leftMargin=dp(8);controls.addView(stop,stopLayout);
        int width=Math.min(dp(360),getResources().getDisplayMetrics().widthPixels-dp(24));
        controlParams=new WindowManager.LayoutParams(width,-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);controlParams.gravity=Gravity.TOP|Gravity.LEFT;controlParams.x=dp(12);controlParams.y=dp(48);
        statusView.setOnTouchListener(new View.OnTouchListener(){float downX,downY;int startX,startY;public boolean onTouch(View v,MotionEvent event){
            if(controlParams==null||controls==null)return false;
            if(event.getAction()==MotionEvent.ACTION_DOWN){downX=event.getRawX();downY=event.getRawY();startX=controlParams.x;startY=controlParams.y;return true;}
            if(event.getAction()==MotionEvent.ACTION_MOVE){int screenW=getResources().getDisplayMetrics().widthPixels,screenH=getResources().getDisplayMetrics().heightPixels;controlParams.x=Math.max(0,Math.min(screenW-controls.getWidth(),startX+Math.round(event.getRawX()-downX)));controlParams.y=Math.max(0,Math.min(screenH-controls.getHeight(),startY+Math.round(event.getRawY()-downY)));try{windows.updateViewLayout(controls,controlParams);}catch(Exception ignored){}return true;}
            return true;
        }});
        try{windows.addView(controls,controlParams);return true;}catch(Exception e){controls=null;statusView=null;pauseView=null;controlParams=null;return false;}
    }
    private Rect controlRect(){if(controls==null)return null;int[] xy=new int[2];controls.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+controls.getWidth(),xy[1]+controls.getHeight());}
    private void removeControls(){if(controls!=null){try{windows.removeView(controls);}catch(Exception ignored){}controls=null;statusView=null;pauseView=null;controlParams=null;}}
    @Override public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);
        if(controls!=null&&controlParams!=null){
            int screenW=getResources().getDisplayMetrics().widthPixels,screenH=getResources().getDisplayMetrics().heightPixels;
            controlParams.width=Math.min(dp(360),screenW-dp(24));
            controlParams.x=Math.max(0,Math.min(controlParams.x,screenW-controlParams.width));
            controlParams.y=Math.max(0,Math.min(controlParams.y,screenH-Math.max(dp(90),controls.getHeight())));
            try{windows.updateViewLayout(controls,controlParams);}catch(Exception e){stopMacro("Không duy trì được nút dừng. Macro đã dừng.");}
        }
    }
    @Override public boolean onUnbind(Intent intent){stopInternal(null,false);if(instance==this)instance=null;status="Trợ năng đã tắt";return super.onUnbind(intent);}
    @Override public void onInterrupt(){stopMacro("Trợ năng bị gián đoạn · Macro đã dừng");}
    @Override public void onDestroy(){stopInternal(null,false);worker.shutdownNow();if(instance==this)instance=null;status="Trợ năng đã tắt";super.onDestroy();}
}
