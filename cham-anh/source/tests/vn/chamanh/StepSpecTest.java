package vn.chamanh;
import org.json.*;
public final class StepSpecTest {
 static int checks;
 static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static JSONObject step(String a)throws Exception{return new JSONObject().put("action",a).put("label","Thử");}
 static JSONObject point(String a)throws Exception{return step(a).put("x",.5).put("y",.2).put("refWidth",1080).put("refHeight",2400);}
 static void bad(JSONObject o,String name){boolean failed=false;try{StepSpec.from(o);}catch(IllegalArgumentException e){failed=true;}ok(failed,name);}
 public static void main(String[]args)throws Exception {
  for(String a:new String[]{"home","back","notifications","quick_settings","recents","sleep"}){StepSpec p=StepSpec.from(step(a));ok(p.image.isEmpty()&&!StepSpec.imageAction(p.action),"System action needs no image: "+a);}
  for(String a:new String[]{"tap","wait","image_long_press","image_double_tap"})ok(StepSpec.from(step(a).put("image","sample_123.png")).image.equals("sample_123.png"),"Image action "+a);
  StepSpec legacy=StepSpec.from(new JSONObject().put("image","old.png"));ok(legacy.action.equals("tap")&&legacy.delay==500,"Legacy steps supported");
  for(String a:new String[]{"coordinate","long_press","double_tap"})ok(StepSpec.from(point(a)).fitsScreen(1080,2400),"Coordinate action "+a);
  ok(StepSpec.from(point("swipe").put("x2",.7).put("y2",.8)).x2==.7,"Swipe endpoint");
  ok(StepSpec.from(step("launch_app").put("package","com.example.app")).packageName.equals("com.example.app"),"Launcher package");
  bad(step("bogus"),"Unknown action rejected");bad(step("tap"),"Image required");bad(step("tap").put("image","../secret.png"),"No path traversal");
  bad(point("coordinate").put("x",1.1),"Out of bounds X");bad(point("coordinate").put("y",-1),"Out of bounds Y");bad(point("coordinate").put("refWidth",0),"Source resolution required");
  bad(point("coordinate").put("x","not-a-number"),"Invalid numeric coordinate");bad(point("swipe").put("x2",.5).put("y2",.2),"Zero swipe rejected");
  bad(step("sleep").put("waitMs",99),"Sleep too short");bad(step("sleep").put("waitMs",600001),"Sleep too long");ok(StepSpec.from(step("sleep").put("waitMs",600000)).waitMs==600000,"Maximum sleep");
  bad(step("home").put("delay",-1),"Negative delay");bad(step("home").put("delay",60001),"Oversize delay");bad(step("home").put("delay",1.5),"Fractional delay");
  bad(step("launch_app").put("package","no path here"),"Invalid package");
  StepSpec p=StepSpec.from(point("coordinate"));ok(!p.fitsScreen(2400,1080),"Rotated screen rejected");ok(p.fitsScreen(720,1600),"Same aspect resolution allowed");ok(!p.fitsScreen(1080,1920),"Reflow aspect rejected");
  ok(StepSpec.pixel(0,1080)==0&&StepSpec.pixel(1,1080)==1079,"Coordinate boundary stays on display");ok(StepSpec.pixel(.5,1)==0,"One pixel edge");
  RunClock c=new RunClock();ok(c.now(1000)==1000,"Clock starts real");c.setPaused(true,1200);ok(c.now(9000)==1200,"Pause freezes time");c.setPaused(true,9500);ok(c.now(9600)==1200,"Repeated pause does not change baseline");c.setPaused(false,10000);ok(c.now(10100)==1300,"Resume excludes pause");c.setPaused(true,10200);c.setPaused(false,20200);ok(c.now(20300)==1500,"Multiple pauses accumulate");
  long deadline=c.now(20300)+5000;c.setPaused(true,21000);c.setPaused(false,31000);ok(deadline-c.now(31000)==4300,"Pending delay preserved while paused");
  System.out.println("PASS "+checks+" step/clock checks");
 }
}
