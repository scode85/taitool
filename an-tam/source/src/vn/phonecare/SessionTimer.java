package vn.phonecare;

/**
 * Pure monotonic session accounting. Call tick whenever observations change or a
 * poll arrives. Each interval belongs to the PREVIOUS observed screen/app state.
 * This class is confined to its caller's thread and has no Android dependencies.
 */
public final class SessionTimer {
    private final long limitMs;
    private final long restMs;
    private long used;
    private long lastElapsed;
    private long restUntil;
    private long offSince;
    private boolean baseline;
    private boolean observed;
    private boolean previousScreenOn;
    private boolean previousSelected;
    private boolean offTracking;
    private boolean inRest;
    private boolean warned;
    private boolean warningPending;
    private boolean restStartedPending;

    public SessionTimer(long limitMs, long restMs) {
        this(limitMs, restMs, 0, 0, 0);
        baseline = false;
    }

    /**
     * Restore a same-boot snapshot without charging app/service downtime as use.
     * Persist usedMs() and restRemainingMs(snapshotNow) together. The caller must
     * reduce remaining rest by elapsed downtime before restoring it. A positive
     * remaining rest restores the rest phase and does not replay notifications.
     * Used time is capped at the current limit; remaining rest at current restMs.
     */
    public SessionTimer(long limitMs, long restMs, long initialUsedMs,
                        long restRemainingMs, long nowElapsed) {
        if (limitMs <= 0 || restMs <= 0)
            throw new IllegalArgumentException("Limit and rest duration must be positive");
        if (initialUsedMs < 0 || restRemainingMs < 0)
            throw new IllegalArgumentException("Restored durations must not be negative");
        this.limitMs = limitMs;
        this.restMs = restMs;
        this.used = Math.min(initialUsedMs, limitMs);
        lastElapsed = Math.max(0, nowElapsed);
        baseline = true;
        if (restRemainingMs > 0) {
            inRest = true;
            used = limitMs;
            restUntil = add(lastElapsed, Math.min(restMs, restRemainingMs));
        }
        // Never replay a warning merely because the process was recreated.
        warned = used > 0 && used >= warningAt();
    }

    public void tick(long nowElapsed, boolean screenOn, boolean selected) {
        long now = monotonic(nowElapsed);
        if (!observed) {
            if (inRest && now >= restUntil) resetSession();
            // A snapshot at the limit with no active rest still owes a break.
            if (!inRest && used >= limitMs) beginRest(now);
            observe(now, screenOn, selected);
            return;
        }

        long elapsed = now - lastElapsed;
        if (inRest) {
            if (now >= restUntil) resetSession();
            // Do not charge the interval in which the rest overlay was active,
            // including a delayed callback that first observes its expiry.
            observe(now, screenOn, selected);
            return;
        }

        if (previousScreenOn && previousSelected && elapsed > 0) {
            used = Math.min(limitMs, add(used, elapsed));
            if (used >= limitMs) {
                beginRest(now);
            } else if (!warned && used > 0 && used >= warningAt()) {
                warned = true;
                warningPending = true;
            }
        }

        // Check the state of the interval, so returning to a screen that was off
        // long enough also resets the old session immediately.
        if (!previousScreenOn && offTracking && now - offSince >= restMs)
            resetSession();
        observe(now, screenOn, selected);
    }

    public long usedMs() {
        return used;
    }

    /** Read-only: expiry is committed/reset by the next tick. */
    public long restRemainingMs(long nowElapsed) {
        if (!inRest) return 0;
        long now = monotonic(nowElapsed);
        return now >= restUntil ? 0 : restUntil - now;
    }

    public boolean resting(long nowElapsed) {
        return restRemainingMs(nowElapsed) > 0;
    }

    public boolean consumeWarning() {
        boolean value = warningPending;
        warningPending = false;
        return value;
    }

    public boolean consumeRestStarted() {
        boolean value = restStartedPending;
        restStartedPending = false;
        return value;
    }

    private long warningAt() {
        return Math.max(0, limitMs - 60000L);
    }

    private void beginRest(long now) {
        inRest = true;
        restUntil = add(now, restMs);
        warningPending = false;
        restStartedPending = true;
    }

    private void resetSession() {
        used = 0;
        inRest = false;
        restUntil = 0;
        warned = false;
        warningPending = false;
        // A rest-start event remains latched until explicitly consumed.
    }

    private void observe(long now, boolean screenOn, boolean selected) {
        if (!screenOn) {
            if (!offTracking || !observed || previousScreenOn) offSince = now;
            offTracking = true;
        } else {
            offTracking = false;
        }
        previousScreenOn = screenOn;
        previousSelected = selected;
        lastElapsed = now;
        baseline = true;
        observed = true;
    }

    private long monotonic(long now) {
        now = Math.max(0, now);
        return baseline ? Math.max(lastElapsed, now) : now;
    }

    private static long add(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }
}
