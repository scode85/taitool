package vn.phonecare;
import android.app.*;import android.app.usage.*;import android.content.*;import java.util.*;
public final class Usage{
 static boolean allowed(Context c){return c.getSystemService(AppOpsManager.class).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
 static Map<String,Long> today(Context c){Calendar start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);long now=System.currentTimeMillis();Map<String,Long> result=new HashMap<>();UsageEvents events=c.getSystemService(UsageStatsManager.class).queryEvents(start.getTimeInMillis(),now);UsageEvents.Event e=new UsageEvents.Event();String active=null;long since=0;while(events.hasNextEvent()){events.getNextEvent(e);int t=e.getEventType();if(t==1){if(active!=null)add(result,active,e.getTimeStamp()-since);active=e.getPackageName();since=e.getTimeStamp();}else if((t==2&&e.getPackageName().equals(active))||t==15){if(active!=null)add(result,active,e.getTimeStamp()-since);active=null;}}if(active!=null)add(result,active,now-since);return result;}
 static void add(Map<String,Long> m,String p,long n){m.put(p,m.getOrDefault(p,0L)+Math.max(0,n));}
}
