package vn.chamanh;

import org.json.JSONObject;

public final class MissingPolicyTest {
    private static int checks;
    private static void ok(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static JSONObject json(String value) { return new JSONObject(value); }
    private static void bad(JSONObject config, String label) {
        boolean thrown=false;
        try { MissingPolicy.mode(config); } catch (IllegalArgumentException e) { thrown=true; }
        ok(thrown,label);
    }
    public static void main(String[] args) {
        ok("skip".equals(MissingPolicy.mode(json("{}"))), "Missing setting defaults to skip");
        ok(MissingPolicy.skip(json("{}")), "Default skip boolean");
        ok("skip".equals(MissingPolicy.mode(json("{\"onMissing\":\"skip\"}"))), "Explicit skip accepted");
        ok(MissingPolicy.skip(json("{\"onMissing\":\"skip\"}")), "Explicit skip boolean");
        ok("stop".equals(MissingPolicy.mode(json("{\"onMissing\":\"stop\"}"))), "Explicit stop accepted");
        ok(!MissingPolicy.skip(json("{\"onMissing\":\"stop\"}")), "Stop is not skip");
        ok("stop".equals(MissingPolicy.mode(json("{\"trialStep\":1}"))), "Trial stops even with legacy default");
        ok("stop".equals(MissingPolicy.mode(json("{\"trialStep\":9,\"onMissing\":\"skip\"}"))), "Trial overrides skip policy");
        ok(!MissingPolicy.skip(json("{\"trialStep\":1,\"onMissing\":\"skip\"}")), "Trial skip boolean is false");
        ok("stop".equals(MissingPolicy.mode(json("{\"trialStep\":2,\"onMissing\":\"invalid\"}"))), "Trial overrides invalid unrelated saved policy");
        ok("skip".equals(MissingPolicy.mode(json("{\"trialStep\":0}"))), "Zero trial marker remains normal");
        ok("skip".equals(MissingPolicy.mode(json("{\"trialStep\":-1}"))), "Negative trial marker remains normal");
        ok("skip".equals(MissingPolicy.mode(json("{\"infinite\":true,\"repeat\":1000}"))), "Infinite normal macro still skips absent images");
        bad(null,"Null macro rejected");
        bad(json("{\"onMissing\":\"unknown\"}"),"Unknown policy rejected");
        bad(json("{\"onMissing\":\"SKIP\"}"),"Policy is case sensitive");
        bad(json("{\"onMissing\":\"skip \"}"),"Whitespace is not a valid policy");
        bad(json("{\"onMissing\":\"\"}"),"Empty policy rejected");
        bad(json("{\"onMissing\":null}"),"Explicit null policy rejected");
        bad(json("{\"onMissing\":true}"),"Boolean policy rejected");
        bad(json("{\"onMissing\":1}"),"Numeric policy rejected");
        bad(json("{\"onMissing\":[]}"),"Array policy rejected");
        bad(json("{\"onMissing\":{}}"),"Object policy rejected");
        JSONObject config=json("{\"onMissing\":\"skip\",\"infinite\":true}");
        String before=config.toString();MissingPolicy.mode(config);MissingPolicy.skip(config);
        ok(before.equals(config.toString()),"Reading policy does not mutate macro");
        System.out.println("PASS "+checks+" missing-policy checks");
    }
}
