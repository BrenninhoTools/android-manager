package androidmanager;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import org.haxe.extension.Extension;

public class Vibration {

    private static int usage = -1;

    public static void setUsage(int value) {
        usage = value;
    }

    public static int getUsage() {
        return usage;
    }

    @SuppressWarnings("deprecation")
    private static void play(Vibrator v, VibrationEffect effect) {
        if (usage < 0 || Build.VERSION.SDK_INT < 26) {
            v.vibrate(effect);
            return;
        }

        if (Build.VERSION.SDK_INT >= 33) {
            int attribute;
            switch (usage) {
                case 0: attribute = VibrationAttributes.USAGE_TOUCH; break;
                case 2: attribute = VibrationAttributes.USAGE_ALARM; break;
                case 3: attribute = VibrationAttributes.USAGE_NOTIFICATION; break;
                case 4: attribute = VibrationAttributes.USAGE_HARDWARE_FEEDBACK; break;
                default: attribute = VibrationAttributes.USAGE_MEDIA; break;
            }
            v.vibrate(effect, new VibrationAttributes.Builder().setUsage(attribute).build());
            return;
        }

        int audio;
        switch (usage) {
            case 0: audio = AudioAttributes.USAGE_ASSISTANCE_SONIFICATION; break;
            case 2: audio = AudioAttributes.USAGE_ALARM; break;
            case 3: audio = AudioAttributes.USAGE_NOTIFICATION; break;
            default: audio = AudioAttributes.USAGE_GAME; break;
        }
        v.vibrate(effect, new AudioAttributes.Builder().setUsage(audio).build());
    }

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
                play(v, VibrationEffect.createOneShot(duration, amp));
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

                    play(v, VibrationEffect.createWaveform(pattern, amps, repeat));
                } else {
                    play(v, VibrationEffect.createWaveform(pattern, repeat));
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
                play(v, VibrationEffect.createPredefined(effect));
            else
                vibrate(effect == 5 ? 60 : 20, -1);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static boolean isEffectSupported(int effect) {
        Vibrator v = vibrator();
        if (v == null || Build.VERSION.SDK_INT < 30)
            return false;

        try {
            return v.areAllEffectsSupported(effect) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPrimitiveSupported(int primitive) {
        Vibrator v = vibrator();
        if (v == null || Build.VERSION.SDK_INT < 30)
            return false;

        try {
            return v.areAllPrimitivesSupported(primitive);
        } catch (Exception e) {
            return false;
        }
    }

    public static int getPrimitiveDuration(int primitive) {
        Vibrator v = vibrator();
        if (v == null || Build.VERSION.SDK_INT < 31)
            return -1;

        try {
            int[] durations = v.getPrimitiveDurations(primitive);
            return durations != null && durations.length > 0 ? durations[0] : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean compose(String primitives, String scales, String delays) {
        Vibrator v = vibrator();
        if (v == null || Build.VERSION.SDK_INT < 30 || primitives == null || primitives.isEmpty())
            return false;

        try {
            String[] ids = primitives.split(",");
            String[] scaleParts = scales == null || scales.isEmpty() ? new String[0] : scales.split(",");
            String[] delayParts = delays == null || delays.isEmpty() ? new String[0] : delays.split(",");

            int[] parsed = new int[ids.length];
            for (int i = 0; i < ids.length; i++)
                parsed[i] = Integer.parseInt(ids[i].trim());

            if (!v.areAllPrimitivesSupported(parsed))
                return false;

            VibrationEffect.Composition composition = VibrationEffect.startComposition();
            for (int i = 0; i < parsed.length; i++) {
                float scale = i < scaleParts.length ? Float.parseFloat(scaleParts[i].trim()) : 1f;
                int delay = i < delayParts.length ? Integer.parseInt(delayParts[i].trim()) : 0;
                composition.addPrimitive(parsed[i], Math.max(0f, Math.min(1f, scale)), Math.max(0, delay));
            }

            play(v, composition.compose());
            return true;
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static float getResonantFrequency() {
        Vibrator v = vibrator();
        if (v == null || Build.VERSION.SDK_INT < 31)
            return -1f;

        float value = v.getResonantFrequency();
        return Float.isNaN(value) ? -1f : value;
    }

    public static float getQFactor() {
        Vibrator v = vibrator();
        if (v == null || Build.VERSION.SDK_INT < 31)
            return -1f;

        float value = v.getQFactor();
        return Float.isNaN(value) ? -1f : value;
    }

    public static int getVibratorCount() {
        Context ctx = Utils.context();
        if (ctx == null)
            return 0;

        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getVibratorIds().length : 0;
        }

        return hasVibrator() ? 1 : 0;
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
