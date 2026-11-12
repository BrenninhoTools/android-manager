package java.androidmanager;

import android.app.ActivityManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import java.io.File;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

public class Device {

    private static final String[] SU_PATHS = {
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/xbin/daemonsu",
        "/system/etc/init.d/99SuperSUDaemon",
        "/data/adb/magisk",
        "/sbin/.magisk",
        "/cache/.disable_magisk",
        "/dev/.magisk.unblock",
        "/data/adb/ksu",
        "/data/adb/ksud"
    };

    private static final String[] ROOT_PACKAGES = {
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.noshufou.android.su",
        "me.weishu.kernelsu",
        "io.github.huskydg.magisk",
        "io.github.vvb2060.magisk"
    };

    private static PackageManager pm() {
        Context ctx = Utils.context();
        return ctx != null ? ctx.getPackageManager() : null;
    }

    public static boolean hasSystemFeature(String feature) {
        PackageManager pm = pm();
        return pm != null && feature != null && pm.hasSystemFeature(feature);
    }

    public static boolean isRooted() {
        if (Build.TAGS != null && Build.TAGS.contains("test-keys"))
            return true;

        for (String path : SU_PATHS) {
            if (new File(path).exists())
                return true;
        }

        PackageManager pm = pm();
        if (pm != null) {
            for (String pkg : ROOT_PACKAGES) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    return true;
                } catch (Exception ignored) {
                }
            }
        }

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{ "/system/xbin/which", "su" });
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            return reader.readLine() != null;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null)
                process.destroy();
        }
    }

    public static boolean isEmulator() {
        String fingerprint = Build.FINGERPRINT != null ? Build.FINGERPRINT : "";
        String model = Build.MODEL != null ? Build.MODEL : "";
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
        String brand = Build.BRAND != null ? Build.BRAND : "";
        String device = Build.DEVICE != null ? Build.DEVICE : "";
        String product = Build.PRODUCT != null ? Build.PRODUCT : "";
        String hardware = Build.HARDWARE != null ? Build.HARDWARE : "";

        return fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || fingerprint.contains("emulator")
            || model.contains("google_sdk")
            || model.contains("Emulator")
            || model.contains("Android SDK built for")
            || manufacturer.contains("Genymotion")
            || (brand.startsWith("generic") && device.startsWith("generic"))
            || product.contains("sdk_gphone")
            || product.equals("google_sdk")
            || product.contains("vbox86p")
            || product.contains("emulator")
            || product.contains("simulator")
            || hardware.contains("goldfish")
            || hardware.contains("ranchu")
            || hardware.contains("vbox86")
            || hasSystemFeature("com.google.android.play.feature.HPE_EXPERIENCE");
    }

    public static boolean isTablet() {
        Context ctx = Utils.context();
        if (ctx == null)
            return false;

        Configuration config = ctx.getResources().getConfiguration();
        return (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
            || config.smallestScreenWidthDp >= 600;
    }

    public static boolean isAndroidTV() {
        Context ctx = Utils.context();
        if (ctx == null)
            return false;

        UiModeManager ui = (UiModeManager) ctx.getSystemService(Context.UI_MODE_SERVICE);
        if (ui != null && ui.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
            return true;

        return hasSystemFeature("android.software.leanback") || hasSystemFeature("android.hardware.type.television");
    }

    public static boolean isWatch() {
        return hasSystemFeature("android.hardware.type.watch");
    }

    public static boolean isAutomotive() {
        return hasSystemFeature("android.hardware.type.automotive");
    }

    public static boolean isChromebook() {
        return hasSystemFeature("org.chromium.arc") || hasSystemFeature("org.chromium.arc.device_management");
    }

    public static boolean isDeXMode() {
        Context ctx = Utils.context();
        if (ctx == null)
            return false;

        try {
            Configuration config = ctx.getResources().getConfiguration();
            Class<?> configClass = config.getClass();
            int enabled = configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass);
            int current = configClass.getField("semDesktopModeEnabled").getInt(config);
            return enabled == current;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDolbyAtmos() {
        if (Build.VERSION.SDK_INT < 21)
            return false;

        try {
            MediaFormat eac3 = new MediaFormat();
            eac3.setString(MediaFormat.KEY_MIME, "audio/eac3-joc");

            MediaFormat ac4 = new MediaFormat();
            ac4.setString(MediaFormat.KEY_MIME, "audio/ac4");

            MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
            return list.findDecoderForFormat(eac3) != null || list.findDecoderForFormat(ac4) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDarkMode() {
        Context ctx = Utils.context();
        if (ctx == null)
            return false;

        int mode = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isLowRamDevice() {
        Context ctx = Utils.context();
        if (ctx == null)
            return false;

        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        return am != null && am.isLowRamDevice();
    }

    public static boolean isInMultiWindowMode() {
        if (Build.VERSION.SDK_INT < 24 || Utils.activity() == null)
            return false;

        return Utils.activity().isInMultiWindowMode();
    }

    public static boolean isInPictureInPictureMode() {
        if (Build.VERSION.SDK_INT < 24 || Utils.activity() == null)
            return false;

        return Utils.activity().isInPictureInPictureMode();
    }

    public static int getThermalStatus() {
        Context ctx = Utils.context();
        if (Build.VERSION.SDK_INT < 29 || ctx == null)
            return -1;

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        return pm != null ? pm.getCurrentThermalStatus() : -1;
    }

    public static int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static String getSupportedAbis() {
        JSONArray array = new JSONArray();
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null) {
            for (String abi : Build.SUPPORTED_ABIS)
                array.put(abi);
        }
        return array.toString();
    }

    public static String getGlEsVersion() {
        Context ctx = Utils.context();
        if (ctx == null)
            return "";

        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        return am != null ? am.getDeviceConfigurationInfo().getGlEsVersion() : "";
    }

    public static String getLocale() {
        return Locale.getDefault().toLanguageTag();
    }

    public static String getTimeZone() {
        return TimeZone.getDefault().getID();
    }

    public static double getUptime() {
        return (double) SystemClock.elapsedRealtime();
    }

    public static String getDeviceName() {
        Context ctx = Utils.context();
        if (ctx != null && Build.VERSION.SDK_INT >= 25) {
            try {
                String name = Settings.Global.getString(ctx.getContentResolver(), Settings.Global.DEVICE_NAME);
                if (name != null && !name.isEmpty())
                    return name;
            } catch (Exception ignored) {
            }
        }

        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
        String model = Build.MODEL != null ? Build.MODEL : "";
        if (model.toLowerCase(Locale.ROOT).startsWith(manufacturer.toLowerCase(Locale.ROOT)))
            return model;

        return manufacturer + " " + model;
    }

    public static String getMemoryInfo() {
        JSONObject json = new JSONObject();
        try {
            Context ctx = Utils.context();
            if (ctx != null) {
                ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(info);
                json.put("total", (double) info.totalMem);
                json.put("available", (double) info.availMem);
                json.put("threshold", (double) info.threshold);
                json.put("lowMemory", info.lowMemory);
                json.put("memoryClass", am.getMemoryClass());
                json.put("largeMemoryClass", am.getLargeMemoryClass());
            }

            Runtime runtime = Runtime.getRuntime();
            json.put("appMax", (double) runtime.maxMemory());
            json.put("appTotal", (double) runtime.totalMemory());
            json.put("appFree", (double) runtime.freeMemory());
            json.put("appUsed", (double) (runtime.totalMemory() - runtime.freeMemory()));
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return json.toString();
    }

    public static String getStorageInfo(String path) {
        JSONObject json = new JSONObject();
        try {
            String target = path;
            if (target == null || target.isEmpty())
                target = android.os.Environment.getDataDirectory().getAbsolutePath();

            StatFs stat = new StatFs(target);
            json.put("path", target);
            json.put("total", (double) stat.getTotalBytes());
            json.put("free", (double) stat.getFreeBytes());
            json.put("available", (double) stat.getAvailableBytes());
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return json.toString();
    }

    @SuppressWarnings("deprecation")
    public static String getDisplayInfo() {
        JSONObject json = new JSONObject();
        try {
            if (Utils.activity() == null)
                return json.toString();

            Display display = Utils.activity().getWindowManager().getDefaultDisplay();
            DisplayMetrics real = new DisplayMetrics();
            display.getRealMetrics(real);

            DisplayMetrics app = new DisplayMetrics();
            display.getMetrics(app);

            json.put("width", real.widthPixels);
            json.put("height", real.heightPixels);
            json.put("appWidth", app.widthPixels);
            json.put("appHeight", app.heightPixels);
            json.put("density", (double) real.density);
            json.put("densityDpi", real.densityDpi);
            json.put("scaledDensity", (double) real.scaledDensity);
            json.put("xdpi", (double) real.xdpi);
            json.put("ydpi", (double) real.ydpi);
            json.put("refreshRate", (double) display.getRefreshRate());
            json.put("rotation", display.getRotation());

            JSONArray rates = new JSONArray();
            if (Build.VERSION.SDK_INT >= 23) {
                for (Display.Mode mode : display.getSupportedModes()) {
                    double rate = Math.round(mode.getRefreshRate() * 100.0) / 100.0;
                    boolean exists = false;
                    for (int i = 0; i < rates.length(); i++) {
                        if (rates.getDouble(i) == rate)
                            exists = true;
                    }
                    if (!exists)
                        rates.put(rate);
                }
            }
            json.put("supportedRefreshRates", rates);

            JSONObject cutout = new JSONObject();
            cutout.put("top", 0);
            cutout.put("bottom", 0);
            cutout.put("left", 0);
            cutout.put("right", 0);

            if (Build.VERSION.SDK_INT >= 28 && Utils.activity().getWindow() != null) {
                WindowInsets insets = Utils.activity().getWindow().getDecorView().getRootWindowInsets();
                if (insets != null) {
                    DisplayCutout dc = insets.getDisplayCutout();
                    if (dc != null) {
                        cutout.put("top", dc.getSafeInsetTop());
                        cutout.put("bottom", dc.getSafeInsetBottom());
                        cutout.put("left", dc.getSafeInsetLeft());
                        cutout.put("right", dc.getSafeInsetRight());
                    }
                }
            }
            json.put("cutout", cutout);

            boolean hdr = false;
            if (Build.VERSION.SDK_INT >= 26)
                hdr = display.isHdr();
            json.put("hdr", hdr);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return json.toString();
    }
}
