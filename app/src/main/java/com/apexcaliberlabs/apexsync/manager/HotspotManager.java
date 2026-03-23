package com.apexcaliberlabs.apexsync.manager;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexcaliberlabs.apexsync.model.HotspotConfig;

/**
 * Manages the lifecycle of an Android {@link WifiManager#startLocalOnlyHotspot} session.
 *
 * <p>The local-only hotspot does <em>not</em> route internet traffic to connected clients;
 * it creates an isolated Wi-Fi network useful for direct device-to-device communication.
 * For full tethering, direct the user to the system Tethering Settings.
 *
 * <p>Usage:
 * <pre>
 *   HotspotManager manager = new HotspotManager(context);
 *   manager.startHotspot(new HotspotManager.HotspotCallback() {
 *       {@literal @}Override public void onStarted(HotspotConfig config) { ... }
 *       {@literal @}Override public void onStopped() { ... }
 *       {@literal @}Override public void onFailed(int reason) { ... }
 *   });
 * </pre>
 */
public class HotspotManager {

    /** Hotspot failed because another exclusive app is using the hotspot. */
    public static final int ERROR_TETHERING_DISALLOWED =
            WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED;

    /** Hotspot failed due to an incompatible mode already active. */
    public static final int ERROR_INCOMPATIBLE_MODE =
            WifiManager.LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE;

    /** Hotspot failed because the user has no location permission. */
    public static final int ERROR_NO_CHANNEL =
            WifiManager.LocalOnlyHotspotCallback.ERROR_NO_CHANNEL;

    private final WifiManager wifiManager;
    @Nullable
    private WifiManager.LocalOnlyHotspotReservation reservation;

    public HotspotManager(@NonNull Context context) {
        wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Starts the local-only hotspot and invokes the supplied {@link HotspotCallback} when
     * the hotspot state changes.
     *
     * @param callback the listener to receive hotspot lifecycle events
     */
    public void startHotspot(@NonNull HotspotCallback callback) {
        if (wifiManager == null) {
            callback.onFailed(-1);
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation r) {
                reservation = r;
                WifiConfiguration config = r.getWifiConfiguration();
                HotspotConfig hotspotConfig = buildHotspotConfig(config);
                callback.onStarted(hotspotConfig);
            }

            @Override
            public void onStopped() {
                reservation = null;
                callback.onStopped();
            }

            @Override
            public void onFailed(int reason) {
                reservation = null;
                callback.onFailed(reason);
            }
        }, handler);
    }

    /**
     * Stops the active local-only hotspot by closing the reservation.
     * If no hotspot is active this is a no-op.
     */
    public void stopHotspot() {
        if (reservation != null) {
            reservation.close();
            reservation = null;
        }
    }

    /** Returns {@code true} if a hotspot reservation is currently active. */
    public boolean isHotspotActive() {
        return reservation != null;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @NonNull
    private static HotspotConfig buildHotspotConfig(@Nullable WifiConfiguration wifiConfig) {
        if (wifiConfig == null) {
            return new HotspotConfig("ApexSync", "", 0);
        }
        String ssid = wifiConfig.SSID != null ? wifiConfig.SSID : "ApexSync";
        // Strip surrounding quotes that WifiConfiguration sometimes adds.
        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() > 1) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        String passphrase = wifiConfig.preSharedKey != null ? wifiConfig.preSharedKey : "";
        if (passphrase.startsWith("\"") && passphrase.endsWith("\"")
                && passphrase.length() > 1) {
            passphrase = passphrase.substring(1, passphrase.length() - 1);
        }
        int band = wifiConfig.apBand;
        return new HotspotConfig(ssid, passphrase, band);
    }

    // -------------------------------------------------------------------------
    // Callback interface
    // -------------------------------------------------------------------------

    /**
     * Listener for hotspot lifecycle events.
     */
    public interface HotspotCallback {
        /**
         * Called when the hotspot has started successfully.
         *
         * @param config the SSID and passphrase assigned by the OS
         */
        void onStarted(@NonNull HotspotConfig config);

        /** Called when the hotspot has been stopped (either by the app or by the OS). */
        void onStopped();

        /**
         * Called when the hotspot failed to start.
         *
         * @param reason one of the {@code ERROR_*} constants defined on this class
         */
        void onFailed(int reason);
    }
}
