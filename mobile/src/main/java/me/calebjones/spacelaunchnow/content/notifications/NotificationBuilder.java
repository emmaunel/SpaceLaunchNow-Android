package me.calebjones.spacelaunchnow.content.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.data.models.Constants;
import me.calebjones.spacelaunchnow.data.models.launchlibrary.Launch;
import me.calebjones.spacelaunchnow.ui.launchdetail.activity.LaunchDetailActivity;
import me.calebjones.spacelaunchnow.utils.Utils;
import me.calebjones.spacelaunchnow.utils.analytics.Analytics;
import timber.log.Timber;

import static me.calebjones.spacelaunchnow.content.notifications.NotificationHelper.CHANNEL_LAUNCH_IMMINENT;
import static me.calebjones.spacelaunchnow.content.notifications.NotificationHelper.CHANNEL_LAUNCH_UPDATE;

public class NotificationBuilder {
    public static void notifyUser(Context context, Launch launch, long timeToFinish, boolean update) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        NotificationCompat.Builder mBuilder;
        NotificationManager mNotifyManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            mBuilder = new NotificationCompat.Builder(context, CHANNEL_LAUNCH_IMMINENT);
            mNotifyManager = notificationHelper.getManager();
        } else {
            mBuilder = new NotificationCompat.Builder(context);
            mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        String launchDate;
        String expandedText;
        String launchName = launch.getName();
        String launchPad = launch.getLocation().getName();

        String ringtoneBox = sharedPref.getString("notifications_new_message_ringtone", "default ringtone");
        Uri alarmSound = Uri.parse(ringtoneBox);

        Intent resultIntent = new Intent(context, LaunchDetailActivity.class);
        resultIntent.putExtra("TYPE", "launch");
        resultIntent.putExtra("launchID", launch.getId());

        PendingIntent pending = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (launch.getNet() != null) {
            SimpleDateFormat sdf;
            if (sharedPref.getBoolean("24_hour_mode", false)) {
                sdf = Utils.getSimpleDateFormatForUI("k:mm a zzz");
            } else {
                sdf = Utils.getSimpleDateFormatForUI("h:mm a zzz");
            }
            //Get launch date
            if (sharedPref.getBoolean("local_time", true)) {
                Date date = launch.getNet();
                launchDate = sdf.format(date);
            } else {
                Date date = launch.getNet();
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                launchDate = sdf.format(date);
            }
            expandedText = getContentText(timeToFinish, launchDate, update);
        } else {
            expandedText = getContentText(timeToFinish, update);
        }
        mBuilder.setSubText(launchPad);

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
                        .setHintHideIcon(true);
        if (launch.getRocket().getImageURL() != null && launch.getRocket().getImageURL().length() > 0 && !launch.getRocket().getImageURL().contains("placeholder")) {
            Bitmap bitmap = null;
            try {
                bitmap = Utils.getBitMapFromUrl(context, launch.getRocket().getImageURL());
            } catch (ExecutionException | InterruptedException e) {
                Timber.e(e);
                Crashlytics.logException(e);
            }
            if (bitmap != null) {
                wearableExtender.setBackground(bitmap);
            }
        }

        if (update){
            mBuilder.setContentTitle("UPDATE: " + launchName);
        } else {
            mBuilder.setContentTitle(launchName);
        }

        mBuilder.setContentText(expandedText)
                .setSmallIcon(R.drawable.ic_rocket)
                .setAutoCancel(true)
                .setContentText(expandedText)
                .extend(wearableExtender)
                .setContentIntent(pending);

        if (update){
            mBuilder.setChannelId(CHANNEL_LAUNCH_UPDATE);
        } else {
            mBuilder.setChannelId(CHANNEL_LAUNCH_IMMINENT).setSound(alarmSound);
        }



        if (launch.getRocket().getImageURL() != null && launch.getRocket().getImageURL().length() > 0 && !launch.getRocket().getImageURL().contains("placeholder")) {
            Bitmap bitmap = null;
            try {
                bitmap = Utils.getBitMapFromUrl(context, launch.getRocket().getImageURL());
            } catch (ExecutionException | InterruptedException e) {
                Timber.e(e);
                Crashlytics.logException(e);
            }
            if (bitmap != null){
                mBuilder.setLargeIcon(bitmap);
            }
        }

        if (update){
            mBuilder.setPriority(Notification.PRIORITY_LOW);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                    sharedPref.getBoolean("notifications_new_message_heads_up", true)) {
                mBuilder.setPriority(Notification.PRIORITY_HIGH);
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN &&
                sharedPref.getBoolean("notifications_new_message_vibrate", true)) {
            mBuilder.setVibrate(new long[]{750, 750});
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                && sharedPref.getBoolean("notifications_new_message_led", true)) {
            mBuilder.setLights(Color.GREEN, 3000, 3000);
        }

        Analytics.from(context).sendNotificationEvent(launch.getName(), expandedText);
        mNotifyManager.notify(Constants.NOTIF_ID_HOUR + launch.getId(), mBuilder.build());
    }

    private static String getContentText(long timeToFinish, boolean update) {
        String header;
        if (update){
            header = "Now launching";
        } else {
            header = "Launch attempt";
        }
        if (timeToFinish < 3600000){
            int minutes = (int) ((timeToFinish / (1000 * 60)) % 60);
            if (minutes == 9) {
                return header + " in ten minutes.";
            } else if (minutes == 59){
                return header + " in one hour.";
            } else {
                return header + " in " + minutes + " minutes.";
            }
        } else if (timeToFinish < 8.64e+7) {
            int hours = (int) ((timeToFinish / (1000 * 60 * 60)) % 24);
            if (hours == 23) {
                return header + " in twenty-four hours.";
            } else {
                return header + " in " + hours + " hours.";
            }
        } else {
            int days = (int) (timeToFinish / (1000 * 60 * 60 * 24));
            int hours = (int) ((timeToFinish / (1000 * 60 * 60)) % 24);
            if (hours > 0){
                if (days == 1) {
                    return header + " tomorrow.";
                } else {
                    return header + " in " + days + " day(s).";
                }
            } else if (days == 1) {
                return header + " in one day " + hours + "hours.";
            } else {
                return header + " in " + days + " day(s) " + hours + "hours.";
            }
        }
    }

    private static String getContentText(long timeToFinish, String launchDate, boolean update) {
        String header;
        if (update){
            header = "Now launching";
        } else {
            header = "Launch attempt";
        }
        if (timeToFinish < 3600000){
            int minutes = (int) ((timeToFinish / (1000 * 60)) % 60);
            if (minutes == 9) {
                return header + " in ten minutes at " + launchDate;
            } else if (minutes == 59){
                return header + " in one hour at " + launchDate;
            } else {
                return header + " in " + minutes + " minutes at " + launchDate;
            }
        } else if (timeToFinish < 8.64e+7) {
            int hours = (int) ((timeToFinish / (1000 * 60 * 60)) % 24);
            if (hours == 23) {
                return header + " in twenty-four hours at " + launchDate;
            } else {
                return header + " in " + hours + " hours at " + launchDate;
            }
        } else {
            int days = (int) (timeToFinish / (1000 * 60 * 60 * 24));
            int hours = (int) ((timeToFinish / (1000 * 60 * 60)) % 24);
            if (hours > 0){
                if (days == 1) {
                    return header + " tomorrow at " + launchDate;
                } else {
                    return header + " in " + days + " day(s) at " + launchDate;
                }
            } else if (days == 1) {
                return header + " in one day " + hours + "hours at " + launchDate;
            } else {
                return header + " in " + days + " day(s) " + hours + "hours at " + launchDate;
            }
        }
    }
}