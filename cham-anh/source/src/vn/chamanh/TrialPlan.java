package vn.chamanh;
import org.json.*;
/** Build an isolated one-shot run; never write the trial plan into the saved library. */
public final class TrialPlan {
 private TrialPlan(){}
 public static JSONObject singleStep(JSONObject macro,int index){
  if(macro==null)throw new IllegalArgumentException("Không tìm thấy kịch bản");
  JSONArray saved=macro.optJSONArray("steps");
  if(saved==null||index<0||index>=saved.length())throw new IllegalArgumentException("Không tìm thấy bước cần thử");
  try{
   JSONObject selected=saved.optJSONObject(index);StepSpec.from(selected);
   JSONObject trial=new JSONObject(macro.toString());
   trial.put("steps",new JSONArray().put(new JSONObject(selected.toString())));
   trial.put("repeat",1).put("infinite",false).put("trialStep",index+1);
   return trial;
  }catch(JSONException e){throw new IllegalArgumentException("Không đọc được bước cần thử",e);}
 }
}
