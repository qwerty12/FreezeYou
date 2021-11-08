package cf.playhi.freezeyou;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

import static cf.playhi.freezeyou.utils.ToastUtils.showToast;

import androidx.core.app.NotificationCompat;

import com.catchingnow.delegatedscopesmanager.centerApp.CenterApp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private static final String TAG = "DeviceAdminReceiver";

    private static final String LOGS_DIR = "logs";

    private static final String FAILED_PASSWORD_LOG_FILE =
            "failed_pw_attempts_timestamps.log";

    private static final int PASSWORD_FAILED_NOTIFICATION_ID = 102;
    private static final String PASSWORD_FAILED_NOTIFICATION_CHANNEL = "DeviceAdminWrongPasswordNotifications";

    @Override
    public void onEnabled(Context context, Intent intent) {
//        showToast(context, R.string.activated);
        CenterApp.getInstance(context).refreshState();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.disableConfirmation);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, R.string.disabled);
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
        if (!Process.myUserHandle().equals(user)) {
            // This password failure was on another user, for example a parent profile. Ignore it.
            return;
        }
        DevicePolicyManager devicePolicyManager = DevicePolicyManagerUtils.getDevicePolicyManager(context);
        /*
         * Post a notification to show:
         *  - how many wrong passwords have been entered
         */
        int attempts = devicePolicyManager.getCurrentFailedPasswordAttempts();

        String title = context.getResources().getQuantityString(
                R.plurals.password_failed_attempts_title, attempts, attempts);

        ArrayList<Date> previousFailedAttempts = getFailedPasswordAttempts(context);
        Date date = new Date();
        previousFailedAttempts.add(date);
        Collections.sort(previousFailedAttempts, Collections.<Date>reverseOrder());
        try {
            saveFailedPasswordAttempts(context, previousFailedAttempts);
        } catch (IOException e) {
            Log.e(TAG, "Unable to save failed password attempts", e);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel =
                new NotificationChannel(
                        PASSWORD_FAILED_NOTIFICATION_CHANNEL,
                        "Wrong unlock password entered",
                        NotificationManager.IMPORTANCE_LOW
                );
        notificationManager.createNotificationChannel(channel);
        NotificationCompat.Builder warn = new NotificationCompat.Builder(context, PASSWORD_FAILED_NOTIFICATION_CHANNEL);
        warn.setSmallIcon(R.drawable.ic_notification)
                .setTicker(title)
                .setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, /* requestCode */ -1,
                        new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD), /* flags */ 0));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);

        final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();
        for(Date d : previousFailedAttempts) {
            inboxStyle.addLine(dateFormat.format(d));
        }
        warn.setStyle(inboxStyle);

        notificationManager.notify(PASSWORD_FAILED_NOTIFICATION_ID, warn.build());
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent, UserHandle user) {
        if (Process.myUserHandle().equals(user)) {
            logFile(context).delete();
        }
    }

    private static File logFile(Context context) {
        File parent = context.getDir(LOGS_DIR, Context.MODE_PRIVATE);
        return new File(parent, FAILED_PASSWORD_LOG_FILE);
    }

    private static ArrayList<Date> getFailedPasswordAttempts(Context context) {
        File logFile = logFile(context);
        ArrayList<Date> result = new ArrayList<Date>();

        if(!logFile.exists()) {
            return result;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(logFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line = null;
            while ((line = br.readLine()) != null && line.length() > 0) {
                result.add(new Date(Long.parseLong(line)));
            }

            br.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to read failed password attempts", e);
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to close failed password attempts log file", e);
                }
            }
        }

        return result;
    }

    private static void saveFailedPasswordAttempts(Context context, ArrayList<Date> attempts)
            throws IOException {
        File logFile = logFile(context);

        if(!logFile.exists()) {
            logFile.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(logFile);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for(Date date : attempts) {
            bw.write(Long.toString(date.getTime()));
            bw.newLine();
        }

        bw.close();
    }

}
