package vn.phonecare;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Resolves the foreground activity without depending on Android framework classes. */
public final class ForegroundState {
    private static final int MAX_RECORDS = 500;
    private final Map<ActivityKey, ActivityRecord> activities = new HashMap<>();
    private long latestEventTime;
    private long order;
    private long clearedThrough = Long.MIN_VALUE;
    private long forgottenThrough = Long.MIN_VALUE;

    public void resumed(String pkg, String className, int instanceId, long timestamp) {
        apply(pkg, className, instanceId, timestamp, true);
    }

    public void paused(String pkg, String className, int instanceId, long timestamp) {
        apply(pkg, className, instanceId, timestamp, false);
    }

    private void apply(String pkg, String className, int instanceId, long timestamp,
                       boolean resumed) {
        if (pkg == null || pkg.isEmpty() || timestamp <= clearedThrough) return;
        ActivityKey key = new ActivityKey(pkg, className == null ? "" : className, instanceId);
        ActivityRecord record = activities.get(key);
        if (record == null) {
            // Never resurrect an evicted tombstone while replaying an old event window.
            if (timestamp <= forgottenThrough) return;
            record = new ActivityRecord();
            activities.put(key, record);
        } else if (timestamp < record.timestamp) {
            return;
        }
        // Equal timestamps intentionally use the order in which Android delivered events.
        record.timestamp = timestamp;
        record.order = ++order;
        record.resumed = resumed;
        latestEventTime = Math.max(latestEventTime, timestamp);
        trim();
    }

    /** Clears screen-off candidates; subsequent replay through this timestamp is ignored. */
    public void clear(long timestamp) {
        if (timestamp < clearedThrough) return;
        clearedThrough = timestamp;
        latestEventTime = Math.max(latestEventTime, timestamp);
        Iterator<ActivityRecord> it = activities.values().iterator();
        while (it.hasNext()) {
            if (it.next().timestamp <= timestamp) it.remove();
        }
    }

    /** Returns the newest still-resumed package, or an empty string when none is known. */
    public String current() {
        String pkg = "";
        ActivityRecord newest = null;
        for (Map.Entry<ActivityKey, ActivityRecord> entry : activities.entrySet()) {
            ActivityRecord candidate = entry.getValue();
            if (candidate.resumed && (newest == null
                    || candidate.timestamp > newest.timestamp
                    || (candidate.timestamp == newest.timestamp && candidate.order > newest.order))) {
                newest = candidate;
                pkg = entry.getKey().pkg;
            }
        }
        return pkg;
    }

    public long latestEventTime() {
        return latestEventTime;
    }

    /** Call after replaying the whole query window; retains active records. */
    public void pruneBefore(long timestamp) {
        if (timestamp == Long.MIN_VALUE) return;
        forgottenThrough = Math.max(forgottenThrough, timestamp - 1);
        Iterator<ActivityRecord> it = activities.values().iterator();
        while (it.hasNext()) {
            ActivityRecord record = it.next();
            if (!record.resumed && record.timestamp < timestamp) it.remove();
        }
    }

    private void trim() {
        while (activities.size() > MAX_RECORDS) {
            ActivityKey oldestKey = null;
            ActivityRecord oldest = null;
            for (Map.Entry<ActivityKey, ActivityRecord> entry : activities.entrySet()) {
                ActivityRecord value = entry.getValue();
                if (oldest == null
                        || (!value.resumed && oldest.resumed)
                        || (value.resumed == oldest.resumed &&
                            (value.timestamp < oldest.timestamp
                             || (value.timestamp == oldest.timestamp && value.order < oldest.order)))) {
                    oldest = value;
                    oldestKey = entry.getKey();
                }
            }
            forgottenThrough = Math.max(forgottenThrough, oldest.timestamp);
            activities.remove(oldestKey);
        }
    }

    private static final class ActivityRecord {
        long timestamp;
        long order;
        boolean resumed;
    }

    private static final class ActivityKey {
        final String pkg;
        final String className;
        final int instanceId;

        ActivityKey(String pkg, String className, int instanceId) {
            this.pkg = pkg;
            this.className = className;
            this.instanceId = instanceId;
        }

        @Override public boolean equals(Object other) {
            if (!(other instanceof ActivityKey)) return false;
            ActivityKey key = (ActivityKey) other;
            return instanceId == key.instanceId && pkg.equals(key.pkg)
                    && className.equals(key.className);
        }

        @Override public int hashCode() {
            return (31 * pkg.hashCode() + className.hashCode()) * 31 + instanceId;
        }
    }
}
