package vn.phonecare;

public final class SessionTimerTest {
    private static int checks;
    private static void ok(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void eq(long expected, long actual, String label) {
        ok(expected == actual, label + " expected=" + expected + " actual=" + actual);
    }
    private static void firstAndDelayedPolls() {
        SessionTimer t = new SessionTimer(180000, 10000);
        t.tick(500000, true, true);
        eq(0, t.usedMs(), "First observation cannot charge time since boot");
        t.tick(505000, true, true);
        eq(5000, t.usedMs(), "Delayed five-second poll is counted fully");
        t.tick(522000, true, true);
        eq(22000, t.usedMs(), "Seventeen-second poll has no two-second cap");
        ok(!t.consumeWarning(), "No premature one-minute warning");
        ok(!t.consumeRestStarted(), "No premature break");
    }
    private static void appSwitches() {
        SessionTimer t = new SessionTimer(180000, 10000);
        t.tick(1000, true, false);
        t.tick(6000, true, true);
        eq(0, t.usedMs(), "Newly selected app cannot inherit unselected interval");
        t.tick(11000, true, false);
        eq(5000, t.usedMs(), "Leaving app credits previous selected interval");
        t.tick(51000, true, false);
        eq(5000, t.usedMs(), "Unselected screen neither accrues nor resets use");
        t.tick(52000, true, true);
        t.tick(59000, true, true);
        eq(12000, t.usedMs(), "Selected apps share cumulative use after switching");
    }
    private static void warningsAndRest() {
        SessionTimer t = new SessionTimer(120000, 30000);
        t.tick(0, true, true);
        t.tick(59000, true, true);
        ok(!t.consumeWarning(), "Warning not before one-minute remainder");
        t.tick(60000, true, true);
        ok(t.consumeWarning(), "Warning exactly at one-minute remainder");
        ok(!t.consumeWarning(), "Warning is consumed only once");
        t.tick(90000, true, true);
        ok(!t.consumeWarning(), "Later polls do not repeat warning");
        t.tick(120000, true, true);
        eq(120000, t.usedMs(), "Limit reached");
        ok(t.resting(120000), "Limit starts rest immediately");
        eq(30000, t.restRemainingMs(120000), "Full rest begins at observed threshold");
        ok(t.consumeRestStarted(), "Rest-start event latched");
        ok(!t.consumeRestStarted(), "Rest-start event consumed once");
        ok(!t.consumeWarning(), "No contradictory warning after rest starts");
        t.tick(135000, true, true);
        eq(120000, t.usedMs(), "Usage does not grow during rest");
        eq(15000, t.restRemainingMs(135000), "Rest countdown decreases");
        ok(!t.consumeRestStarted(), "No reentry while already resting");
        t.tick(151000, true, true);
        eq(0, t.usedMs(), "Expiry resets use");
        ok(!t.resting(151000), "Expiry leaves rest phase");
        t.tick(156000, true, true);
        eq(5000, t.usedMs(), "Fresh use resumes after expiry observation");
        t.tick(211000, true, true);
        ok(t.consumeWarning(), "New session may warn again");
    }
    private static void overshootAndShortLimit() {
        SessionTimer t = new SessionTimer(10000, 5000);
        t.tick(0, true, true);
        ok(!t.consumeWarning(), "Short limit does not warn before any use");
        t.tick(1000, true, true);
        ok(t.consumeWarning(), "Short limit warns on first elapsed use");
        t.tick(25000, true, true);
        eq(10000, t.usedMs(), "Large poll overshoot caps use at limit");
        eq(5000, t.restRemainingMs(25000), "Delayed callback still requires full rest");
        ok(t.consumeRestStarted(), "Overshoot enters rest once");
        t.tick(26000, true, true);
        ok(!t.consumeRestStarted(), "Overshoot does not continually restart rest");
        SessionTimer leap = new SessionTimer(10000, 5000);
        leap.tick(0, true, true);
        leap.tick(30000, true, true);
        ok(!leap.consumeWarning(), "Direct threshold leap emits rest instead of stale warning");
        ok(leap.consumeRestStarted(), "Direct leap starts rest");
    }
    private static void screenOffReset() {
        SessionTimer t = new SessionTimer(120000, 10000);
        t.tick(0, true, true);
        t.tick(65000, false, false);
        eq(65000, t.usedMs(), "Turning screen off credits prior visible app interval");
        ok(t.consumeWarning(), "Warning reached before screen-off break");
        t.tick(70000, false, true);
        eq(65000, t.usedMs(), "Screen off does not accrue even if selected flag remains true");
        t.tick(75000, true, true);
        eq(0, t.usedMs(), "Returning after continuous rest-length off resets immediately");
        t.tick(76000, true, true);
        eq(1000, t.usedMs(), "Use after screen-off reset starts a fresh session");
        ok(!t.consumeWarning(), "Old warning state cleared after screen-off reset");
        t.tick(81000, false, false);
        t.tick(85000, true, false);
        eq(6000, t.usedMs(), "Short off interval does not reset cumulative use");
        t.tick(86000, false, false);
        t.tick(96000, false, false);
        eq(0, t.usedMs(), "Rest-length off interval resets without a screen-on event");
        SessionTimer rest = new SessionTimer(5000, 10000);
        rest.tick(0, true, true);rest.tick(5000, false, false);
        rest.tick(15000, false, false);
        eq(0, rest.usedMs(), "Active rest expiry resets even while screen stays off");
        ok(!rest.resting(15000), "Screen-off rest expires");
    }
    private static void monotonicEdges() {
        SessionTimer t = new SessionTimer(600000, 10000);
        t.tick(10000, true, true);t.tick(15000, true, true);
        t.tick(12000, true, true);
        eq(5000, t.usedMs(), "Backward poll cannot subtract or add usage");
        t.tick(18000, true, true);
        eq(8000, t.usedMs(), "Backward timestamp never rewinds baseline and double-counts");
        t.tick(-100, true, false);t.tick(20000, true, true);
        eq(8000, t.usedMs(), "Backward observation still updates app state without negative delta");
        t.tick(20000, true, true);
        eq(8000, t.usedMs(), "Equal timestamps add zero");
        SessionTimer edge = new SessionTimer(10, Long.MAX_VALUE);
        edge.tick(Long.MAX_VALUE - 20, true, true);edge.tick(Long.MAX_VALUE - 10, true, true);
        eq(10, edge.usedMs(), "Use near timestamp maximum remains valid");
        eq(10, edge.restRemainingMs(Long.MAX_VALUE - 10), "Rest deadline saturates instead of overflowing");
        ok(edge.restRemainingMs(Long.MAX_VALUE) == 0, "Rest remaining never becomes negative at maximum");
    }
    private static void restore() {
        SessionTimer restored = new SessionTimer(120000, 30000, 50000, 0, 500000);
        restored.tick(510000, true, true);
        eq(50000, restored.usedMs(), "Restore does not charge service downtime");
        restored.tick(520000, true, true);
        eq(60000, restored.usedMs(), "Restored active session accrues new use");
        ok(restored.consumeWarning(), "Restored session can cross a not-yet-issued warning");
        SessionTimer warned = new SessionTimer(120000, 30000, 70000, 0, 500000);
        warned.tick(500000, true, true);warned.tick(501000, true, true);
        ok(!warned.consumeWarning(), "Restore suppresses duplicate old warning");
        SessionTimer resting = new SessionTimer(120000, 30000, 120000, 15000, 500000);
        resting.tick(500000, true, true);
        eq(15000, resting.restRemainingMs(500000), "Restore preserves remaining break");
        ok(!resting.consumeRestStarted(), "Restore does not replay a rest-start notification");
        resting.tick(515000, false, false);
        eq(0, resting.usedMs(), "Restored break expiry resets while off");
        SessionTimer expired = new SessionTimer(120000, 30000, 120000, 1000, 500000);
        expired.tick(510000, true, true);
        eq(0, expired.usedMs(), "First restore observation can expire old break");
        ok(!expired.resting(510000), "Expired restore does not immediately owe another break");
        SessionTimer capped = new SessionTimer(120000, 30000, 130000, 60000, 500000);
        eq(120000, capped.usedMs(), "Restored used value capped at current limit");
        eq(30000, capped.restRemainingMs(500000), "Restored remaining break capped at current duration");
    }
    private static void invalid() {
        long[][] values={{0,1},{1,0},{-1,1},{1,-1}};
        for(long[] pair:values){boolean bad=false;try{new SessionTimer(pair[0],pair[1]);}catch(IllegalArgumentException expected){bad=true;}ok(bad,"Nonpositive setting rejected");}
        boolean bad=false;try{new SessionTimer(1,1,-1,0,0);}catch(IllegalArgumentException expected){bad=true;}ok(bad,"Negative restored use rejected");
        bad=false;try{new SessionTimer(1,1,0,-1,0);}catch(IllegalArgumentException expected){bad=true;}ok(bad,"Negative restored rest rejected");
    }
    public static void main(String[] args) {
        firstAndDelayedPolls();appSwitches();warningsAndRest();overshootAndShortLimit();
        screenOffReset();monotonicEdges();restore();invalid();
        System.out.println("PASS " + checks + " session-timer checks");
    }
}
