package java.androidmanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class Notifications {

    private static NotificationManager manager() {
        Context ctx = Utils.context();
        return ctx != null ? (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE) : null;
    }

    public static boolean areEnabled() {
        NotificationManager nm = manager();
        if (nm == null)
            return false;

        if (Build.VERSION.SDK_INT >= 24)
            return nm.areNotificationsEnabled();

        return true;
    }

    public static void createChannel(String id, String name, String description, int importance) {
        if (Build.VERSION.SDK_INT < 26)
            return;

        NotificationManager nm = manager();
        if (nm == null)
            return;

        NotificationChannel channel = new NotificationChannel(id, name, importance);
        if (description != null && !description.isEmpty())
            channel.setDescription(description);

        nm.createNotificationChannel(channel);
    }

    public static void deleteChannel(String id) {
        if (Build.VERSION.SDK_INT < 26)
            return;

        NotificationManager nm = manager();
        if (nm != null)
            nm.deleteNotificationChannel(id);
    }

    private static int smallIcon(Context ctx) {
        int icon = ctx.getResources().getIdentifier("icon", "drawable", ctx.getPackageName());
        if (icon == 0)
            icon = ctx.getApplicationInfo().icon;
        if (icon == 0)
            icon = android.R.drawable.ic_dialog_info;
        return icon;
    }

    @SuppressWarnings("deprecation")
    public static void show(final int id, final String title, final String message, final String channelId,
            final String channelName, final int priority, final boolean autoCancel, final boolean ongoing,
            final int progressMax, final int progress, final boolean indeterminate, final boolean silent) {
        final Context ctx = Utils.context();
        final NotificationManager nm = manager();
        if (ctx == null || nm == null)
            return;

        try {
            String channel = (channelId == null || channelId.isEmpty()) ? "android_manager" : channelId;

            if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(channel) == null) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                if (priority > 0)
                    importance = NotificationManager.IMPORTANCE_HIGH;
                else if (priority < 0)
                    importance = NotificationManager.IMPORTANCE_LOW;

                String label = (channelName == null || channelName.isEmpty()) ? channel : channelName;
                nm.createNotificationChannel(new NotificationChannel(channel, label, importance));
            }

            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(ctx, channel)
                : new Notification.Builder(ctx);

            builder.setSmallIcon(smallIcon(ctx));
            builder.setContentTitle(title);
            builder.setContentText(message);
            builder.setStyle(new Notification.BigTextStyle().bigText(message));
            builder.setAutoCancel(autoCancel);
            builder.setOngoing(ongoing);
            builder.setWhen(System.currentTimeMillis());
            builder.setShowWhen(true);
            builder.setPriority(Math.max(-2, Math.min(2, priority)));

            if (silent && Build.VERSION.SDK_INT >= 26)
                builder.setOnlyAlertOnce(true);

            if (progressMax > 0 || indeterminate)
                builder.setProgress(progressMax, progress, indeterminate);

            Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 23)
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                builder.setContentIntent(PendingIntent.getActivity(ctx, id, launch, flags));
            }

            nm.notify(id, builder.build());
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void cancel(int id) {
        NotificationManager nm = manager();
        if (nm != null)
            nm.cancel(id);
    }

    public static void cancelAll() {
        NotificationManager nm = manager();
        if (nm != null)
            nm.cancelAll();
    }
}
