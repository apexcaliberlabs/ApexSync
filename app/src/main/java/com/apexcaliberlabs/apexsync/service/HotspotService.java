package com.apexcaliberlabs.apexsync.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.apexcaliberlabs.apexsync.R;
import com.apexcaliberlabs.apexsync.manager.HotspotManager;
import com.apexcaliberlabs.apexsync.model.HotspotConfig;
import com.apexcaliberlabs.apexsync.ui.MainActivity;

/**
 * Foreground service that manages the local-only Wi-Fi hotspot lifecycle.
 *
 * <p>Start the service with {@link #ACTION_START} and stop it with {@link #ACTION_STOP}.
 * The service broadcasts {@link #ACTION_HOTSPOT_STARTED} and {@link #ACTION_HOTSPOT_STOPPED}
 * so that UI components can react to state changes.
 */
public class HotspotService extends Service {

    // -------------------------------------------------------------------------
    // Public API – Intent actions
    // -------------------------------------------------------------------------

    /** Start action: sent by UI to begin the hotspot. */
    public static final String ACTION_START = "com.apexcaliberlabs.apexsync.hotspot.START";

    /** Stop action: sent by UI or the notification's stop button. */
    public static final String ACTION_STOP = "com.apexcaliberlabs.apexsync.hotspot.STOP";

    /** Broadcast emitted when the hotspot starts successfully. */
    public static final String ACTION_HOTSPOT_STARTED =
            "com.apexcaliberlabs.apexsync.hotspot.STARTED";

    /** Broadcast emitted when the hotspot stops. */
    public static final String ACTION_HOTSPOT_STOPPED =
            "com.apexcaliberlabs.apexsync.hotspot.STOPPED";

    /** Broadcast emitted when the hotspot fails to start. */
    public static final String ACTION_HOTSPOT_FAILED =
            "com.apexcaliberlabs.apexsync.hotspot.FAILED";

    /** Extra key: SSID string attached to {@link #ACTION_HOTSPOT_STARTED}. */
    public static final String EXTRA_SSID = "ssid";

    /** Extra key: passphrase string attached to {@link #ACTION_HOTSPOT_STARTED}. */
    public static final String EXTRA_PASSPHRASE = "passphrase";

    /** Extra key: error reason int attached to {@link #ACTION_HOTSPOT_FAILED}. */
    public static final String EXTRA_ERROR_REASON = "error_reason";

    // -------------------------------------------------------------------------
    // Notification constants
    // -------------------------------------------------------------------------

    private static final String CHANNEL_ID = "hotspot_channel";
    private static final int NOTIFICATION_ID = 101;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private HotspotManager hotspotManager;

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        hotspotManager = new HotspotManager(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        switch (intent.getAction() != null ? intent.getAction() : "") {
            case ACTION_START:
                startHotspotAndForeground();
                break;
            case ACTION_STOP:
                stopHotspotAndService();
                break;
            default:
                break;
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        return null; // Not a bound service.
    }

    @Override
    public void onDestroy() {
        hotspotManager.stopHotspot();
        super.onDestroy();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void startHotspotAndForeground() {
        // Post an initial "starting" notification so we can run in the foreground
        // before the hotspot callback fires.
        startForeground(NOTIFICATION_ID, buildNotification(
                getString(R.string.hotspot_starting), "", ""));

        hotspotManager.startHotspot(new HotspotManager.HotspotCallback() {
            @Override
            public void onStarted(@NonNull HotspotConfig config) {
                // Update notification with real credentials.
                updateNotification(config.getSsid(), config.getPassphrase());

                Intent broadcast = new Intent(ACTION_HOTSPOT_STARTED);
                broadcast.putExtra(EXTRA_SSID, config.getSsid());
                broadcast.putExtra(EXTRA_PASSPHRASE, config.getPassphrase());
                sendBroadcast(broadcast);
            }

            @Override
            public void onStopped() {
                sendBroadcast(new Intent(ACTION_HOTSPOT_STOPPED));
                stopSelf();
            }

            @Override
            public void onFailed(int reason) {
                Intent broadcast = new Intent(ACTION_HOTSPOT_FAILED);
                broadcast.putExtra(EXTRA_ERROR_REASON, reason);
                sendBroadcast(broadcast);
                stopSelf();
            }
        });
    }

    private void stopHotspotAndService() {
        hotspotManager.stopHotspot();
        sendBroadcast(new Intent(ACTION_HOTSPOT_STOPPED));
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.hotspot_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.hotspot_notification_channel_desc));
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private void updateNotification(String ssid, String passphrase) {
        Notification notification = buildNotification(
                getString(R.string.hotspot_active), ssid, passphrase);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFICATION_ID, notification);
    }

    @NonNull
    private Notification buildNotification(String title, String ssid, String passphrase) {
        // Tapping the notification opens MainActivity.
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent openAppPi = PendingIntent.getActivity(
                this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Stop action in the notification.
        Intent stopIntent = new Intent(this, HotspotService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String contentText = ssid.isEmpty() ? "" : ssid + " · " + passphrase;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wifi_tethering)
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(openAppPi)
                .addAction(R.drawable.ic_stop, getString(R.string.action_stop), stopPi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
