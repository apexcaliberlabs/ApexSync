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
import com.apexcaliberlabs.apexsync.manager.RepeaterManager;
import com.apexcaliberlabs.apexsync.model.RepeaterConfig;
import com.apexcaliberlabs.apexsync.ui.MainActivity;

/**
 * Foreground service that manages the Wi-Fi Direct repeater group lifecycle.
 *
 * <p>Start the service with {@link #ACTION_START} and stop it with {@link #ACTION_STOP}.
 * The service broadcasts {@link #ACTION_REPEATER_STARTED} and {@link #ACTION_REPEATER_STOPPED}
 * so that UI components can react to state changes.
 */
public class RepeaterService extends Service {

    // -------------------------------------------------------------------------
    // Public API – Intent actions
    // -------------------------------------------------------------------------

    /** Start action: sent by UI to create the P2P group. */
    public static final String ACTION_START = "com.apexcaliberlabs.apexsync.repeater.START";

    /** Stop action: sent by UI or the notification's stop button. */
    public static final String ACTION_STOP = "com.apexcaliberlabs.apexsync.repeater.STOP";

    /** Broadcast emitted when the repeater group starts successfully. */
    public static final String ACTION_REPEATER_STARTED =
            "com.apexcaliberlabs.apexsync.repeater.STARTED";

    /** Broadcast emitted when the repeater group stops. */
    public static final String ACTION_REPEATER_STOPPED =
            "com.apexcaliberlabs.apexsync.repeater.STOPPED";

    /** Broadcast emitted when the repeater group fails to start. */
    public static final String ACTION_REPEATER_FAILED =
            "com.apexcaliberlabs.apexsync.repeater.FAILED";

    /** Extra key: network name string attached to {@link #ACTION_REPEATER_STARTED}. */
    public static final String EXTRA_NETWORK_NAME = "network_name";

    /** Extra key: passphrase string attached to {@link #ACTION_REPEATER_STARTED}. */
    public static final String EXTRA_PASSPHRASE = "passphrase";

    /** Extra key: error message string attached to {@link #ACTION_REPEATER_FAILED}. */
    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    // -------------------------------------------------------------------------
    // Notification constants
    // -------------------------------------------------------------------------

    private static final String CHANNEL_ID = "repeater_channel";
    private static final int NOTIFICATION_ID = 102;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private RepeaterManager repeaterManager;

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        repeaterManager = new RepeaterManager(this);
        createNotificationChannel();
        // Register the P2P receiver so we detect OS-initiated group teardown.
        repeaterManager.registerReceiver(() -> {
            sendBroadcast(new Intent(ACTION_REPEATER_STOPPED));
            stopForeground(true);
            stopSelf();
        });
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        switch (intent.getAction() != null ? intent.getAction() : "") {
            case ACTION_START:
                startRepeaterAndForeground();
                break;
            case ACTION_STOP:
                stopRepeaterAndService();
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
        repeaterManager.unregisterReceiver();
        repeaterManager.stopRepeater(new RepeaterManager.RepeaterCallback() {
            @Override public void onStarted(@NonNull RepeaterConfig config) { }
            @Override public void onStopped() { }
            @Override public void onFailed(@NonNull String reason) { }
        });
        super.onDestroy();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void startRepeaterAndForeground() {
        startForeground(NOTIFICATION_ID, buildNotification(
                getString(R.string.repeater_starting), ""));

        repeaterManager.startRepeater(new RepeaterManager.RepeaterCallback() {
            @Override
            public void onStarted(@NonNull RepeaterConfig config) {
                updateNotification(config.getNetworkName());

                Intent broadcast = new Intent(ACTION_REPEATER_STARTED);
                broadcast.putExtra(EXTRA_NETWORK_NAME, config.getNetworkName());
                broadcast.putExtra(EXTRA_PASSPHRASE, config.getPassphrase());
                sendBroadcast(broadcast);
            }

            @Override
            public void onStopped() {
                sendBroadcast(new Intent(ACTION_REPEATER_STOPPED));
                stopSelf();
            }

            @Override
            public void onFailed(@NonNull String reason) {
                Intent broadcast = new Intent(ACTION_REPEATER_FAILED);
                broadcast.putExtra(EXTRA_ERROR_MESSAGE, reason);
                sendBroadcast(broadcast);
                stopSelf();
            }
        });
    }

    private void stopRepeaterAndService() {
        repeaterManager.stopRepeater(new RepeaterManager.RepeaterCallback() {
            @Override public void onStarted(@NonNull RepeaterConfig config) { }

            @Override
            public void onStopped() {
                sendBroadcast(new Intent(ACTION_REPEATER_STOPPED));
                stopForeground(true);
                stopSelf();
            }

            @Override public void onFailed(@NonNull String reason) {
                stopForeground(true);
                stopSelf();
            }
        });
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.repeater_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.repeater_notification_channel_desc));
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private void updateNotification(String networkName) {
        Notification notification = buildNotification(
                getString(R.string.repeater_active), networkName);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFICATION_ID, notification);
    }

    @NonNull
    private Notification buildNotification(String title, String networkName) {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent openAppPi = PendingIntent.getActivity(
                this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, RepeaterService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_router)
                .setContentTitle(title)
                .setContentText(networkName)
                .setContentIntent(openAppPi)
                .addAction(R.drawable.ic_stop, getString(R.string.action_stop), stopPi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
