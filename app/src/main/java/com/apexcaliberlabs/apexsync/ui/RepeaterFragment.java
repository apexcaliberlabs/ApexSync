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
import com.apexcaliberlabs.apexsync.databinding.FragmentRepeaterBinding;
import com.apexcaliberlabs.apexsync.service.RepeaterService;
import com.apexcaliberlabs.apexsync.util.NetworkUtils;
import com.apexcaliberlabs.apexsync.util.PermissionHelper;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Fragment that exposes the Wi-Fi Direct repeater feature to the user.
 *
 * <p>Communicates with {@link RepeaterService} via explicit Intents and listens for
 * status broadcasts.
 */
public class RepeaterFragment extends Fragment {

    private FragmentRepeaterBinding binding;

    // -------------------------------------------------------------------------
    // Broadcast receiver
    // -------------------------------------------------------------------------

    private final BroadcastReceiver repeaterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case RepeaterService.ACTION_REPEATER_STARTED:
                    String networkName =
                            intent.getStringExtra(RepeaterService.EXTRA_NETWORK_NAME);
                    String passphrase =
                            intent.getStringExtra(RepeaterService.EXTRA_PASSPHRASE);
                    onRepeaterStarted(networkName, passphrase);
                    break;
                case RepeaterService.ACTION_REPEATER_STOPPED:
                    onRepeaterStopped();
                    break;
                case RepeaterService.ACTION_REPEATER_FAILED:
                    String error =
                            intent.getStringExtra(RepeaterService.EXTRA_ERROR_MESSAGE);
                    onRepeaterFailed(error);
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
        binding = FragmentRepeaterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show a warning if the device is not connected to Wi-Fi.
        if (!NetworkUtils.isWifiConnected(requireContext())) {
            binding.bannerNoWifi.setVisibility(View.VISIBLE);
        }

        binding.switchRepeater.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestPermissionsAndStart();
            } else {
                stopRepeater();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(RepeaterService.ACTION_REPEATER_STARTED);
        filter.addAction(RepeaterService.ACTION_REPEATER_STOPPED);
        filter.addAction(RepeaterService.ACTION_REPEATER_FAILED);
        requireContext().registerReceiver(repeaterReceiver, filter,
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(repeaterReceiver);
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
        String[] permissions = PermissionHelper.getRepeaterPermissions();
        if (PermissionHelper.hasPermissions(requireContext(), permissions)) {
            startRepeater();
        } else {
            PermissionHelper.requestPermissions(
                    this, permissions, PermissionHelper.REQUEST_CODE_REPEATER);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_REPEATER) {
            if (PermissionHelper.allGranted(grantResults)) {
                startRepeater();
            } else {
                binding.switchRepeater.setChecked(false);
                Toast.makeText(requireContext(),
                        R.string.permission_required_repeater, Toast.LENGTH_LONG).show();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Service interaction
    // -------------------------------------------------------------------------

    private void startRepeater() {
        Intent intent = new Intent(requireContext(), RepeaterService.class);
        intent.setAction(RepeaterService.ACTION_START);
        requireContext().startForegroundService(intent);
        setLoadingState(true);
    }

    private void stopRepeater() {
        Intent intent = new Intent(requireContext(), RepeaterService.class);
        intent.setAction(RepeaterService.ACTION_STOP);
        requireContext().startService(intent);
    }

    // -------------------------------------------------------------------------
    // UI state updates
    // -------------------------------------------------------------------------

    private void onRepeaterStarted(String networkName, String passphrase) {
        setLoadingState(false);
        binding.switchRepeater.setChecked(true);
        binding.textNetworkName.setText(networkName);
        binding.textPassphrase.setText(passphrase);
        binding.layoutCredentials.setVisibility(View.VISIBLE);
        generateQrCode(networkName, passphrase);
    }

    private void onRepeaterStopped() {
        setLoadingState(false);
        binding.switchRepeater.setChecked(false);
        binding.layoutCredentials.setVisibility(View.GONE);
        binding.imageQrCode.setVisibility(View.GONE);
    }

    private void onRepeaterFailed(String error) {
        setLoadingState(false);
        binding.switchRepeater.setChecked(false);
        String msg = getString(R.string.repeater_failed, error != null ? error : "");
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void setLoadingState(boolean loading) {
        binding.progressRepeater.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.switchRepeater.setEnabled(!loading);
    }

    private void generateQrCode(String networkName, String passphrase) {
        try {
            String wifiUri = "WIFI:S:" + escapeQrField(networkName)
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
}
