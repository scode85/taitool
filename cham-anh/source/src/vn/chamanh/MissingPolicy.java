package vn.chamanh;

import org.json.JSONObject;

/** What a normal run should do after an image step reaches its search timeout. */
public final class MissingPolicy {
    private MissingPolicy() {}

    public static String mode(JSONObject macro) {
        if (macro == null) throw new IllegalArgumentException("Không tìm thấy kịch bản");
        // A trial must report that its one requested action did not happen.
        if (macro.optInt("trialStep", 0) > 0) return "stop";
        if (!macro.has("onMissing")) return "skip";
        Object value = macro.opt("onMissing");
        if ("skip".equals(value)) return "skip";
        if ("stop".equals(value)) return "stop";
        throw new IllegalArgumentException("Khi không thấy ảnh phải chọn bỏ qua hoặc dừng");
    }

    public static boolean skip(JSONObject macro) {
        return "skip".equals(mode(macro));
    }
}
