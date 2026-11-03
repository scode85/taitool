package vn.phonecare;
import android.content.*;import android.app.*;import org.json.*;
public class ReminderReceiver extends BroadcastReceiver{
 public void onReceive(Context c,Intent i){Data.channels(c);if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){JSONArray a=Data.list(c,"reminders");for(int j=0;j<a.length();j++)Data.schedule(c,a.optJSONObject(j));return;}try{c.getSystemService(NotificationManager.class).notify((int)System.currentTimeMillis(),Data.notice(c,"remind","Đến giờ rồi",i.getStringExtra("title")));}catch(SecurityException e){}}
}
