package androidmanager;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;
import org.json.JSONObject;

public class Screenshot {

    private static MediaActionSound shutter = null;
    private static HaxeObject detectionListener = null;
    private static Object detectionCallback = null;
    private static boolean busy = false;

    private static void respond(HaxeObject callback, boolean success, String data) {
        if (callback == null)
            return;

        try {
            callback.call("onResult", new Object[]{ success, data });
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    private static String errorJson(String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("error", message);
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static boolean isSupported() {
        View view = Extension.mainView;
        return Build.VERSION.SDK_INT >= 24 && view instanceof SurfaceView;
    }

    public static boolean isBusy() {
        return busy;
    }

    public static void playShutterSound() {
        try {
            if (shutter == null) {
                shutter = new MediaActionSound();
                shutter.load(MediaActionSound.SHUTTER_CLICK);
            }
            shutter.play(MediaActionSound.SHUTTER_CLICK);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void capture(final String path, final String optionsJson, final HaxeObject callback) {
        final View view = Extension.mainView;
        if (!isSupported() || view.getWidth() <= 0 || view.getHeight() <= 0) {
            respond(callback, false, errorJson("Screenshot is not supported on this device"));
            return;
        }

        if (busy) {
            respond(callback, false, errorJson("A screenshot is already being captured"));
            return;
        }

        JSONObject parsed;
        try {
            parsed = new JSONObject(optionsJson == null || optionsJson.isEmpty() ? "{}" : optionsJson);
        } catch (Exception e) {
            parsed = new JSONObject();
        }
        final JSONObject options = parsed;

        busy = true;
        Utils.runOnUi(() -> {
            final Bitmap bitmap;
            try {
                bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            } catch (Throwable e) {
                busy = false;
                respond(callback, false, errorJson(e.toString()));
                return;
            }

            final HandlerThread thread = new HandlerThread("AndroidManager-Screenshot");
            thread.start();

            if (options.optBoolean("sound", false))
                playShutterSound();

            try {
                PixelCopy.request((SurfaceView) view, bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                    @Override
                    public void onPixelCopyFinished(int result) {
                        try {
                            if (result != PixelCopy.SUCCESS) {
                                bitmap.recycle();
                                respond(callback, false, errorJson("PixelCopy failed with code " + result));
                                return;
                            }

                            respond(callback, true, process(bitmap, path, options));
                        } catch (Throwable e) {
                            respond(callback, false, errorJson(e.toString()));
                        } finally {
                            busy = false;
                            thread.quitSafely();
                        }
                    }
                }, new Handler(thread.getLooper()));
            } catch (Throwable e) {
                busy = false;
                thread.quitSafely();
                bitmap.recycle();
                respond(callback, false, errorJson(e.toString()));
            }
        });
    }

    private static String process(Bitmap source, String path, JSONObject options) throws Exception {
        Bitmap bitmap = source;

        int x = options.optInt("x", 0);
        int y = options.optInt("y", 0);
        int width = options.optInt("width", 0);
        int height = options.optInt("height", 0);
        if (width > 0 && height > 0) {
            Rect rect = new Rect(Math.max(0, x), Math.max(0, y), Math.min(bitmap.getWidth(), x + width), Math.min(bitmap.getHeight(), y + height));
            if (rect.width() > 0 && rect.height() > 0) {
                Bitmap cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
                if (cropped != bitmap) {
                    bitmap.recycle();
                    bitmap = cropped;
                }
            }
        }

        double scale = options.optDouble("scale", 1.0);
        int maxSize = options.optInt("maxSize", 0);
        if (maxSize > 0) {
            int largest = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (largest > maxSize)
                scale = Math.min(scale, maxSize / (double) largest);
        }

        if (scale > 0 && Math.abs(scale - 1.0) > 0.001) {
            int w = Math.max(1, (int) Math.round(bitmap.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(bitmap.getHeight() * scale));
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, w, h, true);
            if (scaled != bitmap) {
                bitmap.recycle();
                bitmap = scaled;
            }
        }

        String format = options.optString("format", "").toLowerCase(Locale.ROOT);
        if (format.isEmpty() && path != null) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
                format = "jpg";
            else if (lower.endsWith(".webp"))
                format = "webp";
        }
        if (format.isEmpty())
            format = "png";

        int quality = Math.max(0, Math.min(100, options.optInt("quality", 95)));
        Bitmap.CompressFormat compress = compressFormat(format, quality);
        String mime = format.equals("jpg") || format.equals("jpeg") ? "image/jpeg" : format.equals("webp") ? "image/webp" : "image/png";

        JSONObject result = new JSONObject();
        result.put("width", bitmap.getWidth());
        result.put("height", bitmap.getHeight());
        result.put("format", format);
        result.put("path", JSONObject.NULL);
        result.put("uri", JSONObject.NULL);
        result.put("base64", JSONObject.NULL);

        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();

            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(compress, quality, out);
                out.flush();
            }
            result.put("path", file.getAbsolutePath());
        }

        if (options.optBoolean("gallery", false)) {
            String uri = saveToGallery(bitmap, compress, quality, mime, format, options.optString("album", ""), options.optString("fileName", ""));
            if (uri != null)
                result.put("uri", uri);
        }

        if (options.optBoolean("base64", false)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(compress, quality, bytes);
            result.put("base64", Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP));
        }

        bitmap.recycle();
        return result.toString();
    }

    @SuppressWarnings("deprecation")
    private static Bitmap.CompressFormat compressFormat(String format, int quality) {
        if (format.equals("jpg") || format.equals("jpeg"))
            return Bitmap.CompressFormat.JPEG;

        if (format.equals("webp")) {
            if (Build.VERSION.SDK_INT >= 30)
                return quality >= 100 ? Bitmap.CompressFormat.WEBP_LOSSLESS : Bitmap.CompressFormat.WEBP_LOSSY;
            return Bitmap.CompressFormat.WEBP;
        }

        return Bitmap.CompressFormat.PNG;
    }

    private static String appLabel(Context ctx) {
        try {
            return String.valueOf(ctx.getPackageManager().getApplicationLabel(ctx.getApplicationInfo())).replaceAll("[\\\\/:*?\"<>|]", "_");
        } catch (Exception e) {
            return "Screenshots";
        }
    }

    @SuppressWarnings("deprecation")
    private static String saveToGallery(Bitmap bitmap, Bitmap.CompressFormat compress, int quality, String mime, String format, String album, String fileName) throws Exception {
        Context ctx = Utils.context();
        if (ctx == null)
            return null;

        String folder = album == null || album.isEmpty() ? appLabel(ctx) : album;
        String extension = format.equals("jpeg") ? "jpg" : format;
        String name = fileName == null || fileName.isEmpty()
            ? "Screenshot_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date())
            : fileName;
        if (!name.toLowerCase(Locale.ROOT).endsWith("." + extension))
            name = name + "." + extension;

        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = ctx.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, mime);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + folder);
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null)
                return null;

            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null)
                    return null;
                bitmap.compress(compress, quality, out);
            }

            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
            return uri.toString();
        }

        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folder);
        if (!directory.exists() && !directory.mkdirs())
            return null;

        File file = new File(directory, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(compress, quality, out);
        }

        MediaScannerConnection.scanFile(ctx, new String[]{ file.getAbsolutePath() }, new String[]{ mime }, null);
        return Uri.fromFile(file).toString();
    }

    public static boolean isDetectionSupported() {
        return Build.VERSION.SDK_INT >= 34 && PermissionManager.hasPermission("android.permission.DETECT_SCREEN_CAPTURE");
    }

    public static boolean startDetection(final HaxeObject listener) {
        detectionListener = listener;
        final Activity act = Utils.activity();
        if (act == null || detectionCallback != null || !isDetectionSupported())
            return detectionCallback != null;

        try {
            Class<?> callbackClass = Class.forName("android.app.Activity$ScreenCaptureCallback");
            Object callback = java.lang.reflect.Proxy.newProxyInstance(callbackClass.getClassLoader(), new Class<?>[]{ callbackClass },
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        String name = method.getName();
                        if (name.equals("onScreenCaptured")) {
                            HaxeObject target = detectionListener;
                            if (target != null) {
                                try {
                                    target.call("onScreenCaptured", new Object[0]);
                                } catch (Exception e) {
                                    Log.e(Utils.LOG_TAG, e.toString());
                                }
                            }
                            return null;
                        }

                        if (name.equals("hashCode"))
                            return System.identityHashCode(proxy);
                        if (name.equals("equals"))
                            return args != null && args.length > 0 && proxy == args[0];
                        if (name.equals("toString"))
                            return "AndroidManagerScreenCaptureCallback";
                        return null;
                    }
                });

            Activity.class.getMethod("registerScreenCaptureCallback", java.util.concurrent.Executor.class, callbackClass)
                .invoke(act, act.getMainExecutor(), callback);
            detectionCallback = callback;
            return true;
        } catch (Throwable e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static void stopDetection() {
        Activity act = Utils.activity();
        if (act != null && detectionCallback != null && Build.VERSION.SDK_INT >= 34) {
            try {
                Class<?> callbackClass = Class.forName("android.app.Activity$ScreenCaptureCallback");
                Activity.class.getMethod("unregisterScreenCaptureCallback", callbackClass).invoke(act, detectionCallback);
            } catch (Throwable e) {
                Log.e(Utils.LOG_TAG, e.toString());
            }
        }

        detectionCallback = null;
        detectionListener = null;
    }
}
