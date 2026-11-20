package androidmanager;

import android.app.GameManager;
import android.app.GameState;
import android.content.Context;
import android.os.Build;
import android.os.PerformanceHintManager;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

public class PerformanceManager {

    private static PerformanceHintManager.Session session = null;

    private static long targetNanos = 0;

    private static PowerManager.WakeLock cpuLock = null;

    public static int getGameMode() {
        Context ctx = Utils.context();
        if (ctx == null || Build.VERSION.SDK_INT < 31)
            return 0;

        try {
            GameManager manager = (GameManager) ctx.getSystemService(Context.GAME_SERVICE);
            return manager != null ? manager.getGameMode() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean setGameState(boolean loading, int mode) {
        Context ctx = Utils.context();
        if (ctx == null || Build.VERSION.SDK_INT < 33)
            return false;

        try {
            GameManager manager = (GameManager) ctx.getSystemService(Context.GAME_SERVICE);
            if (manager == null)
                return false;

            manager.setGameState(new GameState(loading, mode));
            return true;
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static boolean isHintSupported() {
        Context ctx = Utils.context();
        if (ctx == null || Build.VERSION.SDK_INT < 31)
            return false;

        PerformanceHintManager manager = (PerformanceHintManager) ctx.getSystemService(Context.PERFORMANCE_HINT_SERVICE);
        return manager != null && manager.getPreferredUpdateRateNanos() > 0;
    }

    public static boolean startHintSession(double target) {
        Context ctx = Utils.context();
        if (ctx == null || Build.VERSION.SDK_INT < 31)
            return false;

        try {
            closeHintSession();

            PerformanceHintManager manager = (PerformanceHintManager) ctx.getSystemService(Context.PERFORMANCE_HINT_SERVICE);
            if (manager == null)
                return false;

            targetNanos = Math.max(1000000L, (long) target);
            session = manager.createHintSession(new int[] { Process.myTid() }, targetNanos);
            return session != null;
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            session = null;
            return false;
        }
    }

    public static boolean hasHintSession() {
        return session != null;
    }

    public static void updateHintTarget(double target) {
        if (session == null || Build.VERSION.SDK_INT < 31)
            return;

        try {
            long value = Math.max(1000000L, (long) target);
            if (value == targetNanos)
                return;

            targetNanos = value;
            session.updateTargetWorkDuration(targetNanos);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void reportHintDuration(double actual) {
        if (session == null || Build.VERSION.SDK_INT < 31 || actual <= 0)
            return;

        try {
            session.reportActualWorkDuration((long) actual);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void closeHintSession() {
        if (session == null)
            return;

        try {
            session.close();
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        session = null;
        targetNanos = 0;
    }

    public static boolean setCpuAwake(boolean enable) {
        Context ctx = Utils.context();
        if (ctx == null)
            return false;

        try {
            if (enable) {
                if (cpuLock == null) {
                    PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                    if (pm == null)
                        return false;

                    cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Utils.packageName() + ":performance");
                    cpuLock.setReferenceCounted(false);
                }
                if (!cpuLock.isHeld())
                    cpuLock.acquire(10 * 60 * 1000L);
            } else if (cpuLock != null && cpuLock.isHeld()) {
                cpuLock.release();
            }
            return true;
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static boolean isSustainedPerformanceSupported() {
        Context ctx = Utils.context();
        if (ctx == null || Build.VERSION.SDK_INT < 24)
            return false;

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isSustainedPerformanceModeSupported();
    }
}
