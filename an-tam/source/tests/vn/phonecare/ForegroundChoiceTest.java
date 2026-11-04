package vn.phonecare;

public final class ForegroundChoiceTest {
    private static int checks;

    public static void main(String[] args) {
        equal("game", choose("an.tam", 1000, "game", 199900, 10000, 200000),
                "Stale accessibility own app cannot override fresh game event");
        equal("launcher", choose("launcher", 9900, "game", 195000, 10000, 200000),
                "Fresh accessibility Home wins over older usage game");
        equal("", choose("game", 1000, "", 199900, 10000, 200000),
                "Fresh usage pause clears stale accessibility game");
        equal("game", choose("game", 8500, "game", 199000, 10000, 200000),
                "Same-package activity transition inside grace stays on game");
        equal("game", choose("game", 8500, "other", 199000, 10000, 200000),
                "Accessibility retains precedence inside one-second grace");
        equal("game", choose("game", 8000, "other", 199000, 10000, 200000),
                "Grace is inclusive at exactly one second");
        equal("other", choose("game", 7999, "other", 199000, 10000, 200000),
                "Usage wins beyond grace");
        equal("", choose(null, 0, null, 0, 10000, 200000), "Both absent remains empty");
        equal("game", choose(null, 1000, "game", 199000, 10000, 200000),
                "Missing accessibility uses usage package");
        equal("game", choose("launcher", 0, "game", 199000, 10000, 200000),
                "Nonpositive accessibility timestamp is unavailable");
        equal("launcher", choose("launcher", 9000, "game", 0, 10000, 200000),
                "Missing usage timestamp leaves valid accessibility");
        equal("launcher", choose("launcher", 9000, "game", -1, 10000, 200000),
                "Negative usage timestamp cannot win");
        equal("game", choose("launcher", -1, "game", 199000, 10000, 200000),
                "Negative accessibility timestamp cannot win");
        equal("launcher", choose("launcher", 2000, "game", 205000, 10000, 200000),
                "Future usage timestamp beyond grace is untrusted after clock rollback");
        equal("game", choose("launcher", 2000, "game", 200500, 10000, 200000),
                "Small future wall skew is clamped to now");
        equal("game", choose("launcher", 15000, "game", 199000, 10000, 200000),
                "Future accessibility timestamp beyond grace is untrusted");
        equal("launcher", choose("launcher", 10500, "game", 199000, 10000, 200000),
                "Small future elapsed skew is clamped to now");
        equal("launcher", choose("launcher", 1, "ancient", 1, 10000, Long.MAX_VALUE),
                "Ancient wall timestamp cannot underflow elapsed time");
        equal("launcher", choose("launcher", Long.MAX_VALUE, "game", Long.MAX_VALUE - 1,
                Long.MAX_VALUE, Long.MAX_VALUE), "Maximum clocks do not overflow grace check");
        equal("game", choose("launcher", 0, "game", 1, -1, -1),
                "Invalid current clocks do not crash or invent accessibility state");
        System.out.println("ForegroundChoiceTest: " + checks + " checks passed");
    }

    private static String choose(String access, long accessAt, String usage, long usageAt,
                                 long elapsedNow, long wallNow) {
        return ForegroundChoice.choose(access, accessAt, usage, usageAt, elapsedNow, wallNow);
    }

    private static void equal(String expected, String actual, String message) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
