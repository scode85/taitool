package vn.phonecare;
import android.content.*;
import android.app.*;
import org.json.*;
public final class Data {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("care",0);}
 static JSONArray list(Context c,String k){try{return new JSONArray(prefs(c).getString(k,"[]"));}catch(Exception e){return new JSONArray();}}
 static void save(Context c,String k,JSONArray a){prefs(c).edit().putString(k,a.toString()).apply();}
 static void remove(Context c,String k,int index){JSONArray a=list(c,k);a.remove(index);save(c,k,a);}
 static void channels(Context c){NotificationManager n=c.getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel("monitor","Theo dõi sử dụng",NotificationManager.IMPORTANCE_LOW));n.createNotificationChannel(new NotificationChannel("remind","Nhắc lịch và nghỉ ngơi",NotificationManager.IMPORTANCE_HIGH));}
 static Notification notice(Context c,String channel,String title,String body){return new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_notification).setColor(0xFF176B56).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setAutoCancel(true).build();}
 static void schedule(Context c,JSONObject o){try{long at=o.getLong("at");if(at<=System.currentTimeMillis())return;Intent i=new Intent(c,ReminderReceiver.class).putExtra("title",o.getString("title"));PendingIntent pi=PendingIntent.getBroadcast(c,o.getInt("id"),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);c.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);}catch(Exception e){}}
 static void cancel(Context c,JSONObject o){try{PendingIntent pi=PendingIntent.getBroadcast(c,o.getInt("id"),new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);c.getSystemService(AlarmManager.class).cancel(pi);pi.cancel();}catch(Exception e){}}
}
