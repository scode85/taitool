package vn.phonecare;
import android.app.*;import android.app.usage.*;import android.content.*;import android.os.*;import android.provider.Settings;import android.view.*;import java.util.*;
/** A user-started monitoring session; rest enforcement is restricted to selected apps. */
public class CareService extends Service {
 static volatile boolean running;
 static volatile String statusText="Chưa bật theo dõi",currentPackage="",lastError="";
 static volatile long usedMs,restRemainingMs;
 private static CareService service;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final ForegroundState foreground=new ForegroundState();
 private SessionTimer timer;private RestOverlay ordinaryOverlay;
 private long queryFrom,lastSave,lastHomeAttempt,lastDiagnostic;
 private String dialer="",launcher="";
 public static void poke(){CareService s=service;if(s!=null)s.handler.post(s::safeTick);}
 @Override public void onCreate(){super.onCreate();Data.channels(this);startForeground(1,Data.notice(this,"monitor","An Tâm đang theo dõi","Chạm để xem bộ đếm, quyền và chế độ nghỉ."));service=this;running=true;ordinaryOverlay=new RestOverlay(this,getSystemService(WindowManager.class),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);queryFrom=System.currentTimeMillis()-86400000L;resolveExemptions();restore();handler.post(loop);}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&intent.getBooleanExtra("newSession",false)){Data.prefs(this).edit().putBoolean("monitorRequested",true).apply();timer=new SessionTimer(limit(),rest());lastError="";lastHomeAttempt=0;hide();save();}
  if(!Data.prefs(this).getBoolean("monitorRequested",false)){stopSelf();return START_NOT_STICKY;}
  return START_STICKY;
 }
 private long limit(){return Math.max(1,Data.prefs(this).getInt("limit",30))*60000L;}
 private long rest(){return Math.max(1,Data.prefs(this).getInt("rest",5))*60000L;}
 private int bootCount(){try{return Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1);}catch(RuntimeException e){return -1;}}
 private void restore(){long now=SystemClock.elapsedRealtime(),end=Data.prefs(this).getLong("sessionRestEnd",0),saved=Data.prefs(this).getLong("sessionSavedElapsed",0);long used=Data.prefs(this).getLong("sessionUsed",0),remaining;
  int boot=bootCount();boolean sameBoot=boot>=0&&boot==Data.prefs(this).getInt("sessionBoot",-2);if(sameBoot&&saved<=now)remaining=Math.max(0,end-now);else remaining=Math.max(0,Data.prefs(this).getLong("sessionRestWall",0)-System.currentTimeMillis());
  if(end>0&&remaining==0)used=0;timer=new SessionTimer(limit(),rest(),used,Math.min(rest(),remaining),now);
 }
 private final Runnable loop=new Runnable(){public void run(){safeTick();if(service==CareService.this)handler.postDelayed(this,1000);}};
 private void safeTick(){if(service!=this)return;try{tick();}catch(RuntimeException e){lastError="Lỗi theo dõi: "+e.getClass().getSimpleName()+". Mở An Tâm để kiểm tra quyền.";statusText=lastError;diagnostic(lastError);}}
 private void updateForeground(long wall){
  if(!Usage.allowed(this))return;
  try{UsageEvents events=getSystemService(UsageStatsManager.class).queryEvents(queryFrom,wall);UsageEvents.Event event=new UsageEvents.Event();while(events.hasNextEvent()){events.getNextEvent(event);int type=event.getEventType();long at=event.getTimeStamp();int instance=0;if(type==1)foreground.resumed(event.getPackageName(),event.getClassName(),instance,at);else if(type==2||type==23)foreground.paused(event.getPackageName(),event.getClassName(),instance,at);else if(type==15||type==17||type==26)foreground.clear(at);}queryFrom=wall-10000;foreground.pruneBefore(wall-120000);if(lastError.startsWith("Không đọc được dữ liệu sử dụng"))lastError="";}catch(RuntimeException e){lastError="Không đọc được dữ liệu sử dụng; kiểm tra lại quyền trong Cài đặt.";}
 }
 private boolean exempt(String pkg){return pkg==null||pkg.isEmpty()||pkg.equals(getPackageName())||pkg.equals(dialer)||pkg.equals(launcher)||pkg.equals("com.android.systemui")||pkg.equals("android")||pkg.equals("com.android.settings")||pkg.contains("permissioncontroller");}
 private void tick(){long now=SystemClock.elapsedRealtime(),wall=System.currentTimeMillis();RestAccessService access=RestAccessService.instance;
  if(!Usage.allowed(this)&&access==null){lastError="Thiếu quyền dữ liệu sử dụng và Trợ năng. Theo dõi đã dừng.";statusText=lastError;diagnostic(lastError);stopSelf();return;}
  updateForeground(wall);String pkg=ForegroundChoice.choose(access!=null?RestAccessService.foregroundPackage:"",access!=null?RestAccessService.foregroundAt:0,foreground.current(),foreground.latestEventTime(),now,wall);currentPackage=pkg;
  boolean screen=getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();boolean selected=screen&&!exempt(pkg)&&Data.prefs(this).getStringSet("apps",Collections.emptySet()).contains(pkg);
  if(timer==null)restore();boolean wasRest=timer.resting(now);timer.tick(now,screen,selected);usedMs=timer.usedMs();restRemainingMs=timer.restRemainingMs(now);
  boolean started=timer.consumeRestStarted();if(timer.consumeWarning())notice("Sắp đến giờ nghỉ","Phiên sử dụng sắp hết. Hãy chuẩn bị tạm dừng ứng dụng.");if(started)notice("Đã đến giờ nghỉ","Đã hết thời gian sử dụng. Bắt đầu nghỉ "+Data.prefs(this).getInt("rest",5)+" phút.");
  if(restRemainingMs>0)enforce(now,selected,access);else{hide();statusText=selected?"Đang tính thời gian ứng dụng đã chọn":"Đang chờ ứng dụng đã chọn";if(!screen)statusText="Màn hình đã khóa/tắt · Tạm dừng tính giờ";}
  if(started||wasRest!=timer.resting(now)||now-lastSave>=5000)save();
 }
 private void enforce(long now,boolean selected,RestAccessService access){
  if(!selected){hide();statusText="Đang nghỉ · Có thể dùng ứng dụng khác";return;}
  boolean strict=Data.prefs(this).getBoolean("strict",true),home=Data.prefs(this).getBoolean("goHome",true);boolean shown=false;
  if(strict){if(access!=null){shown=access.showRest(now+restRemainingMs);if(shown)ordinaryOverlay.hide();}if(!shown&&Settings.canDrawOverlays(this))shown=ordinaryOverlay.show(now+restRemainingMs);}
  if(home&&access!=null&&now-lastHomeAttempt>=1500){lastHomeAttempt=now;try{if(access.goHome()){hide();lastError="";statusText="Đã đưa về màn hình chính · Đang nghỉ";return;}}catch(RuntimeException e){lastError="Android từ chối về màn hình chính. Kiểm tra quyền Trợ năng.";}}
  if(shown){lastError="";statusText="Đang che ứng dụng · Đếm ngược nghỉ";}
  else if(!strict&&!home)statusText="Đang nghỉ · Chế độ chỉ cảnh báo";
  else{lastError=home&&access==null?"Chưa bật Trợ năng: không thể tự về màn hình chính. Bật trong An Tâm → Nghỉ ngơi.":"Chưa chặn được ứng dụng. Cấp quyền hiển thị hoặc bật Trợ năng; kiểm tra chế độ trò chơi của máy.";statusText=lastError;diagnostic(lastError);}
 }
 private void hide(){if(ordinaryOverlay!=null)ordinaryOverlay.hide();RestAccessService access=RestAccessService.instance;if(access!=null)access.hideRest();}
 private void save(){if(timer==null)return;long now=SystemClock.elapsedRealtime(),remaining=timer.restRemainingMs(now);Data.prefs(this).edit().putLong("sessionUsed",timer.usedMs()).putLong("sessionRestEnd",remaining>0?now+remaining:0).putLong("sessionRestWall",remaining>0?System.currentTimeMillis()+remaining:0).putLong("sessionSavedElapsed",now).putInt("sessionBoot",bootCount()).apply();lastSave=now;}
 private void notice(String title,String body){try{getSystemService(NotificationManager.class).notify(2,Data.notice(this,"remind",title,body));}catch(SecurityException ignored){}}
 private void diagnostic(String reason){long now=SystemClock.elapsedRealtime();if(now-lastDiagnostic<60000)return;lastDiagnostic=now;try{getSystemService(NotificationManager.class).notify(3,Data.notice(this,"remind","An Tâm cần kiểm tra quyền",reason));}catch(SecurityException ignored){}}
 private void resolveExemptions(){try{String p=getSystemService(android.telecom.TelecomManager.class).getDefaultDialerPackage();dialer=p==null?"":p;}catch(RuntimeException ignored){}try{android.content.pm.ResolveInfo info=getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);if(info!=null&&info.activityInfo!=null)launcher=info.activityInfo.packageName;}catch(RuntimeException ignored){}}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){save();handler.removeCallbacksAndMessages(null);hide();running=false;if(service==this)service=null;super.onDestroy();}
}
