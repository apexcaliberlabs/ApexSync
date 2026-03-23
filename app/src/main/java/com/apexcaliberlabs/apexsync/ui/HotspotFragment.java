package com.apexcaliberlabs.apexsync.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.apexcaliberlabs.apexsync.R;
import com.apexcaliberlabs.apexsync.databinding.FragmentHotspotBinding;
import com.apexcaliberlabs.apexsync.service.HotspotService;
import com.apexcaliberlabs.apexsync.util.PermissionHelper;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Fragment that exposes the local-only Wi-Fi hotspot feature to the user.
 *
 * <p>The fragment communicates with {@link HotspotService} via explicit Intents and
 * listens for status broadcasts to update its UI.
 */
public class HotspotFragment extends Fragment {

    private FragmentHotspotBinding binding;

    private boolean hotspotActive = false;
    private String currentSsid = "";
    private String currentPassphrase = "";

    // -------------------------------------------------------------------------
    // Broadcast receiver
    // -------------------------------------------------------------------------

    private final BroadcastReceiver hotspotReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case HotspotService.ACTION_HOTSPOT_STARTED:
                    currentSsid = intent.getStringExtra(HotspotService.EXTRA_SSID);
                    currentPassphrase = intent.getStringExtra(HotspotService.EXTRA_PASSPHRASE);
                    onHotspotStarted(currentSsid, currentPassphrase);
                    break;
                case HotspotService.ACTION_HOTSPOT_STOPPED:
                    onHotspotStopped();
                    break;
                case HotspotService.ACTION_HOTSPOT_FAILED:
                    int reason = intent.getIntExtra(HotspotService.EXTRA_ERROR_REASON, -1);
                    onHotspotFailed(reason);
                    break;
                default:
                    break;
            }
        }
    };

    // -------------------------------------------------------------------------
    // Fragment lifecycle
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHotspotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.switchHotspot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestPermissionsAndStart();
            } else {
                stopHotspot();
            }
        });

        binding.buttonOpenTethering.setOnClickListener(v -> openSystemTethering());

        binding.buttonTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(HotspotService.ACTION_HOTSPOT_STARTED);
        filter.addAction(HotspotService.ACTION_HOTSPOT_STOPPED);
        filter.addAction(HotspotService.ACTION_HOTSPOT_FAILED);
        requireContext().registerReceiver(hotspotReceiver, filter,
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(hotspotReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // -------------------------------------------------------------------------
    // Permission handling
    // -------------------------------------------------------------------------

    private void requestPermissionsAndStart() {
        String[] permissions = PermissionHelper.getHotspotPermissions();
        if (PermissionHelper.hasPermissions(requireContext(), permissions)) {
            startHotspot();
        } else {
            PermissionHelper.requestPermissions(
                    this, permissions, PermissionHelper.REQUEST_CODE_HOTSPOT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_HOTSPOT) {
            if (PermissionHelper.allGranted(grantResults)) {
                startHotspot();
            } else {
                binding.switchHotspot.setChecked(false);
                Toast.makeText(requireContext(),
                        R.string.permission_required_hotspot, Toast.LENGTH_LONG).show();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Service interaction
    // -------------------------------------------------------------------------

    private void startHotspot() {
        Intent intent = new Intent(requireContext(), HotspotService.class);
        intent.setAction(HotspotService.ACTION_START);
        requireContext().startForegroundService(intent);
        setLoadingState(true);
    }

    private void stopHotspot() {
        Intent intent = new Intent(requireContext(), HotspotService.class);
        intent.setAction(HotspotService.ACTION_STOP);
        requireContext().startService(intent);
    }

    // -------------------------------------------------------------------------
    // UI state updates
    // -------------------------------------------------------------------------

    private void onHotspotStarted(String ssid, String passphrase) {
        hotspotActive = true;
        setLoadingState(false);
        binding.switchHotspot.setChecked(true);
        binding.textSsid.setText(ssid);
        binding.textPassword.setText(passphrase);
        binding.layoutCredentials.setVisibility(View.VISIBLE);
        generateQrCode(ssid, passphrase);
    }

    private void onHotspotStopped() {
        hotspotActive = false;
        setLoadingState(false);
        binding.switchHotspot.setChecked(false);
        binding.layoutCredentials.setVisibility(View.GONE);
        binding.imageQrCode.setVisibility(View.GONE);
    }

    private void onHotspotFailed(int reason) {
        hotspotActive = false;
        setLoadingState(false);
        binding.switchHotspot.setChecked(false);
        String msg = getString(R.string.hotspot_failed, reason);
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void setLoadingState(boolean loading) {
        binding.progressHotspot.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.switchHotspot.setEnabled(!loading);
    }

    private void togglePasswordVisibility() {
        if (binding.textPassword.getInputType() ==
                android.text.InputType.TYPE_CLASS_TEXT) {
            binding.textPassword.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT
                            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.buttonTogglePassword.setText(R.string.show);
        } else {
            binding.textPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            binding.buttonTogglePassword.setText(R.string.hide);
        }
    }

    private void generateQrCode(String ssid, String passphrase) {
        try {
            String wifiUri = "WIFI:S:" + escapeQrField(ssid)
                    + ";T:WPA;P:" + escapeQrField(passphrase)
                    + ";;";
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(wifiUri, BarcodeFormat.QR_CODE, 400, 400);
            binding.imageQrCode.setImageBitmap(bitmap);
            binding.imageQrCode.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            binding.imageQrCode.setVisibility(View.GONE);
        }
    }

    private static String escapeQrField(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace(";", "\\;")
                    .replace(",", "\\,")
                    .replace("\"", "\\\"");
    }

    private void openSystemTethering() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
                "com.android.settings.TetherSettings");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    R.string.cannot_open_tethering_settings, Toast.LENGTH_SHORT).show();
        }
    }
}
