package androidmanager;

import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public final class BuildInfo {

    private BuildInfo() {}

    private static String safe(String value) {
        return value != null ? value : "Unknown";
    }

    public static String getModel() {
        return safe(Build.MODEL);
    }

    public static String getManufacturer() {
        return safe(Build.MANUFACTURER);
    }

    public static String getDevice() {
        return safe(Build.DEVICE);
    }

    public static String getBoard() {
        return safe(Build.BOARD);
    }

    public static String getBrand() {
        return safe(Build.BRAND);
    }

    public static String getHardware() {
        return safe(Build.HARDWARE);
    }

    public static String getDisplay() {
        return safe(Build.DISPLAY);
    }

    public static String getFingerprint() {
        return safe(Build.FINGERPRINT);
    }

    public static String getProduct() {
        return safe(Build.PRODUCT);
    }

    public static String getBootloader() {
        return safe(Build.BOOTLOADER);
    }

    public static String getId() {
        return safe(Build.ID);
    }

    public static String getHost() {
        return safe(Build.HOST);
    }

    public static String getUser() {
        return safe(Build.USER);
    }

    public static String getTags() {
        return safe(Build.TAGS);
    }

    public static String getType() {
        return safe(Build.TYPE);
    }

    public static long getTime() {
        return Build.TIME;
    }

    public static String getSocManufacturer() {
        return Build.VERSION.SDK_INT >= 31 ? safe(Build.SOC_MANUFACTURER) : "Unknown";
    }

    public static String getSocModel() {
        return Build.VERSION.SDK_INT >= 31 ? safe(Build.SOC_MODEL) : "Unknown";
    }

    public static String getSupportedAbis() {
        JSONArray array = new JSONArray();
        if (Build.SUPPORTED_ABIS != null)
            for (String abi : Build.SUPPORTED_ABIS)
                array.put(abi);
        return array.toString();
    }

    public static boolean is64Bit() {
        return Build.SUPPORTED_64_BIT_ABIS != null && Build.SUPPORTED_64_BIT_ABIS.length > 0;
    }

    public static int getSdkInt() {
        return Build.VERSION.SDK_INT;
    }

    public static String getRelease() {
        return safe(Build.VERSION.RELEASE);
    }

    public static String getCodename() {
        return safe(Build.VERSION.CODENAME);
    }

    public static String getIncremental() {
        return safe(Build.VERSION.INCREMENTAL);
    }

    public static String getBaseOs() {
        return Build.VERSION.SDK_INT >= 23 ? safe(Build.VERSION.BASE_OS) : "Unknown";
    }

    public static String getSecurityPatch() {
        return Build.VERSION.SDK_INT >= 23 ? safe(Build.VERSION.SECURITY_PATCH) : "Unknown";
    }

    public static int getMediaPerformanceClass() {
        return Build.VERSION.SDK_INT >= 31 ? Build.VERSION.MEDIA_PERFORMANCE_CLASS : 0;
    }

    public static int getPreviewSdkInt() {
        return Build.VERSION.SDK_INT >= 23 ? Build.VERSION.PREVIEW_SDK_INT : 0;
    }

    public static boolean isAtLeast(int sdk) {
        return Build.VERSION.SDK_INT >= sdk;
    }

    public static String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("model", getModel());
            json.put("manufacturer", getManufacturer());
            json.put("device", getDevice());
            json.put("board", getBoard());
            json.put("brand", getBrand());
            json.put("hardware", getHardware());
            json.put("display", getDisplay());
            json.put("fingerprint", getFingerprint());
            json.put("product", getProduct());
            json.put("bootloader", getBootloader());
            json.put("id", getId());
            json.put("host", getHost());
            json.put("user", getUser());
            json.put("tags", getTags());
            json.put("type", getType());
            json.put("time", getTime());
            json.put("socManufacturer", getSocManufacturer());
            json.put("socModel", getSocModel());
            json.put("supportedAbis", new JSONArray(getSupportedAbis()));
            json.put("is64Bit", is64Bit());
            json.put("sdkInt", getSdkInt());
            json.put("release", getRelease());
            json.put("codename", getCodename());
            json.put("incremental", getIncremental());
            json.put("baseOs", getBaseOs());
            json.put("securityPatch", getSecurityPatch());
            json.put("mediaPerformanceClass", getMediaPerformanceClass());
            json.put("previewSdkInt", getPreviewSdkInt());
            return json.toString();
        } catch (Exception e) {
            return Utils.error(e.toString());
        }
    }
}
