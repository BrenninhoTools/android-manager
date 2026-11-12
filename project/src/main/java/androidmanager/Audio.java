package java.androidmanager;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class Audio {

    private static HaxeObject focusListener = null;
    private static AudioFocusRequest focusRequest = null;

    private static final AudioManager.OnAudioFocusChangeListener focusChange = (int change) -> {
        HaxeObject target = focusListener;
        if (target == null)
            return;

        try {
            target.call("onFocusChange", new Object[]{ change });
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    };

    private static AudioManager manager() {
        Context ctx = Utils.context();
        return ctx != null ? (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE) : null;
    }

    public static int getStreamVolume(int stream) {
        AudioManager am = manager();
        return am != null ? am.getStreamVolume(stream) : -1;
    }

    public static int getStreamMaxVolume(int stream) {
        AudioManager am = manager();
        return am != null ? am.getStreamMaxVolume(stream) : -1;
    }

    public static int getStreamMinVolume(int stream) {
        AudioManager am = manager();
        if (am == null)
            return -1;

        return Build.VERSION.SDK_INT >= 28 ? am.getStreamMinVolume(stream) : 0;
    }

    public static void setStreamVolume(int stream, int volume, int flags) {
        AudioManager am = manager();
        if (am == null)
            return;

        try {
            am.setStreamVolume(stream, volume, flags);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void adjustStreamVolume(int stream, int direction, int flags) {
        AudioManager am = manager();
        if (am == null)
            return;

        try {
            am.adjustStreamVolume(stream, direction, flags);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static boolean isStreamMute(int stream) {
        AudioManager am = manager();
        if (am == null)
            return false;

        return Build.VERSION.SDK_INT >= 23 ? am.isStreamMute(stream) : am.getStreamVolume(stream) == 0;
    }

    public static int getRingerMode() {
        AudioManager am = manager();
        return am != null ? am.getRingerMode() : -1;
    }

    public static int getMode() {
        AudioManager am = manager();
        return am != null ? am.getMode() : -1;
    }

    public static boolean isMusicActive() {
        AudioManager am = manager();
        return am != null && am.isMusicActive();
    }

    @SuppressWarnings("deprecation")
    public static boolean isHeadphonesConnected() {
        AudioManager am = manager();
        if (am == null)
            return false;

        if (Build.VERSION.SDK_INT < 23)
            return am.isWiredHeadsetOn() || am.isBluetoothA2dpOn();

        for (AudioDeviceInfo device : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                || type == AudioDeviceInfo.TYPE_USB_HEADSET
                || (Build.VERSION.SDK_INT >= 31 && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)))
                return true;
        }
        return false;
    }

    public static String getOutputDevices() {
        JSONArray array = new JSONArray();
        AudioManager am = manager();
        if (am == null || Build.VERSION.SDK_INT < 23)
            return array.toString();

        try {
            for (AudioDeviceInfo device : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                JSONObject json = new JSONObject();
                json.put("id", device.getId());
                json.put("type", device.getType());
                json.put("name", String.valueOf(device.getProductName()));
                array.put(json);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return array.toString();
    }

    public static int getOutputSampleRate() {
        AudioManager am = manager();
        if (am == null)
            return -1;

        try {
            return Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
        } catch (Exception e) {
            return -1;
        }
    }

    public static int getOutputFramesPerBuffer() {
        AudioManager am = manager();
        if (am == null)
            return -1;

        try {
            return Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
        } catch (Exception e) {
            return -1;
        }
    }

    @SuppressWarnings("deprecation")
    public static int requestAudioFocus(HaxeObject listener, int stream, int durationHint) {
        AudioManager am = manager();
        if (am == null)
            return 0;

        focusListener = listener;

        if (Build.VERSION.SDK_INT >= 26) {
            int usage = stream == AudioManager.STREAM_ALARM ? AudioAttributes.USAGE_ALARM
                : stream == AudioManager.STREAM_NOTIFICATION ? AudioAttributes.USAGE_NOTIFICATION
                : AudioAttributes.USAGE_GAME;

            AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

            focusRequest = new AudioFocusRequest.Builder(durationHint)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusChange)
                .build();

            return am.requestAudioFocus(focusRequest);
        }

        return am.requestAudioFocus(focusChange, stream, durationHint);
    }

    @SuppressWarnings("deprecation")
    public static int abandonAudioFocus() {
        AudioManager am = manager();
        if (am == null)
            return 0;

        int result;
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null)
            result = am.abandonAudioFocusRequest(focusRequest);
        else
            result = am.abandonAudioFocus(focusChange);

        focusRequest = null;
        focusListener = null;
        return result;
    }
}
