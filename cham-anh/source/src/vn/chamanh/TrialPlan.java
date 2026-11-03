package vn.chamanh;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Build isolated execution snapshots; never write a run plan into the saved library. */
public final class TrialPlan {
    private TrialPlan() {}

    /** Try only the selected step once. A missing image is a failed trial. */
    public static JSONObject singleStep(JSONObject macro, int index) {
        if (macro == null) throw new IllegalArgumentException("Không tìm thấy kịch bản");
        JSONArray saved = macro.optJSONArray("steps");
        if (saved == null || index < 0 || index >= saved.length())
            throw new IllegalArgumentException("Không tìm thấy bước cần thử");
        try {
            JSONObject selected = saved.optJSONObject(index);
            StepSpec.from(selected);
            JSONObject trial = new JSONObject(macro.toString());
            trial.put("steps", new JSONArray().put(new JSONObject(selected.toString())));
            trial.put("repeat", 1).put("infinite", false).put("trialStep", index + 1);
            trial.put("onMissing", "stop");
            return trial;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Không đọc được bước cần thử", e);
        }
    }

    /**
     * Run the complete macro starting at the selected step, then wrap to the
     * beginning. All later repetitions retain that rotated order. Keep original
     * one-based step numbers for runtime status and never change saved objects.
     */
    public static JSONObject continuousFrom(JSONObject macro, int index) {
        if (macro == null) throw new IllegalArgumentException("Không tìm thấy kịch bản");
        JSONArray saved = macro.optJSONArray("steps");
        if (saved == null || saved.length() < 1 || saved.length() > 100)
            throw new IllegalArgumentException("Kịch bản cần từ 1 đến 100 bước");
        if (index < 0 || index >= saved.length())
            throw new IllegalArgumentException("Không tìm thấy bước bắt đầu");
        try {
            JSONObject plan = new JSONObject(macro.toString());
            JSONArray rotated = new JSONArray();
            for (int offset = 0; offset < saved.length(); offset++) {
                int source = (index + offset) % saved.length();
                JSONObject original = saved.optJSONObject(source);
                StepSpec.from(original);
                JSONObject copy = new JSONObject(original.toString());
                copy.put("sourceStep", source + 1);
                rotated.put(copy);
            }
            plan.remove("trialStep");
            plan.put("startAtStep", index + 1);
            plan.put("steps", rotated);
            return plan;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Không đọc được kịch bản cần chạy", e);
        }
    }
}
