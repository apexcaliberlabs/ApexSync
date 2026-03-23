package com.apexcaliberlabs.apexsync.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexcaliberlabs.apexsync.model.RepeaterConfig;

/**
 * Manages the lifecycle of a Wi-Fi Direct (P2P) group that acts as a soft-AP.
 *
 * <p>The device creates a Wi-Fi Direct Group where it is the Group Owner (GO).
 * Other devices can connect to the GO using the group credentials.
 *
 * <p><b>Limitation:</b> Wi-Fi Direct groups do NOT automatically forward internet
 * traffic from the upstream Wi-Fi interface. This class exposes only the group
 * creation / teardown flow; internet bridging is out of scope for this manager.
 *
 * <p>Usage:
 * <pre>
 *   RepeaterManager manager = new RepeaterManager(context);
 *   manager.startRepeater(new RepeaterManager.RepeaterCallback() {
 *       {@literal @}Override public void onStarted(RepeaterConfig config) { ... }
 *       {@literal @}Override public void onStopped() { ... }
 *       {@literal @}Override public void onFailed(String reason) { ... }
 *   });
 * </pre>
 */
public class RepeaterManager {

    private final Context appContext;
    private final WifiP2pManager p2pManager;
    private final WifiP2pManager.Channel channel;

    @Nullable
    private BroadcastReceiver p2pReceiver;
    private boolean groupActive = false;

    public RepeaterManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        p2pManager = (WifiP2pManager) appContext.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager != null
                ? p2pManager.initialize(appContext, Looper.getMainLooper(), null)
                : null;
    }

    /**
     * Creates a Wi-Fi Direct group (the device becomes the Group Owner) and notifies
     * the caller via {@code callback} when the group is established or fails.
     *
     * @param callback listener for group lifecycle events
     */
    public void startRepeater(@NonNull RepeaterCallback callback) {
        if (p2pManager == null || channel == null) {
            callback.onFailed("Wi-Fi Direct is not supported on this device.");
            return;
        }

        p2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Group creation initiated – request group info to get SSID/passphrase.
                requestGroupInfo(callback);
            }

            @Override
            public void onFailure(int reason) {
                callback.onFailed("Group creation failed (reason=" + reason + ")");
            }
        });
    }

    /**
     * Removes the active Wi-Fi Direct group and notifies the caller via {@code callback}.
     *
     * @param callback listener; {@link RepeaterCallback#onStopped()} is called on success
     */
    public void stopRepeater(@NonNull RepeaterCallback callback) {
        if (p2pManager == null || channel == null || !groupActive) {
            groupActive = false;
            callback.onStopped();
            return;
        }
        p2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                groupActive = false;
                callback.onStopped();
            }

            @Override
            public void onFailure(int reason) {
                // Best-effort: treat the group as gone regardless.
                groupActive = false;
                callback.onStopped();
            }
        });
    }

    /**
     * Registers a P2P broadcast receiver so that the manager can detect when the
     * group is torn down by the OS.
     *
     * <p>Call this from your service/activity's {@code onResume()} (or equivalent).
     *
     * @param stoppedCallback called if the OS removes the group unexpectedly
     */
    public void registerReceiver(@NonNull Runnable stoppedCallback) {
        if (p2pReceiver != null) return;
        p2pReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    if (groupActive) {
                        // Re-check group info; if null the group was removed.
                        if (p2pManager != null && channel != null) {
                            p2pManager.requestGroupInfo(channel, group -> {
                                if (group == null) {
                                    groupActive = false;
                                    stoppedCallback.run();
                                }
                            });
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        appContext.registerReceiver(p2pReceiver, filter);
    }

    /** Unregisters the P2P broadcast receiver. Call from service/activity's {@code onPause()}. */
    public void unregisterReceiver() {
        if (p2pReceiver != null) {
            try {
                appContext.unregisterReceiver(p2pReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            p2pReceiver = null;
        }
    }

    /** Returns {@code true} if a P2P group is currently active. */
    public boolean isRepeaterActive() {
        return groupActive;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void requestGroupInfo(@NonNull RepeaterCallback callback) {
        p2pManager.requestGroupInfo(channel, group -> {
            if (group == null) {
                callback.onFailed("Could not retrieve group information.");
                return;
            }
            groupActive = true;
            String ownerAddress = group.getOwner() != null
                    ? group.getOwner().deviceAddress : "";
            RepeaterConfig config = new RepeaterConfig(
                    group.getNetworkName(),
                    group.getPassphrase(),
                    ownerAddress
            );
            callback.onStarted(config);
        });
    }

    // -------------------------------------------------------------------------
    // Callback interface
    // -------------------------------------------------------------------------

    /**
     * Listener for repeater (Wi-Fi Direct group) lifecycle events.
     */
    public interface RepeaterCallback {
        /**
         * Called when the group has been created and group info is available.
         *
         * @param config the network name and passphrase for the group
         */
        void onStarted(@NonNull RepeaterConfig config);

        /** Called when the group has been removed successfully. */
        void onStopped();

        /**
         * Called when group creation or teardown failed.
         *
         * @param reason a human-readable description of the failure
         */
        void onFailed(@NonNull String reason);
    }
}
