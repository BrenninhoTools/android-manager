package androidmanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.os.Handler;
import android.os.Looper;
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

    private static HaxeObject eventListener = null;
    private static BroadcastReceiver receiver = null;
    private static Object deviceCallback = null;
    private static Object modeListener = null;
    private static final java.util.HashMap<Integer, Integer> lastVolumes = new java.util.HashMap<>();

    private static void emit(String method, Object... args) {
        HaxeObject target = eventListener;
        if (target == null)
            return;

        try {
            target.call(method, args);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static String getDeviceTypeName(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE: return "earpiece";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: return "speaker";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET: return "wired_headset";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES: return "wired_headphones";
            case AudioDeviceInfo.TYPE_LINE_ANALOG: return "line_analog";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL: return "line_digital";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return "bluetooth_sco";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: return "bluetooth_a2dp";
            case AudioDeviceInfo.TYPE_HDMI: return "hdmi";
            case AudioDeviceInfo.TYPE_HDMI_ARC: return "hdmi_arc";
            case AudioDeviceInfo.TYPE_USB_DEVICE: return "usb_device";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY: return "usb_accessory";
            case AudioDeviceInfo.TYPE_DOCK: return "dock";
            case AudioDeviceInfo.TYPE_TELEPHONY: return "telephony";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC: return "builtin_mic";
            case AudioDeviceInfo.TYPE_USB_HEADSET: return "usb_headset";
            case 23: return "hearing_aid";
            case 24: return "builtin_speaker_safe";
            case 26: return "ble_headset";
            case 27: return "ble_speaker";
            case 29: return "hdmi_earc";
            case 30: return "ble_broadcast";
            default: return "unknown";
        }
    }

    private static boolean isHeadphoneType(int type) {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            || type == AudioDeviceInfo.TYPE_USB_HEADSET
            || type == 23
            || type == 26
            || type == 27;
    }

    private static boolean isBluetoothType(int type) {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            || type == 23
            || type == 26
            || type == 27
            || type == 30;
    }

    private static JSONArray intArray(int[] values) {
        JSONArray array = new JSONArray();
        if (values != null) {
            for (int value : values)
                array.put(value);
        }
        return array;
    }

    private static JSONObject deviceToJson(AudioDeviceInfo device) throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", device.getId());
        json.put("type", device.getType());
        json.put("typeName", getDeviceTypeName(device.getType()));
        json.put("name", String.valueOf(device.getProductName()));
        json.put("sink", device.isSink());
        json.put("source", device.isSource());
        json.put("headphones", isHeadphoneType(device.getType()));
        json.put("bluetooth", isBluetoothType(device.getType()));
        json.put("address", Build.VERSION.SDK_INT >= 28 ? device.getAddress() : "");
        json.put("sampleRates", intArray(device.getSampleRates()));
        json.put("channelCounts", intArray(device.getChannelCounts()));
        json.put("encodings", intArray(device.getEncodings()));
        return json;
    }

    public static String getDevices(boolean outputs) {
        JSONArray array = new JSONArray();
        AudioManager am = manager();
        if (am == null || Build.VERSION.SDK_INT < 23)
            return array.toString();

        try {
            for (AudioDeviceInfo device : am.getDevices(outputs ? AudioManager.GET_DEVICES_OUTPUTS : AudioManager.GET_DEVICES_INPUTS))
                array.put(deviceToJson(device));
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return array.toString();
    }

    @SuppressWarnings("deprecation")
    public static boolean isBluetoothConnected() {
        AudioManager am = manager();
        if (am == null)
            return false;

        if (Build.VERSION.SDK_INT < 23)
            return am.isBluetoothA2dpOn() || am.isBluetoothScoOn();

        for (AudioDeviceInfo device : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (isBluetoothType(device.getType()))
                return true;
        }
        return false;
    }

    public static boolean isCallActive() {
        int mode = getMode();
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE;
    }

    public static boolean isMicrophoneMute() {
        AudioManager am = manager();
        return am != null && am.isMicrophoneMute();
    }

    public static int getInterruptionFilter() {
        Context ctx = Utils.context();
        if (ctx == null || Build.VERSION.SDK_INT < 23)
            return 1;

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null ? nm.getCurrentInterruptionFilter() : 1;
    }

    public static boolean hasLowLatency() {
        Context ctx = Utils.context();
        return ctx != null && ctx.getPackageManager().hasSystemFeature("android.hardware.audio.low_latency");
    }

    public static boolean hasProAudio() {
        Context ctx = Utils.context();
        return ctx != null && Build.VERSION.SDK_INT >= 23 && ctx.getPackageManager().hasSystemFeature("android.hardware.audio.pro");
    }

    public static int getOutputLatency(int stream) {
        AudioManager am = manager();
        if (am == null)
            return -1;

        try {
            Object value = AudioManager.class.getMethod("getOutputLatency", int.class).invoke(am, stream);
            return value instanceof Integer ? (Integer) value : -1;
        } catch (Throwable e) {
            return -1;
        }
    }

    public static boolean isSpatializerAvailable() {
        AudioManager am = manager();
        return am != null && Build.VERSION.SDK_INT >= 32 && am.getSpatializer().isAvailable();
    }

    public static boolean isSpatializerEnabled() {
        AudioManager am = manager();
        return am != null && Build.VERSION.SDK_INT >= 32 && am.getSpatializer().isEnabled();
    }

    public static boolean isHeadTrackerAvailable() {
        AudioManager am = manager();
        return am != null && Build.VERSION.SDK_INT >= 33 && am.getSpatializer().isHeadTrackerAvailable();
    }

    public static void setStreamMute(int stream, boolean mute, int flags) {
        AudioManager am = manager();
        if (am == null)
            return;

        try {
            if (Build.VERSION.SDK_INT >= 23)
                am.adjustStreamVolume(stream, mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, flags);
            else
                am.setStreamMute(stream, mute);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void setVolumeControlStream(final int stream) {
        Utils.runOnUi(() -> Utils.activity().setVolumeControlStream(stream));
    }

    public static int getVolumeControlStream() {
        return Utils.activity() != null ? Utils.activity().getVolumeControlStream() : -1;
    }

    public static void playSoundEffect(int effect, float volume) {
        AudioManager am = manager();
        if (am == null)
            return;

        if (volume < 0)
            am.playSoundEffect(effect);
        else
            am.playSoundEffect(effect, volume);
    }

    public static String getInfo() {
        JSONObject json = new JSONObject();
        try {
            json.put("sampleRate", getOutputSampleRate());
            json.put("framesPerBuffer", getOutputFramesPerBuffer());
            json.put("outputLatency", getOutputLatency(AudioManager.STREAM_MUSIC));
            json.put("lowLatency", hasLowLatency());
            json.put("proAudio", hasProAudio());
            json.put("musicActive", isMusicActive());
            json.put("mode", getMode());
            json.put("ringerMode", getRingerMode());
            json.put("callActive", isCallActive());
            json.put("headphones", isHeadphonesConnected());
            json.put("bluetooth", isBluetoothConnected());
            json.put("microphoneMute", isMicrophoneMute());
            json.put("interruptionFilter", getInterruptionFilter());
            json.put("spatializerAvailable", isSpatializerAvailable());
            json.put("spatializerEnabled", isSpatializerEnabled());
            json.put("headTrackerAvailable", isHeadTrackerAvailable());
            json.put("volumeControlStream", getVolumeControlStream());

            JSONObject volumes = new JSONObject();
            int[] streams = { AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING, AudioManager.STREAM_ALARM,
                AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_VOICE_CALL };
            for (int stream : streams) {
                JSONObject entry = new JSONObject();
                entry.put("volume", getStreamVolume(stream));
                entry.put("max", getStreamMaxVolume(stream));
                entry.put("min", getStreamMinVolume(stream));
                entry.put("muted", isStreamMute(stream));
                volumes.put(String.valueOf(stream), entry);
            }
            json.put("volumes", volumes);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return json.toString();
    }

    public static void startListening(HaxeObject listener) {
        eventListener = listener;
        final Context ctx = Utils.context();
        final AudioManager am = manager();
        if (ctx == null || am == null || receiver != null)
            return;

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null)
                    return;

                if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                    int stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                    if (stream < 0)
                        return;

                    int volume = getStreamVolume(stream);
                    Integer previous = lastVolumes.get(stream);
                    if (previous != null && previous == volume)
                        return;

                    lastVolumes.put(stream, volume);
                    emit("onVolumeChanged", stream, volume, getStreamMaxVolume(stream));
                } else if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    emit("onBecomingNoisy");
                } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                    emit("onRingerModeChanged", intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, getRingerMode()));
                } else if (action.equals(Intent.ACTION_HEADSET_PLUG) && Build.VERSION.SDK_INT < 23) {
                    emit("onHeadphonesChanged", intent.getIntExtra("state", 0) == 1);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);

        try {
            if (Build.VERSION.SDK_INT >= 33)
                ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            else
                ctx.registerReceiver(receiver, filter);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            receiver = null;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            AudioDeviceCallback callback = new AudioDeviceCallback() {
                private boolean initial = true;

                @Override
                public void onAudioDevicesAdded(AudioDeviceInfo[] devices) {
                    if (initial) {
                        initial = false;
                        return;
                    }
                    report(devices, true);
                }

                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) {
                    report(devices, false);
                }

                private void report(AudioDeviceInfo[] devices, boolean added) {
                    boolean headphones = false;
                    for (AudioDeviceInfo device : devices) {
                        if (!device.isSink())
                            continue;

                        try {
                            emit("onDeviceChanged", added, deviceToJson(device).toString());
                        } catch (Exception e) {
                            Log.e(Utils.LOG_TAG, e.toString());
                        }

                        if (isHeadphoneType(device.getType()))
                            headphones = true;
                    }

                    if (headphones)
                        emit("onHeadphonesChanged", isHeadphonesConnected());
                }
            };
            am.registerAudioDeviceCallback(callback, new Handler(Looper.getMainLooper()));
            deviceCallback = callback;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            AudioManager.OnModeChangedListener listenerMode = new AudioManager.OnModeChangedListener() {
                @Override
                public void onModeChanged(int mode) {
                    emit("onModeChanged", mode);
                }
            };
            am.addOnModeChangedListener(ctx.getMainExecutor(), listenerMode);
            modeListener = listenerMode;
        }
    }

    public static void stopListening() {
        Context ctx = Utils.context();
        AudioManager am = manager();

        try {
            if (receiver != null && ctx != null)
                ctx.unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }

        if (am != null && deviceCallback != null && Build.VERSION.SDK_INT >= 23)
            am.unregisterAudioDeviceCallback((AudioDeviceCallback) deviceCallback);

        if (am != null && modeListener != null && Build.VERSION.SDK_INT >= 31)
            am.removeOnModeChangedListener((AudioManager.OnModeChangedListener) modeListener);

        receiver = null;
        deviceCallback = null;
        modeListener = null;
        eventListener = null;
        lastVolumes.clear();
    }
}
