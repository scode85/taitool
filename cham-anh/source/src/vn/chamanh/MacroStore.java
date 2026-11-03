package vn.chamanh;
import android.content.*;import org.json.*;import java.io.*;
public final class MacroStore {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("macros",0);}
 static JSONArray all(Context c){try{return new JSONArray(prefs(c).getString("list","[]"));}catch(Exception e){return new JSONArray();}}
 static void save(Context c,JSONArray a){prefs(c).edit().putString("list",a.toString()).apply();}
 static JSONObject find(Context c,long id){JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("id")==id)return o;}return null;}
 static void put(Context c,JSONObject o){JSONArray a=all(c);for(int i=0;i<a.length();i++)if(a.optJSONObject(i)!=null&&a.optJSONObject(i).optLong("id")==o.optLong("id")){try{a.put(i,o);save(c,a);return;}catch(Exception e){throw new IllegalStateException(e);}}a.put(o);save(c,a);}
 static JSONObject create(Context c,String name){try{JSONObject o=new JSONObject().put("id",System.currentTimeMillis()).put("name",name).put("repeat",1).put("interval",1500).put("timeout",15).put("threshold",92).put("steps",new JSONArray());put(c,o);return o;}catch(Exception e){throw new IllegalStateException(e);}}
 static File templates(Context c){File f=new File(c.getFilesDir(),"templates");f.mkdirs();return f;}
 static File image(Context c,String name){if(name==null||!name.matches("[A-Za-z0-9_-]+\\.png"))throw new IllegalArgumentException("Tên ảnh không hợp lệ");return new File(templates(c),name);}
 static void delete(Context c,long id){JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("id")==id){JSONArray steps=o.optJSONArray("steps");if(steps!=null)for(int j=0;j<steps.length();j++)try{image(c,steps.optJSONObject(j).optString("image")).delete();}catch(Exception e){}a.remove(i);break;}}save(c,a);}
}
