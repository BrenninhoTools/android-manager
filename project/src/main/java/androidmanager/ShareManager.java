package androidmanager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;

public class ShareManager {

    private static HaxeObject listener = null;
    private static BroadcastReceiver chosenReceiver = null;

    private static String chosenAction() {
        return Utils.packageName() + ".androidmanager.SHARE_CHOSEN";
    }

    public static void setListener(HaxeObject object) {
        listener = object;
    }

    private static void ensureReceiver() {
        Context ctx = Utils.context();
        if (ctx == null || chosenReceiver != null || Build.VERSION.SDK_INT < 22)
            return;

        chosenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ComponentName component = intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT);
                HaxeObject target = listener;
                if (component == null || target == null)
                    return;

                try {
                    target.call("onChosen", new Object[]{ component.getPackageName(), intent.getIntExtra("androidmanager.request", -1) });
                } catch (Exception e) {
                    Log.e(Utils.LOG_TAG, e.toString());
                }
            }
        };

        try {
            IntentFilter filter = new IntentFilter(chosenAction());
            if (Build.VERSION.SDK_INT >= 33)
                ctx.registerReceiver(chosenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else
                ctx.registerReceiver(chosenReceiver, filter);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            chosenReceiver = null;
        }
    }

    public static String getUriForFile(String path) {
        Uri uri = toUri(path);
        return uri != null ? uri.toString() : null;
    }

    private static Uri toUri(String path) {
        if (path == null || path.isEmpty())
            return null;

        if (path.startsWith("content://") || path.startsWith("http://") || path.startsWith("https://"))
            return Uri.parse(path);

        String filePath = path.startsWith("file://") ? Uri.parse(path).getPath() : path;
        if (filePath == null || !new File(filePath).exists())
            return null;

        return ShareProvider.getUriForFile(Utils.context(), filePath);
    }

    private static String commonMime(ArrayList<Uri> uris, String requested) {
        if (requested != null && !requested.isEmpty())
            return requested;

        String result = null;
        for (Uri uri : uris) {
            String mime = Utils.context().getContentResolver().getType(uri);
            if (mime == null)
                mime = ShareProvider.mimeFor(uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "");

            if (result == null) {
                result = mime;
            } else if (!result.equals(mime)) {
                String a = result.contains("/") ? result.substring(0, result.indexOf('/')) : result;
                String b = mime.contains("/") ? mime.substring(0, mime.indexOf('/')) : mime;
                result = a.equals(b) ? a + "/*" : "*/*";
            }
        }
        return result != null ? result : "*/*";
    }

    public static String share(String pathsJson, String text, String subject, String mimeType, String title, String targetPackage, int requestCode) {
        final Activity act = Utils.activity();
        if (act == null)
            return "No activity";

        ArrayList<Uri> uris = new ArrayList<>();
        try {
            JSONArray paths = new JSONArray(pathsJson == null || pathsJson.isEmpty() ? "[]" : pathsJson);
            for (int i = 0; i < paths.length(); i++) {
                Uri uri = toUri(paths.getString(i));
                if (uri == null)
                    return "File not found or not shareable: " + paths.getString(i);
                uris.add(uri);
            }
        } catch (Exception e) {
            return e.toString();
        }

        boolean hasText = text != null && !text.isEmpty();
        if (uris.isEmpty() && !hasText)
            return "Nothing to share";

        final Intent intent;
        if (uris.size() > 1) {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            intent = new Intent(Intent.ACTION_SEND);
            if (!uris.isEmpty())
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }

        intent.setType(uris.isEmpty() ? (mimeType == null || mimeType.isEmpty() ? "text/plain" : mimeType) : commonMime(uris, mimeType));

        if (hasText)
            intent.putExtra(Intent.EXTRA_TEXT, text);
        if (subject != null && !subject.isEmpty())
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        if (!uris.isEmpty()) {
            ClipData clip = ClipData.newRawUri("", uris.get(0));
            for (int i = 1; i < uris.size(); i++)
                clip.addItem(new ClipData.Item(uris.get(i)));
            intent.setClipData(clip);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        Intent launch = null;
        if (targetPackage != null && !targetPackage.isEmpty()) {
            Intent direct = new Intent(intent);
            direct.setPackage(targetPackage);
            if (direct.resolveActivity(act.getPackageManager()) != null)
                launch = direct;
        }

        if (launch == null) {
            String chooserTitle = title == null || title.isEmpty() ? null : title;
            if (Build.VERSION.SDK_INT >= 22) {
                ensureReceiver();
                Intent chosen = new Intent(chosenAction());
                chosen.setPackage(Utils.packageName());
                chosen.putExtra("androidmanager.request", requestCode);

                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 31)
                    flags |= PendingIntent.FLAG_MUTABLE;

                PendingIntent pending = PendingIntent.getBroadcast(act, requestCode, chosen, flags);
                launch = Intent.createChooser(intent, chooserTitle, pending.getIntentSender());
            } else {
                launch = Intent.createChooser(intent, chooserTitle);
            }
        } else if (listener != null) {
            try {
                listener.call("onChosen", new Object[]{ targetPackage, requestCode });
            } catch (Exception e) {
                Log.e(Utils.LOG_TAG, e.toString());
            }
        }

        if (!uris.isEmpty())
            launch.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return Utils.startForResult(launch, requestCode) ? "" : "Could not start share";
    }
}
