package vn.phonecare;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import java.util.Locale;

/**
 * Tracks foreground window changes for an explicitly enabled rest session.
 * Does not inspect view content, capture screens, or dispatch touch gestures.
 */
public final class RestAccessService extends AccessibilityService {
    public static volatile RestAccessService instance;
    public static volatile String foregroundPackage = "";
    public static volatile long foregroundAt = 0;

    private final Handler main = new Handler(Looper.getMainLooper());
    private RestOverlay overlay;
    private String inputMethodPackage = "";

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        foregroundPackage = "";
        foregroundAt = 0;
        refreshInputMethod();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        String className = event.getClassName() == null ? "" : event.getClassName().toString();
        String lower = className.toLowerCase(Locale.ROOT);
        boolean activity = className.endsWith("Activity") || className.contains(".Activity");

        // The full-screen rest cover belongs to our package too. Only our real
        // activity may replace the selected app as the foreground window.
        if (pkg.equals(getPackageName())) {
            if (!className.equals(getPackageName() + ".MainActivity") && !activity) return;
        } else {
            refreshInputMethod();
            if (pkg.equals(inputMethodPackage) || lower.contains("inputmethod")
                    || lower.contains("softinputwindow")) return;
            if (lower.contains("popupwindow") || lower.contains("toast")
                    || lower.endsWith("dialog") || lower.contains("accessibilityoverlay")) return;
            if (pkg.equals("com.android.systemui")) {
                // Notification/quick-settings panels and volume popups are
                // temporary overlays, not a replacement for the current game.
                boolean keyguardOrRecents = lower.contains("keyguard") || lower.contains("lockscreen")
                        || lower.contains("bouncer") || lower.contains("recents");
                if (!keyguardOrRecents && (lower.contains("notificationshade") || lower.contains("notificationpanel")
                        || lower.contains("notificationstack") || lower.contains("statusbar")
                        || lower.contains("quicksettings") || lower.contains(".qs.")
                        || lower.contains("volumedialog"))) return;
                // Keyguard/recents and other genuine full-screen system views
                // must replace the selected app so a rest cover can be hidden.
                if (!event.isFullScreen() && !activity) return;
            } else if (!event.isFullScreen() && !activity) {
                return;
            }
        }
        foregroundPackage = pkg;
        foregroundAt = SystemClock.elapsedRealtime();
        CareService.poke();
    }

    private void refreshInputMethod() {
        try {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            ComponentName component = id == null ? null : ComponentName.unflattenFromString(id);
            inputMethodPackage = component == null ? "" : component.getPackageName();
        } catch (RuntimeException ignored) {
            // Foreground events remain usable on OEM builds hiding this setting.
        }
    }

    /** CareService calls this on the main thread while a selected-app rest is active. */
    public boolean showRest(long untilElapsed) {
        if (Looper.myLooper() != Looper.getMainLooper() || instance != this) return false;
        if (overlay == null) {
            WindowManager manager = getSystemService(WindowManager.class);
            if (manager == null) return false;
            overlay = new RestOverlay(this, manager, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        }
        return overlay.show(untilElapsed);
    }

    public void hideRest() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(this::hideRest);
            return;
        }
        if (overlay != null) overlay.hide();
    }

    /** Only invoked by the monitoring service for an active selected-app violation. */
    public boolean goHome() {
        return Looper.myLooper() == Looper.getMainLooper() && instance == this
                && performGlobalAction(GLOBAL_ACTION_HOME);
    }

    private void clearConnection() {
        main.removeCallbacksAndMessages(null);
        hideRest();
        if (instance == this) {
            instance = null;
            foregroundPackage = "";
            foregroundAt = 0;
        }
    }

    @Override public void onInterrupt() { hideRest(); foregroundPackage = ""; foregroundAt = 0; CareService.poke(); }
    @Override public boolean onUnbind(Intent intent) {
        clearConnection();
        return super.onUnbind(intent);
    }
    @Override public void onDestroy() {
        clearConnection();
        overlay = null;
        super.onDestroy();
    }
}
