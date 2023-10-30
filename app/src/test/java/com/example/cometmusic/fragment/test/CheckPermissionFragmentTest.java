package com.example.cometmusic.fragment.test;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.example.cometmusic.R;
import com.example.cometmusic.testUtils.buildversion.BuildVersionImpl;
import com.example.cometmusic.fragment.CheckPermissionFragment;
import com.example.cometmusic.model.SharedData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class CheckPermissionFragmentTest {

    @Mock
    private CheckPermissionFragment fragment;

    @Mock
    private Context mockContext;

    @Mock
    private SharedData mockSharedData;

    @Mock
    private BuildVersionImpl mockBuildVersionProvider;

    @Before
    public void setup() {
        // initialize the object with Mock Tag
        MockitoAnnotations.openMocks(this);

        mockSharedData = mock(SharedData.class);

        mockBuildVersionProvider = mock(BuildVersionImpl.class);

        fragment = spy(new CheckPermissionFragment(mockSharedData, mockBuildVersionProvider));
    }

    @Test
    public void checkPermissionFragment_OnResumeWithCheckStoragePermissionsIsTrue_CallsNavigateToCurrentList() {

        doReturn(true).when(fragment).checkStoragePermissions();

        doNothing().when(fragment).navigateToCurrentList();

        // the method is tested
        fragment.onResume();

        verify(fragment, times(1)).navigateToCurrentList();
    }

    @Test
    public void checkPermissionFragment_OnResumeWithCheckStoragePermissionsIsFalse_CallsRequestStoragePermissions() {

        doNothing().when(fragment).requestStoragePermissions();

        doReturn(false).when(fragment).checkStoragePermissions();

        // the method is tested
        fragment.onResume();

        verify(fragment, times(1)).requestStoragePermissions();
    }

    @Test
    public void checkPermissionFragment_RequestStoragePermissionWithCheckStoragePermissionsIsTrue_CallsNavigateToCurrentList() {

        doNothing().when(fragment).navigateToCurrentList();

        doReturn(true).when(fragment).checkStoragePermissions();

        // call the method that should be tested
        fragment.requestStoragePermissions();

        verify(fragment, times(1)).navigateToCurrentList();
    }

    @Test
    public void checkPermissionFragment_RequestStoragePermissionWithCheckStoragePermissionsIsFalse_CallsNavigateToCurrentList() {

        doNothing().when(fragment).userResponses();

        doReturn(false).when(fragment).checkStoragePermissions();

        // call the method that should be tested
        fragment.requestStoragePermissions();

        verify(fragment, times(1)).userResponses();
    }

    @Test
    public void checkPermissionFragment_CheckStoragePermissionsWithVersionIsNoLessThanTiramisuAndAllGranted_ReturnsTrue() {

        when(mockBuildVersionProvider.isTiramisuOrAbove()).thenReturn(true);

        FragmentActivity mockActivity = mock(FragmentActivity.class);

        doReturn(mockActivity).when(fragment).requireActivity();

        when(fragment.requireActivity()
                .checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO))
                .thenReturn(PERMISSION_GRANTED);

        when(fragment.requireActivity()
                .checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES))
                .thenReturn(PERMISSION_GRANTED);

        // call the method that should be tested
        assertTrue(fragment.checkStoragePermissions());
    }

    @Test
    public void checkPermissionFragment_CheckStoragePermissionsWithVersionIsNoLessThanTiramisuAndNotAllGranted_ReturnsFalse() {

        when(mockBuildVersionProvider.isTiramisuOrAbove()).thenReturn(true);

        FragmentActivity mockActivity = mock(FragmentActivity.class);

        doReturn(mockActivity).when(fragment).requireActivity();

        when(fragment.requireActivity()
                .checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO))
                .thenReturn(PERMISSION_DENIED);

        when(fragment.requireActivity()
                .checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES))
                .thenReturn(PERMISSION_GRANTED);

        // call the method that should be tested
        assertFalse(fragment.checkStoragePermissions());
    }

    @Test
    public void checkPermissionFragment_CheckStoragePermissionsWithVersionIsNoLessThanTiramisuAndAllDenied_ReturnsFalse() {

        when(mockBuildVersionProvider.isTiramisuOrAbove()).thenReturn(true);

        FragmentActivity mockActivity = mock(FragmentActivity.class);

        doReturn(mockActivity).when(fragment).requireActivity();

        when(fragment.requireActivity()
                .checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO))
                .thenReturn(PERMISSION_DENIED);

        when(fragment.requireActivity()
                .checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES))
                .thenReturn(PERMISSION_DENIED);

        // call the method that should be tested
        assertFalse(fragment.checkStoragePermissions());
    }

    @Test
    public void checkPermissionFragment_CheckStoragePermissionsWithVersionIsLessThanTiramisuAndAllGranted_ReturnsTrue() {

        when(mockBuildVersionProvider.isTiramisuOrAbove()).thenReturn(false);

        doReturn(mockContext).when(fragment).requireContext();

        when(fragment.requireContext()
                .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .thenReturn(PERMISSION_GRANTED);

        when(fragment.requireContext()
                .checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                .thenReturn(PERMISSION_GRANTED);

        // call the method that should be tested
        assertTrue(fragment.checkStoragePermissions());
    }

    @Test
    public void checkPermissionFragment_CheckStoragePermissionsWithVersionIsLessThanTiramisuAndNotAllGranted_ReturnsFalse() {

        when(mockBuildVersionProvider.isTiramisuOrAbove()).thenReturn(false);

        doReturn(mockContext).when(fragment).requireContext();

        when(fragment.requireContext()
                .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .thenReturn(PERMISSION_GRANTED);

        when(fragment.requireContext()
                .checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                .thenReturn(PERMISSION_DENIED);

        // call the method that should be tested
        assertFalse(fragment.checkStoragePermissions());
    }

    @Test
    public void checkPermissionFragment_CheckStoragePermissionsWithVersionIsLessThanTiramisuAndAllDenied_ReturnsFalse() {

        doReturn(mockContext).when(fragment).requireContext();

        when(mockBuildVersionProvider.isTiramisuOrAbove()).thenReturn(false);

        when(fragment.requireContext()
                .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .thenReturn(PERMISSION_DENIED);

        when(fragment.requireContext()
                .checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                .thenReturn(PERMISSION_DENIED);

        // call the method that should be tested
        assertFalse(fragment.checkStoragePermissions());
    }

    @Test
    public void checkPermissionFragment_UserResponseWithVersionIsNotLessThanTiramisuAndCheckStoragePermissionsIsTrue_DoNothing() {

        doReturn(true).when(mockBuildVersionProvider).isTiramisuOrAbove();

        doReturn(true).when(fragment).checkStoragePermissions();

        doReturn(mockContext).when(fragment).requireContext();

        // call the method that should be tested
        fragment.userResponses();

        verify(fragment).checkStoragePermissions();

        verify(fragment, never()).createAlertDialog(any(), any());
    }

    @Test
    public void checkPermissionFragment_UserResponseWithVersionIsNotLessThanTiramisuAndCheckStoragePermissionsIsFalse_CreateAlertDialog() {

        doReturn(true).when(mockBuildVersionProvider).isTiramisuOrAbove();

        doReturn(false).when(fragment).checkStoragePermissions();

        doReturn(mockContext).when(fragment).requireContext();

        String[] expectedPermissionArray = {
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES
        };

        String[] expectedString = {
                ApplicationProvider.getApplicationContext().getString(R.string.Tiramisu_or_above_permission_message_1),
                ApplicationProvider.getApplicationContext().getString(R.string.Tiramisu_or_above_permission_message_2)
        };

        String expectedRationalMessage = expectedString[0] + "\n\n" + expectedString[1];

        when(fragment.requireContext()
                .getString(R.string.Tiramisu_or_above_permission_message_1))
                .thenReturn(expectedString[0]);

        when(fragment.requireContext()
                .getString(R.string.Tiramisu_or_above_permission_message_2))
                .thenReturn(expectedString[1]);

        doNothing().when(fragment).createAlertDialog(any(), any());

        when(fragment.requireContext()).thenReturn(mockContext);

        // call the method that should be tested
        fragment.userResponses();

        verify(fragment).checkStoragePermissions();

        verify(fragment).
                createAlertDialog(expectedPermissionArray, expectedRationalMessage);
    }

    @Test
    public void checkPermissionFragment_UserResponseWithVersionIsLessThanTiramisuAndCheckStoragePermissionsIsTrue_DoNothing() {

        doReturn(false).when(mockBuildVersionProvider).isTiramisuOrAbove();

        doReturn(true).when(fragment).checkStoragePermissions();

        doReturn(mockContext).when(fragment).requireContext();

        // call the method that should be tested
        fragment.userResponses();

        verify(fragment).checkStoragePermissions();

        verify(fragment, never()).createAlertDialog(any(), any());
    }

    @Test
    public void checkPermissionFragment_UserResponseWithVersionIsLessThanTiramisuAndCheckStoragePermissionsIsFalse_CreateAlertDialog() {

        doReturn(false).when(mockBuildVersionProvider).isTiramisuOrAbove();

        doReturn(false).when(fragment).checkStoragePermissions();

        doReturn(mockContext).when(fragment).requireContext();

        String[] expectedPermissionArray = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        String expectedString =
                ApplicationProvider.getApplicationContext().getString(R.string.less_than_Tiramisu_permission_message);

        when(fragment.requireContext().getString(R.string.less_than_Tiramisu_permission_message)).thenReturn(expectedString);

        doNothing().when(fragment).createAlertDialog(any(), any());

        when(fragment.requireContext()).thenReturn(mockContext);

        // call the method that should be tested
        fragment.userResponses();

        verify(fragment).checkStoragePermissions();

        verify(fragment)
                .createAlertDialog(expectedPermissionArray, expectedString);
    }

    @Test
    public void checkPermissionFragment_HandleDeniedStoragePermissionsWithDeniedTimeIsLessThanTwo_CallsRequestStoragePermissions() {

        doReturn(mockContext).when(fragment).requireContext();

        int mockDeniedTimes = 0;

        doReturn(mockDeniedTimes).when(mockSharedData).getDeniedTimes();

        doNothing().when(mockSharedData).setDeniedTimes(mockDeniedTimes + 1);

        doNothing().when(fragment).requestStoragePermissions();

        Resources resources = mock(Resources.class);

        String expectedText = ApplicationProvider.getApplicationContext().getString(R.string.permission_denied);
        when(mockContext.getResources()).thenReturn(resources);
        when(resources.getString(R.string.permission_denied)).thenReturn(expectedText);

        // call the method that should be tested
        fragment.handleDeniedStoragePermissions();

        verify(mockSharedData).getDeniedTimes();
        verify(mockSharedData).setDeniedTimes(mockDeniedTimes + 1);

        verify(fragment).requestStoragePermissions();

        Toast toast = ShadowToast.getLatestToast();
        String actualText = ShadowToast.getTextOfLatestToast();

        assertEquals(toast.getDuration(), Toast.LENGTH_SHORT);
        assertEquals(expectedText, actualText);
    }

    @Test
    public void checkPermissionFragment_HandleDeniedStoragePermissionsWithDeniedTimeIsNoLessThanTwo_CallsOpenAppSettings() {

        doReturn(mockContext).when(fragment).requireContext();

        int mockDeniedTimes = 1;

        doReturn(mockDeniedTimes).when(mockSharedData).getDeniedTimes();

        doNothing().when(mockSharedData).setDeniedTimes(mockDeniedTimes + 1);

        doNothing().when(fragment).openAppSettings();

        Resources resources = mock(Resources.class);

        String expectedText = ApplicationProvider.getApplicationContext().getString(R.string.permission_denied_twice);
        when(mockContext.getResources()).thenReturn(resources);
        when(resources.getString(R.string.permission_denied_twice)).thenReturn(expectedText);

        // call the method that should be tested
        fragment.handleDeniedStoragePermissions();

        verify(mockSharedData).getDeniedTimes();
        verify(mockSharedData).setDeniedTimes(mockDeniedTimes + 1);

        verify(fragment).openAppSettings();

        Toast toast = ShadowToast.getLatestToast();
        String actualText = ShadowToast.getTextOfLatestToast();

        assertEquals(toast.getDuration(), Toast.LENGTH_LONG);
        assertEquals(expectedText, actualText);
    }

    @Test
    public void checkPermissionFragment_OpenAppSettings_TriggerStartActivity() {

        FragmentActivity mockActivity = mock(FragmentActivity.class);
        String mockPackageName = "testPackageName";

        doReturn(mockActivity).when(fragment).requireActivity();
        when(mockActivity.getPackageName()).thenReturn(mockPackageName);

        Intent expectedIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri expectedUri = Uri.fromParts("package", mockActivity.getPackageName(), null);

        expectedIntent.setData(expectedUri);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(fragment).startActivity(intentCaptor.capture());

        // call the method that should be tested
        fragment.openAppSettings();

        // assert intent action and data
        // cuz simply compare two Intent is equal, it will compare their reference
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals(expectedIntent.getAction(), capturedIntent.getAction());
        assertEquals(expectedIntent.getData(), capturedIntent.getData());
    }


}
