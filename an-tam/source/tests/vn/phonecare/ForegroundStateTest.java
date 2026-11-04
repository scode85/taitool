package vn.phonecare;

/** Run with plain Java; no Android framework, test runner, or network required. */
public final class ForegroundStateTest {
    private static int checks;

    public static void main(String[] args) {
        empty();
        oldPauseAfterNewResume();
        differentInstances();
        newestPauseFallsBack();
        replayAndTombstones();
        clearSuppressesReplay();
        tiesUseDeliveryOrder();
        packagesAndPruning();
        boundedHistory();
        System.out.println("ForegroundStateTest: " + checks + " checks passed");
    }

    private static void empty() {
        ForegroundState f = new ForegroundState();
        equal("", f.current(), "No events cannot invent an app");
        equal(0L, f.latestEventTime(), "No event timestamp starts at zero");
        f.resumed(null, null, 0, 1);
        equal("", f.current(), "Invalid package ignored");
    }

    private static void oldPauseAfterNewResume() {
        ForegroundState f = new ForegroundState();
        f.resumed("game", "Menu", 0, 100);
        f.resumed("game", "Play", 0, 200);
        f.paused("game", "Menu", 0, 201);
        equal("game", f.current(), "An old activity pause must not clear its replacement");
        f.paused("game", "Play", 0, 202);
        equal("", f.current(), "Pausing every activity clears package");
    }

    private static void differentInstances() {
        ForegroundState f = new ForegroundState();
        f.resumed("game", "Play", 1, 100);
        f.resumed("game", "Play", 2, 200);
        f.paused("game", "Play", 1, 201);
        equal("game", f.current(), "Same class, different instance stays resumed");
        f.paused("game", "Play", 2, 202);
        equal("", f.current(), "Second instance can independently pause");
    }

    private static void newestPauseFallsBack() {
        ForegroundState f = new ForegroundState();
        f.resumed("left", "Activity", 1, 100);
        f.resumed("right", "Activity", 2, 200);
        equal("right", f.current(), "Newest resumed candidate wins");
        f.paused("right", "Activity", 2, 300);
        equal("left", f.current(), "Pausing newest falls back to another resumed candidate");
    }

    private static void replayAndTombstones() {
        ForegroundState f = new ForegroundState();
        f.resumed("game", "Play", 1, 100);
        f.paused("game", "Play", 1, 200);
        f.resumed("game", "Play", 1, 100);
        equal("", f.current(), "Replayed old resume cannot resurrect a paused activity");
        for (int i = 0; i < 3; i++) {
            f.resumed("game", "Play", 1, 100);
            f.paused("game", "Play", 1, 200);
        }
        equal("", f.current(), "Repeated overlapping query window remains paused");
        equal(200L, f.latestEventTime(), "Stale events cannot move clock backward");
        f.resumed("game", "Play", 1, 250);
        f.paused("game", "Play", 1, 200);
        equal("game", f.current(), "Stale pause cannot clear a newer resume");
    }

    private static void clearSuppressesReplay() {
        ForegroundState f = new ForegroundState();
        f.resumed("game", "Play", 1, 100);
        f.clear(200);
        equal("", f.current(), "Screen off clears foreground");
        f.resumed("game", "Play", 1, 100);
        f.resumed("other", "New", 2, 199);
        f.resumed("game", "Play", 1, 200);
        equal("", f.current(), "Clear suppresses every replay through clear time");
        f.resumed("other", "New", 2, 201);
        equal("other", f.current(), "A genuinely new resume is accepted after clear");
        f.clear(150);
        equal("other", f.current(), "Late older clear cannot delete newer resume");
        equal(201L, f.latestEventTime(), "Clear does not move clock backward");
    }

    private static void tiesUseDeliveryOrder() {
        ForegroundState f = new ForegroundState();
        f.resumed("a", "A", 0, 100);
        f.resumed("b", "B", 0, 100);
        equal("b", f.current(), "Last delivered equal-time resume wins");
        f.paused("b", "B", 0, 100);
        equal("a", f.current(), "Equal-time pause applies after resume");
        f.resumed("b", "B", 0, 100);
        equal("b", f.current(), "Equal-time later resume applies after pause");
    }

    private static void packagesAndPruning() {
        ForegroundState f = new ForegroundState();
        f.resumed("one", "Same", 0, 100);
        f.resumed("two", "Same", 0, 200);
        f.paused("one", "Same", 0, 201);
        equal("two", f.current(), "Package is part of activity identity");
        f.pruneBefore(300);
        f.resumed("one", "Same", 0, 100);
        equal("two", f.current(), "Pruning a tombstone cannot resurrect it from replay");
        f.paused("two", "Same", 0, 301);
        equal("", f.current(), "Pruning keeps active candidate mutable");
        f.resumed("three", null, 0, 302);
        f.paused("three", null, 0, 303);
        equal("", f.current(), "Null class names normalize consistently");
    }

    private static void boundedHistory() {
        ForegroundState f = new ForegroundState();
        for (int i = 1; i <= 700; i++) {
            f.resumed("app" + i, "Activity", 0, i * 10L);
            f.paused("app" + i, "Activity", 0, i * 10L + 1);
        }
        f.resumed("app1", "Activity", 0, 10);
        equal("", f.current(), "Automatic eviction must not resurrect old records");
        f.resumed("fresh", "Activity", 0, 8000);
        equal("fresh", f.current(), "Recent activity accepted after memory cap");
    }

    private static void equal(Object expected, Object actual, String message) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
