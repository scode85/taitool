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
  ok(trial.getString("onMissing").equals("stop"),"Trial always reports a missing image as failure");
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
  continuousChecks();
  System.out.println("PASS "+checks+" trial-plan checks");
 }
 static void badContinuous(JSONObject macro,int index,String label){boolean thrown=false;try{TrialPlan.continuousFrom(macro,index);}catch(IllegalArgumentException e){thrown=true;}ok(thrown,label);}
 static void order(JSONObject plan,int[] expected,String name)throws Exception{
  JSONArray a=plan.getJSONArray("steps");ok(a.length()==expected.length,name+" preserves count");
  for(int i=0;i<expected.length;i++){
   JSONObject s=a.getJSONObject(i);ok(s.getInt("sourceStep")==expected[i],name+" source number "+i);
   ok(s.getString("label").equals("step"+expected[i]),name+" action order "+i);
  }
 }
 static void continuousChecks()throws Exception{
  JSONArray steps=new JSONArray();for(int i=1;i<=4;i++)steps.put(step(i==2?"tap":"home").put("label","step"+i).put("image","button.png").put("extra",new JSONObject().put("value",i)));
  JSONObject macro=new JSONObject().put("id",789).put("name","Saved macro").put("repeat",1000).put("infinite",true).put("threshold",96).put("interval",3000).put("timeout",8).put("onMissing","skip").put("trialStep",3).put("steps",steps).put("metadata",new JSONObject().put("nested",new JSONArray().put("original")));
  String before=macro.toString();JSONObject middle=TrialPlan.continuousFrom(macro,2);
  order(middle,new int[]{3,4,1,2},"Middle");
  order(TrialPlan.continuousFrom(macro,0),new int[]{1,2,3,4},"First");
  order(TrialPlan.continuousFrom(macro,3),new int[]{4,1,2,3},"Last");
  ok(middle.getInt("startAtStep")==3,"Selected original number stored");
  ok(!middle.has("trialStep"),"Continuous mode removes one-shot trial marker");
  ok(middle.getBoolean("infinite")&&middle.getInt("repeat")==1000,"Continuous mode preserves infinite and finite saved settings");
  ok(middle.getLong("id")==789&&middle.getString("name").equals("Saved macro"),"Owner id/name retained");
  ok(middle.getInt("threshold")==96&&middle.getInt("interval")==3000&&middle.getInt("timeout")==8,"Continuous image settings retained");
  ok(middle.getString("onMissing").equals("skip"),"Missing-image policy preserved");
  ok(before.equals(macro.toString()),"Rotating does not mutate saved macro");
  middle.getJSONArray("steps").getJSONObject(0).put("label","changed");
  middle.getJSONArray("steps").getJSONObject(0).getJSONObject("extra").put("value",999);
  middle.getJSONObject("metadata").getJSONArray("nested").put(0,"changed");
  ok(steps.getJSONObject(2).getString("label").equals("step3"),"Rotated step isolated from source");
  ok(steps.getJSONObject(2).getJSONObject("extra").getInt("value")==3,"Nested step data isolated");
  ok(macro.getJSONObject("metadata").getJSONArray("nested").getString(0).equals("original"),"Macro metadata isolated");
  steps.getJSONObject(2).put("delay",9999);
  ok(middle.getJSONArray("steps").getJSONObject(0).getInt("delay")==900,"Later saved changes do not alter running snapshot");
  JSONObject finite=new JSONObject().put("repeat",7).put("infinite",false).put("onMissing","stop").put("steps",new JSONArray().put(step("back")));
  JSONObject one=TrialPlan.continuousFrom(finite,0);
  ok(one.getInt("repeat")==7&&!one.getBoolean("infinite"),"Finite repetition preserved");
  ok(one.getString("onMissing").equals("stop"),"Explicit stop policy preserved");
  ok(one.getJSONArray("steps").getJSONObject(0).getInt("sourceStep")==1,"Single-step source number");
  JSONObject legacy=new JSONObject().put("steps",new JSONArray().put(new JSONObject().put("image","legacy.png")));
  JSONObject oldPlan=TrialPlan.continuousFrom(legacy,0);
  ok(oldPlan.getJSONArray("steps").getJSONObject(0).getString("image").equals("legacy.png"),"Legacy tap image preserved");
  ok(!oldPlan.has("repeat")&&!oldPlan.has("infinite")&&!oldPlan.has("onMissing"),"Legacy default keys are not rewritten");
  badContinuous(null,0,"Null continuous macro rejected");
  badContinuous(new JSONObject(),0,"Missing continuous steps rejected");
  badContinuous(new JSONObject().put("steps",new JSONArray()),0,"Empty continuous steps rejected");
  badContinuous(finite,-1,"Negative continuous index rejected");
  badContinuous(finite,1,"Out-of-range continuous index rejected");
  badContinuous(new JSONObject().put("steps",new JSONArray().put(step("home")).put(step("unknown"))),0,"Invalid unselected step rejected for complete run");
  badContinuous(new JSONObject().put("steps",new JSONArray().put(step("home")).put(JSONObject.NULL)),0,"Null unselected step rejected for complete run");
  JSONArray hundred=new JSONArray();for(int i=0;i<100;i++)hundred.put(step("home").put("label","step"+(i+1)));
  JSONObject cap=TrialPlan.continuousFrom(new JSONObject().put("steps",hundred),99);
  ok(cap.getJSONArray("steps").length()==100&&cap.getJSONArray("steps").getJSONObject(0).getInt("sourceStep")==100,"100-step boundary accepted");
  hundred.put(step("home"));badContinuous(new JSONObject().put("steps",hundred),0,"Over 100 steps rejected");
  for(String a:new String[]{"tap","wait","image_long_press","image_double_tap","coordinate","long_press","double_tap","swipe","home","back","notifications","quick_settings","recents","sleep","launch_app"}){
   JSONObject s=step(a);
   if(StepSpec.imageAction(a))s.put("image","sample.png");
   if(StepSpec.coordinateAction(a))s.put("x",.1).put("y",.2).put("x2",.8).put("y2",.9).put("refWidth",1080).put("refHeight",2400);
   if(a.equals("launch_app"))s.put("package","com.example.app");
   JSONObject source=new JSONObject().put("steps",new JSONArray().put(s));
   ok(TrialPlan.continuousFrom(source,0).getJSONArray("steps").getJSONObject(0).getString("action").equals(a),"Continuous action supported: "+a);
  }
 }

}
