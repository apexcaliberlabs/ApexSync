package com.apexcaliberlabs.apexsync.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PermissionHelper}.
 */
public class PermissionHelperTest {

    @Test
    public void allGranted_returnsTrueWhenAllGranted() {
        int[] results = {0, 0, 0}; // PackageManager.PERMISSION_GRANTED == 0
        assertTrue(PermissionHelper.allGranted(results));
    }

    @Test
    public void allGranted_returnsFalseWhenAnyDenied() {
        int[] results = {0, -1, 0}; // PackageManager.PERMISSION_DENIED == -1
        assertFalse(PermissionHelper.allGranted(results));
    }

    @Test
    public void allGranted_returnsFalseForEmptyArray() {
        assertFalse(PermissionHelper.allGranted(new int[0]));
    }

    @Test
    public void getHotspotPermissions_isNotEmpty() {
        String[] permissions = PermissionHelper.getHotspotPermissions();
        assertNotNull(permissions);
        assertTrue(permissions.length > 0);
    }

    @Test
    public void getRepeaterPermissions_isNotEmpty() {
        String[] permissions = PermissionHelper.getRepeaterPermissions();
        assertNotNull(permissions);
        assertTrue(permissions.length > 0);
    }
}
