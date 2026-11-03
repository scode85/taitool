package vn.chamanh;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** User-started, on-device image macros. Nothing starts when accessibility is enabled. */
public class AutoService extends AccessibilityService {
    public static volatile AutoService instance;
    public static volatile boolean running;
    public static volatile String status="Sẵn sàng";
    public static volatile long activeMacroId=-1;

    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile long generation;
    private Future<?> work;
    private WindowManager windows;
    private LinearLayout controls;
    private TextView statusView;
    private WindowManager.LayoutParams controlParams;
    private boolean captureMode, screenshotPending, prepared;
    private String foregroundPackage="";
    private ArrayList<Step> steps;
    private int stepIndex,cycle,repeats,intervalMs,timeoutMs;
    private double threshold;
    private long startAt,stepDeadline;
    private static final long MAX_SCREEN_PIXELS=8000000L;
    private static final long MAX_TEMPLATE_PIXELS=2000000L;
    private static final long MAX_ALL_TEMPLATE_PIXELS=4000000L;

    private static final class Step {
        String label,action;
        int[] pixels;
        int width,height,delay;
    }

    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        windows=getSystemService(WindowManager.class);
        instance=this;
        running=false;
        activeMacroId=-1;
        status="Trợ năng đã kết nối · Chưa chạy";
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||event.getPackageName()==null)return;
        String pkg=event.getPackageName().toString();
        String name=event.getClassName()==null?"":event.getClassName().toString();
        // Our own accessibility toolbar must not be mistaken for the foreground Activity.
        if(pkg.equals(getPackageName())&&!name.endsWith("MainActivity")&&!event.isFullScreen())return;
        foregroundPackage=pkg;
    }

    public void startMacro(JSONObject input){
        if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->startMacro(input));return;}
        stopInternal(null,false);
        try{
            if(input==null)throw new IllegalArgumentException("Không tìm thấy macro");
            final JSONObject macro=new JSONObject(input.toString());
            JSONArray raw=macro.optJSONArray("steps");
            if(raw==null||raw.length()==0||raw.length()>20)throw new IllegalArgumentException("Macro cần từ 1 đến 20 bước");
            repeats=bounded(macro.optInt("repeat",1),1,1000,"Số vòng lặp");
            intervalMs=bounded(macro.optInt("interval",1500),1000,60000,"Khoảng kiểm tra");
            timeoutMs=bounded(macro.optInt("timeout",15),1,120,"Thời gian chờ")*1000;
            threshold=bounded(macro.optInt("threshold",92),70,99,"Độ khớp")/100.0;
            final long token=++generation;
            running=true;
            prepared=false;
            activeMacroId=macro.optLong("id",-1);
            foregroundPackage=getPackageName();
            cycle=0;stepIndex=0;stepDeadline=0;
            startAt=SystemClock.elapsedRealtime()+5000;
            status="Chuẩn bị · Mở ứng dụng đích trong 5 giây";
            if(!showControls(false)){stopInternal("Không hiển thị được nút dừng",true);return;}
            work=worker.submit(()->{
                try{
                    ArrayList<Step> loaded=loadSteps(macro.optJSONArray("steps"));
                    main.post(()->{if(!valid(token))return;steps=loaded;prepared=true;});
                }catch(Exception e){main.post(()->fail(token,e.getMessage()));}
                catch(OutOfMemoryError e){main.post(()->fail(token,"Ảnh mẫu quá lớn. Hãy cắt vùng nhỏ hơn."));}
            });
            later(token,100);
        }catch(Exception e){stopInternal(e.getMessage(),true);}
    }

    private int bounded(int value,int min,int max,String title){
        if(value<min||value>max)throw new IllegalArgumentException(title+" không hợp lệ");
        return value;
    }

    private ArrayList<Step> loadSteps(JSONArray source)throws Exception{
        ArrayList<Step> result=new ArrayList<>();
        long total=0;
        for(int i=0;i<source.length();i++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            JSONObject raw=source.getJSONObject(i);
            File file=MacroStore.image(this,raw.optString("image"));
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(file.getAbsolutePath(),bounds);
            long pixels=(long)bounds.outWidth*bounds.outHeight;
            if(bounds.outWidth<8||bounds.outHeight<8||pixels>MAX_TEMPLATE_PIXELS||pixels<1)
                throw new IllegalArgumentException("Ảnh bước "+(i+1)+" thiếu hoặc quá lớn. Chọn lại vùng mẫu.");
            total+=pixels;
            if(total>MAX_ALL_TEMPLATE_PIXELS)throw new IllegalArgumentException("Tổng ảnh mẫu quá lớn. Hãy cắt nhỏ các vùng mẫu.");
            BitmapFactory.Options opts=new BitmapFactory.Options();opts.inPreferredConfig=Bitmap.Config.ARGB_8888;
            Bitmap bitmap=BitmapFactory.decodeFile(file.getAbsolutePath(),opts);
            if(bitmap==null)throw new IllegalArgumentException("Không đọc được ảnh bước "+(i+1));
            Step step=new Step();
            try{
                step.width=bitmap.getWidth();step.height=bitmap.getHeight();
                step.pixels=new int[step.width*step.height];
                bitmap.getPixels(step.pixels,0,step.width,0,0,step.width,step.height);
            }finally{bitmap.recycle();}
            if(!ImageMatcher.hasUsableDetail(step.pixels,step.width,step.height))
                throw new IllegalArgumentException("Ảnh bước "+(i+1)+" ít chi tiết. Chọn vùng có chữ hoặc biểu tượng rõ.");
            step.label=raw.optString("label","Bước "+(i+1));
            step.action=raw.optString("action","tap");
            if(!"tap".equals(step.action)&&!"wait".equals(step.action))throw new IllegalArgumentException("Loại bước không hợp lệ");
            step.delay=bounded(raw.optInt("delay",500),0,10000,"Độ trễ bước");
            result.add(step);
        }
        return result;
    }

    private boolean valid(long token){return running&&generation==token&&instance==this;}
    private void later(long token,long delay){main.postDelayed(()->tick(token),delay);}
    private void setStatus(String message){status=message;if(statusView!=null)statusView.setText(message);}
    private boolean unlocked(){return getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();}
    private boolean protectedPackage(){
        return foregroundPackage.isEmpty()||foregroundPackage.equals(getPackageName())||foregroundPackage.equals("android")
            ||foregroundPackage.equals("com.android.settings")||foregroundPackage.equals("com.android.systemui")
            ||foregroundPackage.contains("permissioncontroller");
    }

    private void tick(long token){
        if(!valid(token)||screenshotPending)return;
        long now=SystemClock.elapsedRealtime();
        if(now<startAt){setStatus("Mở ứng dụng đích · "+((startAt-now+999)/1000)+" giây");later(token,250);return;}
        if(!prepared){setStatus("Đang nạp ảnh mẫu…");later(token,250);return;}
        if(!unlocked()){if(stepDeadline>0)stepDeadline+=1000;setStatus("Tạm chờ · Hãy mở khóa màn hình");later(token,1000);return;}
        if(protectedPackage()){if(stepDeadline>0)stepDeadline+=1000;setStatus("Mở ứng dụng đích để tiếp tục");later(token,1000);return;}
        if(stepDeadline==0)stepDeadline=now+timeoutMs;
        if(now>=stepDeadline){fail(token,"Hết thời gian tìm ảnh ở bước "+(stepIndex+1));return;}
        setStatus("Vòng "+(cycle+1)+"/"+repeats+" · "+steps.get(stepIndex).label);
        final int rotation=windows.getDefaultDisplay().getRotation();
        final String observedPackage=foregroundPackage;
        final long capturedAt=SystemClock.elapsedRealtime();
        screenshotPending=true;
        try{
            takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult screenshot){
                    if(!valid(token)){screenshot.getHardwareBuffer().close();return;}
                    final Bitmap image;
                    try{image=copyScreenshot(screenshot);}catch(Exception e){screenshotPending=false;fail(token,e.getMessage());return;}
                    catch(OutOfMemoryError e){screenshotPending=false;fail(token,"Màn hình quá lớn để xử lý an toàn");return;}
                    final Step step=steps.get(stepIndex);
                    final Rect excluded=controlRect();
                    work=worker.submit(()->{
                        try{
                            int width=image.getWidth(),height=image.getHeight();
                            int[] pixels=new int[width*height];
                            image.getPixels(pixels,0,width,0,0,width,height);image.recycle();
                            // Mask the small floating toolbar so it cannot match its own text or buttons.
                            if(excluded!=null){int x0=Math.max(0,excluded.left),y0=Math.max(0,excluded.top),x1=Math.min(width,excluded.right),y1=Math.min(height,excluded.bottom);
                                for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++)pixels[y*width+x]=Color.MAGENTA;}
                            ImageMatcher.Result match=ImageMatcher.find(pixels,width,height,step.pixels,step.width,step.height,threshold);
                            main.post(()->handleMatch(token,rotation,observedPackage,capturedAt,step,match,excluded));
                        }catch(Exception e){if(!image.isRecycled())image.recycle();main.post(()->{if(valid(token)){screenshotPending=false;fail(token,"Không xử lý được ảnh. Hãy chọn mẫu nhỏ hơn.");}});}
                        catch(OutOfMemoryError e){if(!image.isRecycled())image.recycle();main.post(()->{if(valid(token)){screenshotPending=false;fail(token,"Ảnh quá lớn. Hãy cắt vùng mẫu nhỏ hơn.");}});}
                    });
                }
                @Override public void onFailure(int error){
                    if(!valid(token))return;screenshotPending=false;
                    if(error==ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT){later(token,1000);return;}
                    fail(token,"Không chụp được màn hình ("+error+"). Ứng dụng đích có thể chặn chụp ảnh.");
                }
            });
        }catch(Exception e){screenshotPending=false;fail(token,"Không có quyền chụp màn hình qua Trợ năng");}
    }

    private Bitmap copyScreenshot(ScreenshotResult screenshot){
        HardwareBuffer buffer=screenshot.getHardwareBuffer();Bitmap hardware=null;
        try{
            if((long)buffer.getWidth()*buffer.getHeight()>MAX_SCREEN_PIXELS)throw new IllegalArgumentException("Độ phân giải màn hình quá lớn. Hãy giảm độ phân giải.");
            hardware=Bitmap.wrapHardwareBuffer(buffer,screenshot.getColorSpace());
            if(hardware==null)throw new IllegalStateException("Không đọc được ảnh màn hình");
            Bitmap copy=hardware.copy(Bitmap.Config.ARGB_8888,false);
            if(copy==null)throw new IllegalStateException("Không sao chép được màn hình");
            return copy;
        }finally{if(hardware!=null)hardware.recycle();buffer.close();}
    }

    private void handleMatch(long token,int rotation,String observedPackage,long capturedAt,Step step,ImageMatcher.Result match,Rect excluded){
        if(!valid(token))return;screenshotPending=false;
        if(!unlocked()||protectedPackage()||!observedPackage.equals(foregroundPackage)||rotation!=windows.getDefaultDisplay().getRotation()||SystemClock.elapsedRealtime()-capturedAt>2500){later(token,1000);return;}
        if(match==null||(excluded!=null&&excluded.contains(match.centerX,match.centerY))){later(token,intervalMs);return;}
        if(SystemClock.elapsedRealtime()>=stepDeadline){fail(token,"Hết thời gian tìm ảnh ở bước "+(stepIndex+1));return;}
        if("wait".equals(step.action)){setStatus("Đã thấy ảnh · "+Math.round(match.score*100)+"%");advance(token,step.delay);return;}
        Path path=new Path();path.moveTo(match.centerX,match.centerY);
        GestureDescription gesture=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,70)).build();
        setStatus("Chạm bước "+(stepIndex+1)+" · "+Math.round(match.score*100)+"%");
        boolean dispatched=dispatchGesture(gesture,new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription g){if(valid(token))advance(token,step.delay);}
            @Override public void onCancelled(GestureDescription g){if(valid(token))fail(token,"Thao tác bị hủy. Macro đã dừng.");}
        },main);
        if(!dispatched)fail(token,"Android từ chối thao tác chạm. Hãy kiểm tra quyền Trợ năng.");
    }

    private void advance(long token,int delay){
        if(!valid(token))return;
        stepIndex++;stepDeadline=0;
        if(stepIndex>=steps.size()){
            stepIndex=0;cycle++;
            if(cycle>=repeats){stopInternal("Đã hoàn thành "+repeats+" vòng",true);return;}
            delay=Math.max(delay,intervalMs);
        }
        later(token,Math.max(350,delay));
    }
    private void fail(long token,String reason){if(valid(token))stopInternal(reason==null?"Không chạy được macro":reason,true);}

    public void stopMacro(String reason){
        if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->stopMacro(reason));return;}
        stopInternal(reason==null?"Đã dừng":reason,true);
    }
    private void stopInternal(String reason,boolean showToast){
        ++generation;running=false;captureMode=false;screenshotPending=false;prepared=false;activeMacroId=-1;
        main.removeCallbacksAndMessages(null);
        if(work!=null){work.cancel(true);work=null;}
        steps=null;removeControls();
        if(reason!=null&&!reason.isEmpty()){status=reason;if(showToast)Toast.makeText(this,reason,Toast.LENGTH_LONG).show();}
    }

    public void showCaptureTool(){
        if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->showCaptureTool());return;}
        stopInternal(null,false);captureMode=true;
        status="Mở màn hình mẫu rồi bấm Chụp";
        if(!showControls(true)){captureMode=false;status="Không mở được công cụ chụp";Toast.makeText(this,status,Toast.LENGTH_LONG).show();}
    }

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
                                MacroStore.prefs(AutoService.this).edit().putBoolean("captureReady",true).apply();
                                Intent intent=new Intent().setClassName(getPackageName(),getPackageName()+".MainActivity").putExtra("capture",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
        TextView stop=controlText(capture?"Đóng":"DỪNG",13,Color.WHITE);stop.setGravity(Gravity.CENTER);stop.setPadding(dp(13),dp(10),dp(13),dp(10));stop.setMinHeight(dp(48));stop.setBackground(rounded(capture?0xFF414A69:0xFFC74256,12));stop.setOnClickListener(v->stopMacro(capture?"Đã đóng công cụ chụp":"Đã dừng macro"));LinearLayout.LayoutParams stopLayout=new LinearLayout.LayoutParams(-2,-2);stopLayout.leftMargin=dp(8);controls.addView(stop,stopLayout);
        int width=Math.min(dp(360),getResources().getDisplayMetrics().widthPixels-dp(24));
        controlParams=new WindowManager.LayoutParams(width,-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);controlParams.gravity=Gravity.TOP|Gravity.LEFT;controlParams.x=dp(12);controlParams.y=dp(48);
        statusView.setOnTouchListener(new View.OnTouchListener(){float downX,downY;int startX,startY;public boolean onTouch(View v,MotionEvent event){
            if(controlParams==null||controls==null)return false;
            if(event.getAction()==MotionEvent.ACTION_DOWN){downX=event.getRawX();downY=event.getRawY();startX=controlParams.x;startY=controlParams.y;return true;}
            if(event.getAction()==MotionEvent.ACTION_MOVE){int screenW=getResources().getDisplayMetrics().widthPixels,screenH=getResources().getDisplayMetrics().heightPixels;controlParams.x=Math.max(0,Math.min(screenW-controls.getWidth(),startX+Math.round(event.getRawX()-downX)));controlParams.y=Math.max(0,Math.min(screenH-controls.getHeight(),startY+Math.round(event.getRawY()-downY)));try{windows.updateViewLayout(controls,controlParams);}catch(Exception ignored){}return true;}
            return true;
        }});
        try{windows.addView(controls,controlParams);return true;}catch(Exception e){controls=null;statusView=null;controlParams=null;return false;}
    }
    private Rect controlRect(){if(controls==null)return null;int[] xy=new int[2];controls.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+controls.getWidth(),xy[1]+controls.getHeight());}
    private void removeControls(){if(controls!=null){try{windows.removeView(controls);}catch(Exception ignored){}controls=null;statusView=null;controlParams=null;}}
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
