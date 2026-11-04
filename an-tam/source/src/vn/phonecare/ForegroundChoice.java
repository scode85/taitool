package vn.phonecare;

/** Chooses the freshest foreground observation across elapsed and wall clock sources. */
public final class ForegroundChoice {
    private static final long ACCESS_GRACE_MILLIS = 1000;

    private ForegroundChoice() { }

    public static String choose(String accessPkg, long accessElapsed,
                                String usagePkg, long usageWall,
                                long nowElapsed, long nowWall) {
        String usage = usagePkg == null ? "" : usagePkg;
        String access = accessPkg == null ? "" : accessPkg;
        long elapsedNow = Math.max(0, nowElapsed);
        long wallNow = Math.max(0, nowWall);
        if (access.isEmpty() || accessElapsed <= 0
                || beyondClockGrace(accessElapsed, elapsedNow)) {
            return usage;
        }
        if (usageWall <= 0 || beyondClockGrace(usageWall, wallNow)) {
            // A wall-clock rollback must not keep an old usage event fresh forever.
            return access;
        }
        long age = usageWall >= wallNow ? 0 : wallNow - usageWall;
        // Both clocks are nonnegative here. Clamping avoids underflow for ancient events.
        long usageAt = age >= elapsedNow ? 0 : elapsedNow - age;
        long accessAt = Math.min(accessElapsed, elapsedNow);
        return accessAt >= usageAt - ACCESS_GRACE_MILLIS ? access : usage;
    }

    private static boolean beyondClockGrace(long timestamp, long now) {
        return timestamp > now && timestamp - now > ACCESS_GRACE_MILLIS;
    }
}
