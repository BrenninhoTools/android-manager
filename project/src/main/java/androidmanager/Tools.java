package androidmanager;

public final class Tools {

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private Tools() {}

    public static boolean isRooted() {
        return Device.isRooted();
    }

    public static boolean isEmulator() {
        return Device.isEmulator();
    }

    public static boolean isTablet() {
        return Device.isTablet();
    }

    public static boolean isAndroidTV() {
        return Device.isAndroidTV();
    }

    public static boolean isChromebook() {
        return Device.isChromebook();
    }

    public static boolean isDeXMode() {
        return Device.isDeXMode();
    }

    public static boolean isDolbyAtmos() {
        return Device.isDolbyAtmos();
    }

    public static void minimizeWindow() {
        WindowManager.minimize();
    }

    public static boolean launchPackage(String packageName) {
        return AppManager.launchPackage(packageName, -1);
    }

    public static boolean openURL(String url) {
        return AppManager.openURL(url);
    }

    public static void makeToast(String text, int duration) {
        Interface.makeToast(text, duration, -1, 0, 0);
    }

    public static void makeToast(String text, int duration, int gravity, int xOffset, int yOffset) {
        Interface.makeToast(text, duration, gravity, xOffset, yOffset);
    }
}
