package androidmanager;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import org.haxe.extension.Extension;

public class Vibration {

    @SuppressWarnings("deprecation")
    private static Vibrator vibrator() {
        Context ctx = Utils.context();
        if (ctx == null)
            return null;

        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static boolean hasVibrator() {
        Vibrator v = vibrator();
        return v != null && v.hasVibrator();
    }

    public static boolean hasAmplitudeControl() {
        Vibrator v = vibrator();
        return v != null && Build.VERSION.SDK_INT >= 26 && v.hasAmplitudeControl();
    }

    @SuppressWarnings("deprecation")
    public static void vibrate(int duration, int amplitude) {
        Vibrator v = vibrator();
        if (v == null || duration <= 0)
            return;

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                int amp = amplitude <= 0 ? VibrationEffect.DEFAULT_AMPLITUDE : Math.min(255, amplitude);
                v.vibrate(VibrationEffect.createOneShot(duration, amp));
            } else {
                v.vibrate(duration);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    @SuppressWarnings("deprecation")
    public static void vibratePattern(String timings, String amplitudes, int repeat) {
        Vibrator v = vibrator();
        if (v == null || timings == null || timings.isEmpty())
            return;

        try {
            String[] parts = timings.split(",");
            long[] pattern = new long[parts.length];
            for (int i = 0; i < parts.length; i++)
                pattern[i] = Long.parseLong(parts[i].trim());

            if (Build.VERSION.SDK_INT >= 26) {
                if (amplitudes != null && !amplitudes.isEmpty() && v.hasAmplitudeControl()) {
                    String[] ampParts = amplitudes.split(",");
                    int[] amps = new int[pattern.length];
                    for (int i = 0; i < amps.length; i++)
                        amps[i] = i < ampParts.length ? Math.max(0, Math.min(255, Integer.parseInt(ampParts[i].trim()))) : 0;

                    v.vibrate(VibrationEffect.createWaveform(pattern, amps, repeat));
                } else {
                    v.vibrate(VibrationEffect.createWaveform(pattern, repeat));
                }
            } else {
                v.vibrate(pattern, repeat);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void vibratePredefined(int effect) {
        Vibrator v = vibrator();
        if (v == null)
            return;

        try {
            if (Build.VERSION.SDK_INT >= 29)
                v.vibrate(VibrationEffect.createPredefined(effect));
            else
                vibrate(effect == 5 ? 60 : 20, -1);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void cancel() {
        Vibrator v = vibrator();
        if (v != null)
            v.cancel();
    }

    public static void performHapticFeedback(final int feedback) {
        Utils.runOnUi(() -> {
            View view = Extension.mainView;
            if (view == null && Utils.activity() != null)
                view = Utils.activity().getWindow().getDecorView();

            if (view != null)
                view.performHapticFeedback(feedback < 0 ? HapticFeedbackConstants.VIRTUAL_KEY : feedback, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        });
    }
}
