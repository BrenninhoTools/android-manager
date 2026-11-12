package androidmanager;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

public class Clipboard {

    private static ClipboardManager manager() {
        Context ctx = Utils.context();
        return ctx != null ? (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE) : null;
    }

    public static void setText(final String text, final String label) {
        Utils.runOnUi(() -> {
            ClipboardManager cm = manager();
            if (cm != null)
                cm.setPrimaryClip(ClipData.newPlainText(label == null ? "" : label, text));
        });
    }

    public static String getText() {
        return Utils.callOnUi(() -> {
            ClipboardManager cm = manager();
            if (cm == null || !cm.hasPrimaryClip())
                return "";

            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0)
                return "";

            CharSequence text = clip.getItemAt(0).coerceToText(Utils.context());
            return text != null ? text.toString() : "";
        }, "");
    }

    public static boolean hasText() {
        Boolean result = Utils.callOnUi(() -> {
            ClipboardManager cm = manager();
            if (cm == null || !cm.hasPrimaryClip())
                return false;

            ClipDescription description = cm.getPrimaryClipDescription();
            return description != null
                && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML));
        }, false);
        return result != null && result;
    }

    public static void clear() {
        Utils.runOnUi(() -> {
            ClipboardManager cm = manager();
            if (cm == null)
                return;

            if (Build.VERSION.SDK_INT >= 28)
                cm.clearPrimaryClip();
            else
                cm.setPrimaryClip(ClipData.newPlainText("", ""));
        });
    }
}
