package vn.phonecare;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int BG=0xFFF6F7F3, INK=0xFF173F37, MUTED=0xFF64756F,
        TEAL=0xFF176B56, MINT=0xFFE2EFE7, LINE=0xFFE4E9E1, CREAM=0xFFFFF4D8, WHITE=0xFFFFFFFF;
    LinearLayout root,body;
    int page=0;
    EditText limitField,restField;
    Switch strictField;
    String draftLimit,draftRest;
    boolean draftStrict;
    final Handler handler=new Handler(Looper.getMainLooper());
    final Runnable refreshState=()->{if(page==3)render();};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        Data.channels(this);
        draftLimit=String.valueOf(Data.prefs(this).getInt("limit",30));
        draftRest=String.valueOf(Data.prefs(this).getInt("rest",5));
        draftStrict=Data.prefs(this).getBoolean("strict",false);
        if(b!=null){page=b.getInt("page",0);draftLimit=b.getString("draftLimit",draftLimit);draftRest=b.getString("draftRest",draftRest);draftStrict=b.getBoolean("draftStrict",draftStrict);}
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    @Override public void onResume(){super.onResume();render();}
    @Override public void onPause(){captureDrafts();super.onPause();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onSaveInstanceState(Bundle out){captureDrafts();out.putInt("page",page);out.putString("draftLimit",draftLimit);out.putString("draftRest",draftRest);out.putBoolean("draftStrict",draftStrict);super.onSaveInstanceState(out);}
    void captureDrafts(){if(limitField!=null)draftLimit=limitField.getText().toString();if(restField!=null)draftRest=restField.getText().toString();if(strictField!=null)draftStrict=strictField.isChecked();}
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    TextView text(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setLineSpacing(dp(2),1);t.setIncludeFontPadding(false);return t;}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void clickable(View v,int color,int radius,Runnable action){v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18176B56),shape(color,radius),shape(WHITE,radius)));v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->action.run());}
    void gap(LinearLayout l,int h){l.addView(new View(this),lp(1,h));}
    void spacer(LinearLayout l,int w){l.addView(new View(this),lp(w,1));}
    void stretch(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    void section(String title,String meta){LinearLayout r=row();stretch(r,text(title,18,INK,true));if(meta!=null)r.addView(text(meta,12,MUTED,false));LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(24);p.bottomMargin=dp(14);body.addView(r,p);}
    LinearLayout card(int color){LinearLayout c=column();c.setPadding(dp(20),dp(20),dp(20),dp(20));c.setBackground(shape(color,24));LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(12);body.addView(c,p);return c;}
    TextView pill(String s,int color,int bg){TextView t=text(s,11,color,true);t.setPadding(dp(10),dp(6),dp(10),dp(6));t.setBackground(shape(bg,20));return t;}
    View icon(String kind,int size,int color){Glyph v=new Glyph(kind,color);v.setLayoutParams(lp(size,size));v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}
    View iconTile(String kind,int color,int bg){FrameLayout f=new FrameLayout(this);f.setBackground(shape(bg,15));View i=icon(kind,22,color);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER);f.addView(i,p);f.setLayoutParams(lp(46,46));return f;}
    ImageView brand(int size){ImageView v=new ImageView(this);v.setImageResource(R.drawable.ic_brand);v.setContentDescription("Biểu tượng An Tâm");v.setLayoutParams(lp(size,size));return v;}
    TextView action(String title,boolean primary,Runnable run){TextView t=text(title,15,primary?WHITE:TEAL,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(52));t.setPadding(dp(16),dp(14),dp(16),dp(14));clickable(t,primary?TEAL:MINT,16,run);return t;}
    void mainAction(String title,Runnable run){body.addView(action(title,true,run),lp(-1,-2));}
    void go(int next){captureDrafts();page=next;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);render();}

    void render(){
        captureDrafts();limitField=null;restField=null;strictField=null;
        root=column();root.setBackgroundColor(BG);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        root.requestApplyInsets();
        LinearLayout header=row();header.setPadding(dp(22),dp(16),dp(22),dp(14));header.addView(brand(44));spacer(header,12);
        LinearLayout words=column();words.addView(text("An Tâm",22,INK,true));gap(words,4);words.addView(text("Một chút nghỉ, nhiều an tâm.",11,MUTED,false));stretch(header,words);
        TextView info=text("i",17,TEAL,true);info.setGravity(Gravity.CENTER);info.setContentDescription("Thông tin ứng dụng");clickable(info,MINT,24,()->about());header.addView(info,lp(48,48));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        body=column();body.setPadding(dp(22),dp(8),dp(22),dp(28));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(page){case 1:notes();break;case 2:reminders();break;case 3:rest();break;default:home();}
        View line=new View(this);line.setBackgroundColor(LINE);root.addView(line,lp(-1,1));
        LinearLayout nav=row();nav.setPadding(dp(8),dp(8),dp(8),dp(8));nav.setBackgroundColor(WHITE);
        String[] labels={"Tổng quan","Ghi chú","Nhắc lịch","Nghỉ ngơi"},icons={"home","note","calendar","leaf"};
        for(int i=0;i<4;i++){final int index=i;LinearLayout item=column();item.setGravity(Gravity.CENTER);item.setPadding(0,dp(8),0,dp(8));boolean selected=i==page;clickable(item,selected?MINT:WHITE,18,()->go(index));item.setContentDescription(labels[i]);item.setSelected(selected);item.addView(icon(icons[i],22,selected?TEAL:MUTED));gap(item,5);item.addView(text(labels[i],10,selected?TEAL:MUTED,selected));nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));}
        root.addView(nav);
    }
    void heading(String eyebrow,String title,String subtitle){TextView over=text(eyebrow.toUpperCase(new Locale("vi")),10,TEAL,true);over.setLetterSpacing(.13f);body.addView(over);gap(body,8);body.addView(text(title,28,INK,true));if(subtitle!=null){gap(body,8);body.addView(text(subtitle,13,MUTED,false));}gap(body,20);}
    String duration(long ms){long m=ms/60000;return m>=60?(m/60)+" giờ "+(m%60)+" phút":m+" phút";}
    String label(String p){try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(p,0)).toString();}catch(Exception e){String[] bits=p.split("\\.");return bits.length>0?bits[bits.length-1]:p;}}
    Map<String,Long> usage(){try{return Usage.allowed(this)?Usage.today(this):Collections.emptyMap();}catch(Exception e){return Collections.emptyMap();}}
    int futureReminders(){JSONArray a=Data.list(this,"reminders");int n=0;for(int i=0;i<a.length();i++)if(a.optJSONObject(i)!=null&&a.optJSONObject(i).optLong("at")>System.currentTimeMillis())n++;return n;}
    void home(){
        String date=new java.text.SimpleDateFormat("EEEE, dd MMMM",new Locale("vi")).format(new Date());
        heading(date,"Một ngày\ncân bằng hơn.","Chăm chút cho bạn, từ những thói quen nhỏ.");
        boolean allowed=Usage.allowed(this);Map<String,Long> u=usage();long total=0;for(long n:u.values())total+=n;
        LinearLayout hero=card(INK);hero.setPadding(dp(22),dp(22),dp(18),dp(22));
        LinearLayout line=row();stretch(line,pill("THỜI GIAN HÔM NAY",0xFFD5EDDD,0xFF285448));line.addView(icon("sun",20,0xFFDFE8A7));hero.addView(line);gap(hero,22);
        LinearLayout content=row();LinearLayout numbers=column();numbers.addView(text(allowed?duration(total):"Chưa kết nối",allowed?26:24,WHITE,true));gap(numbers,10);numbers.addView(text(allowed?"Mỗi phút nghỉ là một lần\nnạp lại năng lượng.":"Cho phép truy cập để xem\nthời gian sử dụng của bạn.",13,0xFFC0D9CC,false));stretch(content,numbers);content.addView(new BalanceArt(this),lp(78,112));hero.addView(content);gap(hero,20);
        TextView heroAction=text(allowed?"Xem các ứng dụng bên dưới  ↓":"Kết nối dữ liệu sử dụng  →",12,0xFFE5F3C4,true);heroAction.setMinHeight(dp(44));heroAction.setGravity(Gravity.CENTER_VERTICAL);hero.addView(heroAction);if(!allowed)clickable(heroAction,0xFF285448,12,()->openUsage());
        LinearLayout stats=row();smallStat(stats,"note","Ghi chú",String.valueOf(Data.list(this,"notes").length()),CREAM,()->go(1));spacer(stats,12);smallStat(stats,"calendar","Lịch sắp tới",String.valueOf(futureReminders()),MINT,()->go(2));body.addView(stats);
        section("An tâm mỗi ngày",null);
        boolean secure=getSystemService(KeyguardManager.class).isDeviceSecure();
        feature("shield",secure?"Đã có khóa màn hình":"Bảo vệ điện thoại của bạn",secure?"Bạn đã bật khóa bảo mật trên máy.":"Thêm PIN hoặc mật khẩu trong Cài đặt.",secure?MINT:CREAM,()->open(Settings.ACTION_SECURITY_SETTINGS));
        feature("leaf",CareService.running?"Đang giữ nhịp nghỉ ngơi":"Tạo nhịp nghỉ ngơi",CareService.running?"Phiên theo dõi đang hoạt động.":"Chọn ứng dụng và thời gian cần nghỉ.",MINT,()->go(3));
        if(allowed){section("Ứng dụng hôm nay","Ước tính");ArrayList<Map.Entry<String,Long>> entries=new ArrayList<>(u.entrySet());entries.sort((a,b)->Long.compare(b.getValue(),a.getValue()));int count=0;long max=entries.isEmpty()?1:Math.max(1,entries.get(0).getValue());for(Map.Entry<String,Long> e:entries)if(e.getValue()>=60000){usageRow(e.getKey(),e.getValue(),max);count++;}if(count==0)quiet("Chưa có dữ liệu sử dụng hôm nay.","clock");}
        gap(body,16);privacyFoot();
    }
    void smallStat(LinearLayout parent,String glyph,String label,String value,int bg,Runnable run){LinearLayout c=column();c.setPadding(dp(16),dp(17),dp(16),dp(17));clickable(c,bg,22,run);LinearLayout r=row();r.addView(icon(glyph,21,TEAL));stretch(r,new View(this));r.addView(text(value,26,INK,true));c.addView(r);gap(c,10);c.addView(text(label,12,MUTED,false));parent.addView(c,new LinearLayout.LayoutParams(0,-2,1));}
    void feature(String glyph,String title,String detail,int tint,Runnable action){LinearLayout c=card(WHITE);c.setPadding(dp(16),dp(16),dp(12),dp(16));LinearLayout r=row();r.addView(iconTile(glyph,TEAL,tint));spacer(r,12);LinearLayout copy=column();copy.addView(text(title,14,INK,true));gap(copy,5);copy.addView(text(detail,12,MUTED,false));stretch(r,copy);r.addView(icon("chevron",20,MUTED));c.addView(r);clickable(c,WHITE,24,action);}
    void privacyFoot(){LinearLayout r=row();r.setGravity(Gravity.CENTER);r.addView(icon("lock",14,MUTED));spacer(r,6);r.addView(text("Lưu trên máy · Không có quyền Internet",10,MUTED,false));body.addView(r);}
    void usageRow(String p,long ms,long max){LinearLayout c=card(WHITE);c.setPadding(dp(16),dp(14),dp(16),dp(14));LinearLayout r=row();try{ImageView app=new ImageView(this);app.setImageDrawable(getPackageManager().getApplicationIcon(p));r.addView(app,lp(34,34));}catch(Exception e){r.addView(iconTile("phone",TEAL,MINT));}spacer(r,12);stretch(r,text(label(p),13,INK,true));r.addView(text(duration(ms),11,MUTED,false));c.addView(r);gap(c,12);ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress((int)(ms*1000/Math.max(1,max)));bar.setProgressTintList(ColorStateList.valueOf(TEAL));bar.setProgressBackgroundTintList(ColorStateList.valueOf(MINT));c.addView(bar,lp(-1,4));}
    void quiet(String message,String kind){LinearLayout c=card(WHITE);c.setGravity(Gravity.CENTER);gap(c,16);c.addView(iconTile(kind,TEAL,MINT));gap(c,16);TextView t=text(message,14,MUTED,false);t.setGravity(Gravity.CENTER);c.addView(t);gap(c,16);}
    void emptyState(String kind,String title,String detail){LinearLayout c=card(WHITE);c.setGravity(Gravity.CENTER);gap(c,24);c.addView(iconTile(kind,TEAL,MINT));gap(c,20);TextView t=text(title,19,INK,true);t.setGravity(Gravity.CENTER);c.addView(t);gap(c,10);t=text(detail,13,MUTED,false);t.setGravity(Gravity.CENTER);c.addView(t);gap(c,24);}

    void notes(){
        JSONArray a=Data.list(this,"notes");heading("Góc ghi nhớ", "Giữ lại điều\nquan trọng.",a.length()+" ghi chú · Chỉ lưu trên điện thoại của bạn");
        mainAction("＋  Viết ghi chú mới",()->editNote(-1));gap(body,18);
        if(a.length()==0)emptyState("note","Một nơi cho những ý tưởng","Một việc cần làm, một điều đáng nhớ.\nGhi lại để đầu óc được thảnh thơi.");
        for(int j=a.length()-1;j>=0;j--){final int index=j;JSONObject o=a.optJSONObject(j);if(o==null)continue;LinearLayout c=card(j%2==0?WHITE:0xFFFFF5DF);LinearLayout r=row();r.addView(pill("GHI CHÚ",TEAL,j%2==0?MINT:0xFFF3E7C6));stretch(r,new View(this));r.addView(icon("edit",19,TEAL));c.addView(r);gap(c,16);c.addView(text(o.optString("title"),20,INK,true));String note=o.optString("body");if(!note.isEmpty()){gap(c,10);TextView preview=text(note,14,MUTED,false);preview.setMaxLines(5);preview.setEllipsize(android.text.TextUtils.TruncateAt.END);c.addView(preview);}gap(c,16);c.addView(text("Chạm để chỉnh sửa",10,MUTED,false));clickable(c,j%2==0?WHITE:0xFFFFF5DF,24,()->editNote(index));}
        gap(body,12);privacyFoot();
    }
    EditText input(String hint,String value,boolean multiline){EditText e=new EditText(this);e.setText(value);e.setHint(hint);e.setTextSize(15);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setPadding(dp(15),dp(14),dp(15),dp(14));e.setBackground(shape(BG,14));if(multiline){e.setGravity(Gravity.TOP);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setMinLines(5);}else{e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}return e;}
    LinearLayout dialogBox(){LinearLayout box=column();box.setPadding(dp(22),dp(10),dp(22),dp(10));return box;}
    void styleDialog(AlertDialog d){if(d.getWindow()!=null)d.getWindow().setBackgroundDrawable(shape(WHITE,24));d.getButton(-1).setTextColor(TEAL);d.getButton(-2).setTextColor(MUTED);d.getButton(-3).setTextColor(0xFFAA4B43);}
    void editNote(int idx){
        JSONObject old=Data.list(this,"notes").optJSONObject(idx);LinearLayout box=dialogBox();
        EditText title=input("Tiêu đề ghi chú",old==null?"":old.optString("title"),false),content=input("Viết điều bạn muốn ghi nhớ…",old==null?"":old.optString("body"),true);
        box.addView(title,lp(-1,-2));gap(box,12);box.addView(content,lp(-1,-2));ScrollView scroll=new ScrollView(this);scroll.addView(box);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(idx<0?"Ghi chú mới":"Ghi chú của bạn").setView(scroll).setPositiveButton("Lưu ghi chú",null).setNegativeButton("Hủy",null);
        if(idx>=0)builder.setNeutralButton("Xóa",null);
        AlertDialog d=builder.create();d.setOnShowListener(v->{styleDialog(d);if(idx>=0)d.getButton(-3).setOnClickListener(w->confirm("Xóa ghi chú?","Ghi chú này sẽ bị xóa khỏi điện thoại.",()->{Data.remove(this,"notes",idx);d.dismiss();render();}));d.getButton(-1).setOnClickListener(w->{if(title.getText().toString().trim().isEmpty()){title.setError("Hãy nhập tiêu đề");return;}try{JSONArray a=Data.list(this,"notes");JSONObject o=new JSONObject().put("title",title.getText().toString().trim()).put("body",content.getText().toString());if(idx<0)a.put(o);else a.put(idx,o);Data.save(this,"notes",a);d.dismiss();render();}catch(Exception e){toast("Không lưu được ghi chú");}});});d.show();
    }
    void confirm(String title,String message,Runnable yes){AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Xóa",(v,w)->yes.run()).setNegativeButton("Giữ lại",null).create();d.setOnShowListener(v->styleDialog(d));d.show();}

    void reminders(){
        heading("Lịch của bạn","Đúng việc.\nNhẹ đầu hơn.",futureReminders()+" lịch nhắc sắp tới");
        mainAction("＋  Thêm lịch nhắc",()->addReminder());gap(body,18);
        if(!notificationsAllowed())permissionRow(body,"bell","Cho phép thông báo","Để nhận nhắc lịch khi đến giờ.",false,()->notificationPermission());
        JSONArray a=Data.list(this,"reminders");ArrayList<Integer> order=new ArrayList<>();for(int j=0;j<a.length();j++)if(a.optJSONObject(j)!=null)order.add(j);
        order.sort((i,j)->{long x=a.optJSONObject(i).optLong("at"),y=a.optJSONObject(j).optLong("at"),now=System.currentTimeMillis();boolean xp=x<=now,yp=y<=now;return xp!=yp?(xp?1:-1):(xp?Long.compare(y,x):Long.compare(x,y));});
        if(order.isEmpty())emptyState("calendar","Dành chỗ cho kế hoạch mới","Lịch hẹn, công việc hay một khoảng nghỉ.\nAn Tâm sẽ nhắc bạn khi đến giờ.");
        for(int index:order){JSONObject o=a.optJSONObject(index);long at=o.optLong("at");boolean past=at<=System.currentTimeMillis();LinearLayout c=card(WHITE);LinearLayout r=row();LinearLayout date=column();date.setGravity(Gravity.CENTER);date.setPadding(dp(9),dp(10),dp(9),dp(10));date.setBackground(shape(past?BG:MINT,14));date.addView(text(android.text.format.DateFormat.format("dd",at).toString(),24,TEAL,true));gap(date,3);date.addView(text(android.text.format.DateFormat.format("MM/yyyy",at).toString(),9,MUTED,false));r.addView(date,lp(64,-2));spacer(r,14);LinearLayout copy=column();copy.addView(text(o.optString("title"),16,INK,true));gap(copy,8);copy.addView(text(android.text.format.DateFormat.format("HH:mm",at)+"  ·  "+(past?"Đã qua":"Sắp tới"),12,past?MUTED:TEAL,false));stretch(r,copy);FrameLayout delete=new FrameLayout(this);View glyph=icon("trash",18,MUTED);delete.addView(glyph,new FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER));delete.setContentDescription("Xóa lịch "+o.optString("title"));clickable(delete,WHITE,22,()->confirm("Xóa lịch nhắc?","Thông báo cho lịch này sẽ được hủy.",()->{Data.cancel(this,o);Data.remove(this,"reminders",index);render();}));r.addView(delete,lp(48,48));c.addView(r);}
        gap(body,10);body.addView(text("Thông báo có thể đến trễ khi Android tiết kiệm pin. Lịch tương lai sẽ được đặt lại sau khi khởi động máy.",11,MUTED,false));
    }
    void addReminder(){
        LinearLayout box=dialogBox();EditText title=input("Bạn muốn được nhắc điều gì?","",false);box.addView(title,lp(-1,-2));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Lịch nhắc mới").setView(box).setPositiveButton("Chọn ngày",null).setNegativeButton("Hủy",null).create();
        d.setOnShowListener(v->{styleDialog(d);d.getButton(-1).setOnClickListener(w->{String name=title.getText().toString().trim();if(name.isEmpty()){title.setError("Hãy nhập nội dung cần nhắc");return;}d.dismiss();Calendar at=Calendar.getInstance();DatePickerDialog dates=new DatePickerDialog(this,(dp,y,m,day)->{at.set(y,m,day);new TimePickerDialog(this,(tp,h,min)->{at.set(Calendar.HOUR_OF_DAY,h);at.set(Calendar.MINUTE,min);at.set(Calendar.SECOND,0);at.set(Calendar.MILLISECOND,0);if(at.getTimeInMillis()<=System.currentTimeMillis()){toast("Hãy chọn thời gian trong tương lai");return;}try{JSONArray a=Data.list(this,"reminders");int id=Data.prefs(this).getInt("nextId",100);Data.prefs(this).edit().putInt("nextId",id+1).apply();JSONObject o=new JSONObject().put("id",id).put("title",name).put("at",at.getTimeInMillis());a.put(o);Data.save(this,"reminders",a);Data.schedule(this,o);render();toast("Đã thêm lịch nhắc");}catch(Exception ex){toast("Không tạo được lịch");}},at.get(Calendar.HOUR_OF_DAY),at.get(Calendar.MINUTE),true).show();},at.get(Calendar.YEAR),at.get(Calendar.MONTH),at.get(Calendar.DAY_OF_MONTH));dates.getDatePicker().setMinDate(System.currentTimeMillis()-1000);dates.show();});});d.show();
    }
    boolean notificationsAllowed(){return getSystemService(NotificationManager.class).areNotificationsEnabled()&&(Build.VERSION.SDK_INT<33||checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED);}
    void notificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},3);else openNotifications();}
    void openNotifications(){try{startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}catch(Exception e){toast("Hãy mở cài đặt thông báo của An Tâm");}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);render();if(r==3&&g.length>0&&g[0]!=PackageManager.PERMISSION_GRANTED)toast("Có thể bật thông báo trong Cài đặt Android");}
    void openUsage(){open(Settings.ACTION_USAGE_ACCESS_SETTINGS);}
    void open(String action){try{startActivity(new Intent(action));}catch(Exception e){toast("Không mở được cài đặt trên thiết bị này");}}
    void overlayPermission(){try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){toast("Hãy mở quyền hiển thị trên ứng dụng khác trong Cài đặt");}}

    void rest(){
        heading("Thói quen lành mạnh","Nghỉ một chút,\nkhỏe hơn mỗi ngày.","Tạo khoảng nghỉ sau mỗi phiên dùng ứng dụng.");
        LinearLayout status=card(INK);LinearLayout sr=row();stretch(sr,text("NHỊP NGHỈ CỦA BẠN",10,0xFFD0E7D6,true));sr.addView(pill(CareService.running?"ĐANG THEO DÕI":"CHƯA BẬT",0xFFE1F1D1,0xFF2A594B));status.addView(sr);gap(status,18);
        status.addView(text(Data.prefs(this).getInt("limit",30)+" phút dùng  /  "+Data.prefs(this).getInt("rest",5)+" phút nghỉ",20,WHITE,true));gap(status,8);status.addView(text(CareService.running?"Thay đổi cài đặt và bắt đầu lại sẽ tạo phiên mới.":"Bật theo dõi khi bạn sẵn sàng bắt đầu.",12,0xFFC0D9CC,false));
        section("Thiết lập quyền",null);
        LinearLayout permissions=card(WHITE);permissions.setPadding(dp(16),dp(4),dp(12),dp(4));
        permissionRow(permissions,"clock","Dữ liệu sử dụng","Đọc thời gian dùng ứng dụng",Usage.allowed(this),()->openUsage());
        permissionRow(permissions,"bell","Thông báo","Nhận nhắc lịch và cảnh báo nghỉ",notificationsAllowed(),()->notificationPermission());
        permissionRow(permissions,"shield","Hiển thị trên ứng dụng khác","Cần cho chế độ che ứng dụng",Settings.canDrawOverlays(this),()->overlayPermission());
        section("Ứng dụng giới hạn",null);Set<String> selected=Data.prefs(this).getStringSet("apps",Collections.emptySet());
        LinearLayout apps=card(WHITE);LinearLayout ar=row();ar.addView(iconTile("phone",TEAL,MINT));spacer(ar,12);LinearLayout copy=column();copy.addView(text(selected.isEmpty()?"Chưa chọn ứng dụng":selected.size()+" ứng dụng đã chọn",15,INK,true));gap(copy,5);ArrayList<String> names=new ArrayList<>();for(String p:selected)names.add(label(p));Collections.sort(names);copy.addView(text(selected.isEmpty()?"Chạm để chọn từ ứng dụng đã dùng":android.text.TextUtils.join(", ",names),12,MUTED,false));stretch(ar,copy);ar.addView(icon("chevron",20,MUTED));apps.addView(ar);clickable(apps,WHITE,24,()->chooseApps());
        section("Thời gian của bạn",null);LinearLayout timer=card(WHITE);LinearLayout inputs=row();
        LinearLayout left=column(),right=column();left.addView(text("Dùng ứng dụng",12,MUTED,false));gap(left,9);limitField=numberInput(draftLimit,"Phút sử dụng");left.addView(limitField,lp(-1,-2));gap(left,7);left.addView(text("1–240 phút",10,MUTED,false));
        right.addView(text("Nghỉ ngơi",12,MUTED,false));gap(right,9);restField=numberInput(draftRest,"Phút nghỉ");right.addView(restField,lp(-1,-2));gap(right,7);right.addView(text("1–60 phút",10,MUTED,false));inputs.addView(left,new LinearLayout.LayoutParams(0,-2,1));spacer(inputs,16);inputs.addView(right,new LinearLayout.LayoutParams(0,-2,1));timer.addView(inputs);
        gap(timer,20);View line=new View(this);line.setBackgroundColor(LINE);timer.addView(line,lp(-1,1));gap(timer,18);
        strictField=new Switch(this);strictField.setText("Che ứng dụng khi đến giờ nghỉ");strictField.setTextSize(13);strictField.setTextColor(INK);strictField.setMinHeight(dp(48));strictField.setSwitchPadding(dp(10));strictField.setButtonTintList(ColorStateList.valueOf(TEAL));strictField.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{TEAL,MUTED}));strictField.setTrackTintList(ColorStateList.valueOf(MINT));strictField.setChecked(draftStrict);timer.addView(strictField,lp(-1,-2));gap(timer,5);timer.addView(text("Tắt chế độ này để chỉ nhận cảnh báo.",11,MUTED,false));
        gap(body,8);mainAction(CareService.running?"Lưu và bắt đầu phiên mới":"Bắt đầu theo dõi",()->startMonitoring());
        if(CareService.running){gap(body,10);body.addView(action("Dừng theo dõi",false,()->{stopService(new Intent(this,CareService.class));toast("Đã dừng theo dõi");handler.postDelayed(refreshState,150);}),lp(-1,-2));}
        gap(body,16);body.addView(text("Cộng dồn thời gian giữa các ứng dụng đã chọn. Cảnh báo trước 1 phút. Tắt màn hình đủ thời gian nghỉ sẽ đặt lại phiên.",11,MUTED,false));gap(body,12);body.addView(text("Bạn vẫn có thể dùng nút Home, gọi điện và thu hồi quyền. Sau khi khởi động máy, hãy mở An Tâm để bật lại theo dõi.",11,MUTED,false));
    }
    EditText numberInput(String value,String description){EditText e=input("",value,false);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);e.setTextSize(25);e.setMinHeight(dp(60));e.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));e.setContentDescription(description);e.setSelectAllOnFocus(true);return e;}
    void permissionRow(LinearLayout parent,String kind,String title,String detail,boolean granted,Runnable action){LinearLayout r=row();r.setPadding(0,dp(14),0,dp(14));r.addView(icon(kind,21,TEAL));spacer(r,12);LinearLayout copy=column();copy.addView(text(title,13,INK,true));gap(copy,5);copy.addView(text(detail,11,MUTED,false));stretch(r,copy);spacer(r,8);r.addView(pill(granted?"Đã cấp":"Cấp quyền",granted?TEAL:0xFF805F26,granted?MINT:CREAM));clickable(r,WHITE,16,action);LinearLayout.LayoutParams p=lp(-1,-2);if(parent==body)p.bottomMargin=dp(14);parent.addView(r,p);}
    void startMonitoring(){captureDrafts();try{int limit=Integer.parseInt(draftLimit),rest=Integer.parseInt(draftRest);if(limit<1||limit>240||rest<1||rest>60){toast("Sử dụng 1–240 phút, nghỉ 1–60 phút");return;}if(!Usage.allowed(this)){toast("Hãy cấp quyền dữ liệu sử dụng");return;}if(Data.prefs(this).getStringSet("apps",Collections.emptySet()).isEmpty()){toast("Hãy chọn ít nhất một ứng dụng");return;}if(draftStrict&&!Settings.canDrawOverlays(this)){toast("Chế độ che cần quyền hiển thị trên ứng dụng khác");return;}if(!notificationsAllowed()){notificationPermission();return;}Data.prefs(this).edit().putInt("limit",limit).putInt("rest",rest).putBoolean("strict",draftStrict).remove("off").apply();stopService(new Intent(this,CareService.class));startForegroundService(new Intent(this,CareService.class));toast("Đã bắt đầu phiên theo dõi mới");handler.postDelayed(refreshState,300);}catch(NumberFormatException e){toast("Nhập số phút hợp lệ");}catch(Exception e){toast("Không bật được theo dõi. Hãy mở lại ứng dụng và thử lại.");}}
    void chooseApps(){
        if(!Usage.allowed(this)){toast("Hãy cấp quyền dữ liệu sử dụng trước");return;}Set<String> packages=new HashSet<>(usage().keySet());Set<String> selected=new HashSet<>(Data.prefs(this).getStringSet("apps",Collections.emptySet()));packages.addAll(selected);packages.remove(getPackageName());ArrayList<String> ps=new ArrayList<>(packages);ps.sort((a,b)->label(a).compareToIgnoreCase(label(b)));if(ps.isEmpty()){toast("Hãy dùng ứng dụng cần giới hạn rồi quay lại chọn");return;}
        String[] names=new String[ps.size()];boolean[] checked=new boolean[ps.size()];for(int i=0;i<ps.size();i++){names[i]=label(ps.get(i))+"\n"+ps.get(i);checked[i]=selected.contains(ps.get(i));}
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Chọn ứng dụng đã dùng").setMultiChoiceItems(names,checked,(v,i,c)->{if(c)selected.add(ps.get(i));else selected.remove(ps.get(i));}).setPositiveButton("Lưu lựa chọn",(v,w)->{Data.prefs(this).edit().putStringSet("apps",selected).apply();render();}).setNegativeButton("Hủy",null).create();d.setOnShowListener(v->styleDialog(d));d.show();
    }
    void about(){AlertDialog d=new AlertDialog.Builder(this).setTitle("An Tâm · 0.2.0").setMessage("Một chút nghỉ, nhiều an tâm.\n\nGhi chú, lịch nhắc và thời gian sử dụng được xử lý trên máy. Ứng dụng không có quyền Internet; gỡ cài đặt sẽ xóa ghi chú và lịch.\n\nBảo mật cơ bản: kiểm tra khóa màn hình, không quét virus. Ghi chú chưa có khóa sinh trắc học riêng.\n\nThời gian sử dụng là ước tính; Android có thể trì hoãn nhắc lịch hoặc dừng theo dõi nền.\n\nBản thử nghiệm 0.2.0.").setPositiveButton("Đã hiểu",null).create();d.setOnShowListener(v->styleDialog(d));d.show();}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    final class Glyph extends View {
        final String kind;final int color;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Glyph(String kind,int color){super(MainActivity.this);this.kind=kind;this.color=color;}
        void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,p);}
        void path(Canvas c,float... a){Path q=new Path();q.moveTo(a[0],a[1]);for(int i=2;i<a.length;i+=2)q.lineTo(a[i],a[i+1]);c.drawPath(q,p);}
        @Override protected void onDraw(Canvas original){super.onDraw(original);int save=original.save();original.scale(getWidth()/24f,getHeight()/24f);Canvas c=original;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            switch(kind){
                case "home":path(c,3,10,12,3,21,10);path(c,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
                case "note":c.drawRoundRect(5,3,19,21,3,3,p);line(c,9,8,15,8);line(c,9,12,15,12);line(c,9,16,13,16);break;
                case "calendar":c.drawRoundRect(3,5,21,21,3,3,p);line(c,3,10,21,10);line(c,8,3,8,7);line(c,16,3,16,7);line(c,8,15,10,15);line(c,14,15,16,15);break;
                case "leaf":Path leaf=new Path();leaf.moveTo(4,19);leaf.cubicTo(1,7,12,3,21,3);leaf.cubicTo(22,15,15,22,7,19);c.drawPath(leaf,p);path(c,3,22,10,15,16,9);break;
                case "shield":path(c,12,2,21,6,20,14,17,19,12,22,7,19,4,14,3,6,12,2);path(c,8,12,11,15,16,9);break;
                case "clock":c.drawCircle(12,12,9,p);path(c,12,6,12,12,16,14);break;
                case "sun":c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(c,12+7*(float)Math.cos(a),12+7*(float)Math.sin(a),12+9*(float)Math.cos(a),12+9*(float)Math.sin(a));}break;
                case "phone":c.drawRoundRect(6,2,18,22,3,3,p);line(c,10,18,14,18);line(c,10,5,14,5);break;
                case "lock":c.drawRoundRect(5,10,19,21,2,2,p);c.drawArc(8,3,16,17,180,180,false,p);line(c,12,14,12,17);break;
                case "chevron":path(c,9,5,16,12,9,19);break;
                case "edit":path(c,4,16,16,4,20,8,8,20,4,20,4,16);line(c,13,7,17,11);break;
                case "trash":path(c,5,7,6,21,18,21,19,7);line(c,3,7,21,7);path(c,8,7,8,3,16,3,16,7);line(c,10,11,10,17);line(c,14,11,14,17);break;
                case "bell":Path b=new Path();b.moveTo(4,18);b.lineTo(6,15);b.lineTo(6,9);b.cubicTo(6,1,18,1,18,9);b.lineTo(18,15);b.lineTo(20,18);b.close();c.drawPath(b,p);c.drawArc(9,18,15,23,0,180,false,p);break;
                default:c.drawCircle(12,12,8,p);
            }original.restoreToCount(save);
        }
    }
    final class BalanceArt extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        BalanceArt(Context c){super(c);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/90f,getHeight()/120f);p.setColor(0xFF315B49);c.drawCircle(47,60,40,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xFF91BA93);c.drawRoundRect(26,15,67,101,9,9,p);p.setStyle(Paint.Style.FILL);p.setColor(0xFFDFE8A7);c.drawRoundRect(32,22,61,86,4,4,p);p.setColor(0xFF173F37);c.drawRoundRect(42,92,51,95,2,2,p);Path leaf=new Path();leaf.moveTo(44,68);leaf.cubicTo(30,46,52,39,69,39);leaf.cubicTo(69,58,58,70,44,68);p.setColor(0xFF458364);c.drawPath(leaf,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xFFCCE2B6);c.drawLine(40,74,59,48,p);p.setStyle(Paint.Style.FILL);p.setColor(0xFFE7ECCA);c.drawCircle(73,20,5,p);p.setColor(0xFF7FA17C);c.drawCircle(15,91,3,p);c.restore();}
    }
}
