package vn.chamanh;

import org.json.JSONObject;

/** JVM tests: no Android runtime and no screen/control permissions required. */
public final class LoopPolicyTest {
    private static int checks;
    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
    }
    private static JSONObject json(String value) { return new JSONObject(value); }
    private static void rejects(String config, String name) {
        boolean rejected = false;
        try { LoopPolicy.limit(json(config)); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, name);
    }

    public static void main(String[] args) {
        check(LoopPolicy.limit(json("{}")) == 1, "Legacy missing repeat defaults to one");
        check(LoopPolicy.limit(json("{\"repeat\":3}")) == 3, "Finite count read");
        check(LoopPolicy.limit(json("{\"repeat\":1}")) == 1, "Minimum count");
        check(LoopPolicy.limit(json("{\"repeat\":1000}")) == 1000, "Maximum count");
        check(LoopPolicy.limit(json("{\"repeat\":7.0}")) == 7, "Integer-valued JSON number");
        check(LoopPolicy.limit(json("{\"repeat\":\"12\"}")) == 12, "Compatible numeric string");
        check(LoopPolicy.limit(json("{\"repeat\":4,\"infinite\":false}")) == 4, "Explicit finite mode");
        check(!LoopPolicy.finished(0, 1), "Finite not started");
        check(!LoopPolicy.finished(2, 3), "Finite one remaining");
        check(LoopPolicy.finished(3, 3), "Finite finishes at limit");
        check(LoopPolicy.finished(7, 3), "Finite stays finished beyond limit");
        check(LoopPolicy.finished(1000, 1000), "Boundary count finishes");
        check(!LoopPolicy.finished(999, 1000), "Boundary count not early");
        check(LoopPolicy.limit(json("{\"infinite\":true,\"repeat\":3}")) == 0, "Unlimited mode wins over saved repeat");
        check(LoopPolicy.limit(json("{\"infinite\":true,\"repeat\":-7}")) == 0, "Unused invalid finite value ignored in unlimited mode");
        check(!LoopPolicy.finished(1001, 0), "Unlimited survives old 1000 boundary");
        check(!LoopPolicy.finished(10000000000L, 0), "Unlimited survives many cycles");
        check(!LoopPolicy.finished(Long.MAX_VALUE, 0), "Unlimited never finishes due to counter saturation");
        check(LoopPolicy.limit(json("{\"trialStep\":1,\"infinite\":true,\"repeat\":0}")) == 1, "Trial overrides unlimited plus invalid repeat");
        check(LoopPolicy.limit(json("{\"trialStep\":100,\"infinite\":true,\"repeat\":\"garbage\"}")) == 1, "Any positive trial marker overrides malformed repeat");
        check(LoopPolicy.limit(json("{\"trialStep\":1,\"repeat\":2.5}")) == 1, "Trial ignores fractional finite setting");
        check(LoopPolicy.limit(json("{\"trialStep\":0,\"repeat\":8}")) == 8, "Zero trial marker is normal macro");
        check(LoopPolicy.limit(json("{\"trialStep\":-1,\"repeat\":8}")) == 8, "Negative trial marker is normal macro");
        check("Vòng 1/3".equals(LoopPolicy.progress(0, 3)), "Finite first progress");
        check("Vòng 3/3".equals(LoopPolicy.progress(2, 3)), "Finite final progress");
        check("Vòng 1002 · Vô hạn".equals(LoopPolicy.progress(1001, 0)), "Unlimited progress after many cycles");
        check(LoopPolicy.next(0) == 1, "First increment");
        check(LoopPolicy.next(Long.MAX_VALUE - 1) == Long.MAX_VALUE, "Increment reaches maximum");
        check(LoopPolicy.next(Long.MAX_VALUE) == Long.MAX_VALUE, "Increment saturates at maximum");
        check(("Vòng " + Long.MAX_VALUE + " · Vô hạn").equals(LoopPolicy.progress(Long.MAX_VALUE, 0)), "Progress cannot overflow negative");
        rejects("{\"repeat\":0}", "Zero finite repeat invalid");
        rejects("{\"repeat\":-1}", "Negative finite repeat invalid");
        rejects("{\"repeat\":1001}", "Beyond finite cap invalid");
        rejects("{\"repeat\":1.5}", "Fractional repeat cannot truncate");
        rejects("{\"repeat\":\"NaN\"}", "NaN string invalid");
        rejects("{\"repeat\":\"Infinity\"}", "Infinity string invalid");
        rejects("{\"repeat\":\"-Infinity\"}", "Negative infinity invalid");
        rejects("{\"repeat\":\"abc\"}", "Non-numeric repeat invalid");
        rejects("{\"repeat\":true}", "Boolean repeat invalid");
        rejects("{\"repeat\":null}", "Present null repeat invalid");
        rejects("{\"repeat\":[]}", "Array repeat invalid");
        rejects("{\"repeat\":{}}", "Object repeat invalid");
        rejects("{\"repeat\":9223372036854775807}", "Huge number cannot overflow into valid int");
        boolean rejected = false;
        try { LoopPolicy.limit(null); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Null macro rejected");
        System.out.println("PASS " + checks + " loop-policy checks");
    }
}
