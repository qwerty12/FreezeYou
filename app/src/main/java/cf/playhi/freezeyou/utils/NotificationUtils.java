package cf.playhi.freezeyou.utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import net.grandcentrix.tray.AppPreferences;

import cf.playhi.freezeyou.FUFService;
import cf.playhi.freezeyou.Freeze;
import cf.playhi.freezeyou.R;
import cf.playhi.freezeyou.receiver.NotificationDeletedReceiver;

import static cf.playhi.freezeyou.utils.ApplicationLabelUtils.getApplicationLabel;
import static cf.playhi.freezeyou.utils.ToastUtils.showToast;

public final class NotificationUtils {

    public static void createFUFQuickNotification(Context context, String pkgName, int iconResId, Bitmap bitmap) {

        AppPreferences preferenceManager = new AppPreferences(context);
        boolean notificationBarFreezeImmediately = preferenceManager.getBoolean("notificationBarFreezeImmediately", true);
        String description = notificationBarFreezeImmediately ? context.getString(R.string.freezeImmediately) : context.getString(R.string.disableAEnable);
        Notification.Builder mBuilder = new Notification.Builder(context);
        int mId = pkgName.hashCode();
        String name = getApplicationLabel(context, null, null, pkgName);
        if (!context.getString(R.string.uninstalled).equals(name)) {
            mBuilder.setSmallIcon(iconResId);
            mBuilder.setLargeIcon(bitmap);
            mBuilder.setContentTitle(name);
            mBuilder.setContentText(description);
            mBuilder.setAutoCancel(!preferenceManager.getBoolean("notificationBarDisableClickDisappear", true));
            mBuilder.setOngoing(preferenceManager.getBoolean("notificationBarDisableSlideOut", false));

            Intent intent = new Intent(context, NotificationDeletedReceiver.class).putExtra("pkgName", pkgName);
            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(context, mId, intent,
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                    : PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setDeleteIntent(pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = "FAUf";
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, description, importance);
                channel.setDescription(description);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                if (notificationManager != null)
                    notificationManager.createNotificationChannel(channel);
                mBuilder.setChannelId(CHANNEL_ID);
            }
            // Create an Intent for the activity you want to start
            Intent resultIntent;
            PendingIntent resultPendingIntent;
            if (notificationBarFreezeImmediately) {
                resultIntent = new Intent(context, FUFService.class)
                        .putExtra("pkgName", pkgName)
                        .putExtra("single", true)
                        .putExtra("freeze", true);
                resultPendingIntent =
                        PendingIntent.getService(
                                context, mId, resultIntent,
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                        : PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                resultIntent = new Intent(context, Freeze.class).putExtra("pkgName", pkgName).putExtra("auto", false);
                resultPendingIntent =
                        PendingIntent.getActivity(
                                context, mId, resultIntent,
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                        : PendingIntent.FLAG_UPDATE_CURRENT);
            }
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.notify(mId, mBuilder.build());
                AppPreferences appPreferences = new AppPreferences(context);
                String notifying = appPreferences.getString("notifying", "");
                if (notifying != null && !notifying.contains(pkgName + ",")) {
                    appPreferences.put("notifying", notifying + pkgName + ",");
                }
            }
        }
    }

    public static void deleteNotification(Context context, String pkgName) {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(pkgName.hashCode());
            deleteNotifying(context, pkgName);
        }
    }

    private static boolean deleteNotifying(Context context, String pkgName) {
        AppPreferences defaultSharedPreferences = new AppPreferences(context);
        String notifying = defaultSharedPreferences.getString("notifying", "");
        return notifying == null || !notifying.contains(pkgName + ",") || defaultSharedPreferences.put("notifying", notifying.replace(pkgName + ",", ""));
    }

    public static void startAppNotificationSettingsSystemActivity(Activity activity, String pkgName, int pkgUid) {
        final Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", pkgName);
            intent.putExtra("app_uid", pkgUid);
            intent.putExtra("android.provider.extra.APP_PACKAGE", pkgName);
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.parse("package:" + pkgName);
            intent.setData(uri);
        }
        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            showToast(activity, e.getLocalizedMessage());
        }
    }

}
