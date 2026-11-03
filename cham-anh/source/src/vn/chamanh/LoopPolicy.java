package vn.chamanh;

import org.json.JSONObject;

/** Shared finite/infinite repetition semantics for the editor and macro runner. */
public final class LoopPolicy {
    private LoopPolicy() {}

    /**
     * Zero means unlimited repetitions. A trial always executes once, even if the
     * saved macro is unlimited or its finite repeat setting is malformed.
     */
    public static int limit(JSONObject macro) {
        if (macro == null) throw new IllegalArgumentException("Không tìm thấy kịch bản");
        if (macro.optInt("trialStep", 0) > 0) return 1;
        if (macro.optBoolean("infinite", false)) return 0;
        // Missing repeat is the legacy one-shot default. Present invalid values
        // must not silently become 1 or truncate fractional numbers.
        double value = macro.has("repeat") ? macro.optDouble("repeat", Double.NaN) : 1.0;
        if (Double.isNaN(value) || Double.isInfinite(value)
                || value != Math.floor(value) || value < 1 || value > 1000)
            throw new IllegalArgumentException("Số vòng lặp phải là số nguyên từ 1 đến 1000");
        return (int) value;
    }

    public static boolean finished(long completed, int limit) {
        return limit > 0 && completed >= limit;
    }

    /** Increment display/progress counts without wrapping to a negative number. */
    public static long next(long completed) {
        return completed == Long.MAX_VALUE ? Long.MAX_VALUE : completed + 1;
    }

    public static String progress(long completed, int limit) {
        return "Vòng " + next(completed) + (limit == 0 ? " · Vô hạn" : "/" + limit);
    }
}
