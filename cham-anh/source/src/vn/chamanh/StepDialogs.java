package vn.chamanh;
import android.app.*;import android.graphics.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;import java.util.function.*;
/** Small per-action editor; every parameter shown has a label and a bounded range. */
public final class StepDialogs {
 static void show(MainActivity a,JSONObject original,boolean existing,Consumer<JSONObject> save,Runnable delete,Consumer<String> pick){
  final JSONObject source;try{source=new JSONObject(original.toString());}catch(Exception e){a.toast("Không đọc được bước");return;}
  String type=source.optString("action","tap");android.util.DisplayMetrics dm=new android.util.DisplayMetrics();a.getWindowManager().getDefaultDisplay().getRealMetrics(dm);final int w=source.optInt("refWidth",dm.widthPixels),h=source.optInt("refHeight",dm.heightPixels);
  LinearLayout box=a.dialogBox();TextView intro=a.text(StepSpec.title(type),13,MainActivity.BLUE,true);box.addView(intro);a.gap(box,12);
  EditText name=a.input("Tên bước",source.optString("label",StepSpec.title(type)),false);box.addView(a.text("Tên hiển thị",12,MainActivity.MUTED,false));a.gap(box,6);box.addView(name,a.lp(-1,-2));a.gap(box,14);
  final String[] choices=StepSpec.imageAction(type)?new String[]{"tap","wait","image_long_press","image_double_tap"}:"coordinate".equals(type)||"long_press".equals(type)||"double_tap".equals(type)?new String[]{"coordinate","long_press","double_tap"}:new String[]{type};
  Spinner action=new Spinner(a);String[] labels=new String[choices.length];int selection=0;for(int i=0;i<choices.length;i++){labels[i]=StepSpec.title(choices[i]);if(choices[i].equals(type))selection=i;}ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels);action.setAdapter(adapter);action.setSelection(selection);if(choices.length>1){box.addView(a.text("Kiểu thao tác",12,MainActivity.MUTED,false));box.addView(action,a.lp(-1,52));a.gap(box,12);}
  Map<String,EditText> fields=new HashMap<>();
  if(StepSpec.coordinateAction(type)){
   box.addView(a.text("Tọa độ pixel · Màn hình gốc "+w+" × "+h,12,MainActivity.MUTED,false));a.gap(box,8);
   LinearLayout pair=a.row();field(a,pair,fields,"x","X",String.valueOf(StepSpec.pixel(source.optDouble("x",.5),w)));a.space(pair,12);field(a,pair,fields,"y","Y",String.valueOf(StepSpec.pixel(source.optDouble("y",.5),h)));box.addView(pair);a.gap(box,12);
   if("swipe".equals(type)){LinearLayout end=a.row();field(a,end,fields,"x2","X kết thúc",String.valueOf(StepSpec.pixel(source.optDouble("x2",.5),w)));a.space(end,12);field(a,end,fields,"y2","Y kết thúc",String.valueOf(StepSpec.pixel(source.optDouble("y2",.25),h)));box.addView(end);a.gap(box,12);}
   box.addView(a.button("⊕  Chọn trên ảnh màn hình",false,()->{pick.accept(choices[action.getSelectedItemPosition()]);}),a.lp(-1,-2));a.gap(box,12);box.addView(a.text("Gốc (0,0) ở góc trên trái. Giữ cùng hướng và bố cục màn hình khi chạy.",11,MainActivity.MUTED,false));a.gap(box,14);
  }
  if(StepSpec.imageAction(type)||StepSpec.coordinateAction(type)){edit(a,box,fields,"duration","Thời gian nhấn giữ / vuốt (100–5000 ms)",String.valueOf(source.optInt("duration",type.contains("long_press")?800:400)));a.gap(box,12);box.addView(a.text("Giá trị này chỉ áp dụng cho nhấn giữ hoặc vuốt.",11,MainActivity.MUTED,false));a.gap(box,14);}
  if("sleep".equals(type)){edit(a,box,fields,"waitMs","Thời gian chờ (100–600000 ms)",String.valueOf(source.optInt("waitMs",1000)));a.gap(box,14);}
  if("launch_app".equals(type)){box.addView(a.text("Ứng dụng: "+source.optString("package"),12,MainActivity.MUTED,false));a.gap(box,14);}
  edit(a,box,fields,"delay","Nghỉ sau bước này (0–60000 ms)",String.valueOf(source.optInt("delay",500)));a.gap(box,10);box.addView(a.text("1000 ms = 1 giây. Các bước luôn chạy theo thứ tự từ trên xuống.",11,MainActivity.MUTED,false));
  ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.addView(box);
  AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(existing?"Sửa thao tác":"Thêm thao tác").setView(scroll).setPositiveButton(existing?"Lưu bước":"Thêm vào kịch bản",null).setNegativeButton("Hủy",null);if(existing)builder.setNeutralButton("Xóa",null);
  AlertDialog dialog=builder.create();
  // Point picking opens another flow; dismiss this editor before leaving it.
  for(int i=0;i<box.getChildCount();i++){View v=box.getChildAt(i);if(v instanceof TextView&&((TextView)v).getText().toString().startsWith("⊕"))v.setOnClickListener(z->{dialog.dismiss();pick.accept(choices[action.getSelectedItemPosition()]);});}
  dialog.setOnShowListener(v->{a.style(dialog);dialog.getButton(-1).setOnClickListener(z->{try{
   String label=name.getText().toString().trim();if(label.isEmpty()){name.setError("Nhập tên bước");return;}JSONObject out=new JSONObject(source.toString());String chosen=choices[action.getSelectedItemPosition()];out.put("action",chosen).put("label",label);
   for(Map.Entry<String,EditText> e:fields.entrySet()){long n=Long.parseLong(e.getValue().getText().toString());if("x".equals(e.getKey())||"x2".equals(e.getKey())){if(n<0||n>=w)throw new IllegalArgumentException("X phải từ 0 đến "+(w-1));out.put(e.getKey(),n/(double)Math.max(1,w-1));}else if("y".equals(e.getKey())||"y2".equals(e.getKey())){if(n<0||n>=h)throw new IllegalArgumentException("Y phải từ 0 đến "+(h-1));out.put(e.getKey(),n/(double)Math.max(1,h-1));}else{if(n>Integer.MAX_VALUE||n<0)throw new IllegalArgumentException("Giá trị số không hợp lệ");out.put(e.getKey(),(int)n);}}
   if(StepSpec.coordinateAction(chosen))out.put("refWidth",w).put("refHeight",h);StepSpec.from(out);save.accept(out);dialog.dismiss();
  }catch(NumberFormatException e){a.toast("Nhập số nguyên hợp lệ trong các ô thiết lập");}catch(Exception e){a.toast(e.getMessage()==null?"Không lưu được bước":e.getMessage());}});if(existing)dialog.getButton(-3).setOnClickListener(z->a.confirm("Xóa bước?","Bước này sẽ được gỡ khỏi kịch bản.",()->{delete.run();dialog.dismiss();}));});dialog.show();
 }
 static void field(MainActivity a,LinearLayout parent,Map<String,EditText> map,String key,String label,String value){LinearLayout c=a.col();c.addView(a.text(label,12,MainActivity.MUTED,false));a.gap(c,6);EditText e=a.input("",value,true);map.put(key,e);c.addView(e,a.lp(-1,-2));parent.addView(c,new LinearLayout.LayoutParams(0,-2,1));}
 static void edit(MainActivity a,LinearLayout box,Map<String,EditText> map,String key,String label,String value){box.addView(a.text(label,12,MainActivity.MUTED,false));a.gap(box,6);EditText e=a.input("",value,true);map.put(key,e);box.addView(e,a.lp(-1,-2));}
}
