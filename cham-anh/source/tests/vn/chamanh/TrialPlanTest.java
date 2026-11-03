package vn.chamanh;
import org.json.*;
public final class TrialPlanTest {
 static int checks;
 static void ok(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static JSONObject step(String a)throws Exception{return new JSONObject().put("action",a).put("label",a).put("delay",900);}
 static void bad(JSONObject macro,int i,String label){boolean thrown=false;try{TrialPlan.singleStep(macro,i);}catch(IllegalArgumentException e){thrown=true;}ok(thrown,label);}
 public static void main(String[] args)throws Exception{
  JSONObject selected=step("tap").put("image","sample_123.png");
  JSONObject macro=new JSONObject().put("id",456).put("name","Kịch bản gốc").put("repeat",1000).put("infinite",true).put("threshold",94).put("interval",2500).put("timeout",30).put("steps",new JSONArray().put(step("home")).put(selected).put(step("back")));
  String before=macro.toString();JSONObject trial=TrialPlan.singleStep(macro,1);
  ok(trial.getInt("repeat")==1,"Trial never inherits the saved repeat count");
  ok(!trial.getBoolean("infinite")&&macro.getBoolean("infinite"),"Trial clears infinite mode without changing saved choice");
  ok(trial.getJSONArray("steps").length()==1,"Only selected step runs");
  ok(trial.getJSONArray("steps").getJSONObject(0).getString("action").equals("tap"),"Correct selected action");
  ok(trial.getInt("trialStep")==2,"Original step number is retained");
  ok(trial.getLong("id")==456,"Owner id retained for deletion guard");
  ok(trial.getInt("threshold")==94&&trial.getInt("interval")==2500&&trial.getInt("timeout")==30,"Image settings retained");
  ok(before.equals(macro.toString()),"Building trial does not change stored macro");
  trial.getJSONArray("steps").getJSONObject(0).put("label","changed");ok(selected.getString("label").equals("tap"),"Trial step deep copy is isolated");
  selected.put("delay",2000);ok(trial.getJSONArray("steps").getJSONObject(0).getInt("delay")==900,"Source edits cannot mutate trial snapshot");
  bad(macro,-1,"Negative index rejected");bad(macro,3,"Index past end rejected");bad(null,0,"Null macro rejected");bad(new JSONObject(),0,"Missing steps rejected");
  bad(new JSONObject().put("steps",new JSONArray().put(JSONObject.NULL)),0,"Empty selected step rejected");
  bad(new JSONObject().put("steps",new JSONArray().put(step("unknown"))),0,"Invalid selected action rejected");
  JSONObject mixed=new JSONObject().put("steps",new JSONArray().put(step("unknown")).put(step("home")));
  ok(TrialPlan.singleStep(mixed,1).getJSONArray("steps").getJSONObject(0).getString("action").equals("home"),"Unselected invalid steps are not loaded");
  JSONObject legacy=new JSONObject().put("steps",new JSONArray().put(new JSONObject().put("image","old.png")));
  ok(TrialPlan.singleStep(legacy,0).getJSONArray("steps").getJSONObject(0).getString("image").equals("old.png"),"Legacy image step supported");
  for(String a:new String[]{"home","back","notifications","quick_settings","recents","sleep"}){
   JSONObject source=new JSONObject().put("repeat",1000).put("steps",new JSONArray().put(step(a)));
   ok(TrialPlan.singleStep(source,0).getJSONArray("steps").length()==1,"No image needed for trial "+a);
  }
  System.out.println("PASS "+checks+" single-step trial checks");
 }
}
