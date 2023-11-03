package com.example.cometmusic.fragment;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.cometmusic.R;
import com.example.cometmusic.utils.buildversion.BuildVersionImpl;
import com.example.cometmusic.model.SharedData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class CheckPermissionFragment extends Fragment {

    private SharedData sharedData;

    private final BuildVersionImpl buildVersionImpl;

    // for prod
    public CheckPermissionFragment() {
        buildVersionImpl = new BuildVersionImpl(Build.VERSION.SDK_INT);
    }


    // for test
    public CheckPermissionFragment(SharedData sharedData, BuildVersionImpl buildVersionProvider) {
        this.sharedData = sharedData;
        this.buildVersionImpl = buildVersionProvider;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedData = new SharedData(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_check_permission, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkStoragePermissions()) {
            navigateToCurrentList();
        } else {
            requestStoragePermissions();
        }
    }

    public void requestStoragePermissions() {
        if (checkStoragePermissions())
            navigateToCurrentList();
        else
            userResponses();
    }

    public boolean checkStoragePermissions() {
        if (buildVersionImpl.isTiramisuOrAbove()) {
            return requireActivity().checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PERMISSION_GRANTED
                && requireActivity().checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PERMISSION_GRANTED;

        } else {
            int write = requireContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return read == PERMISSION_GRANTED && write == PERMISSION_GRANTED;
        }
    }

    public void userResponses() {
        String[] permissionArray;

        String rationalMessage;

        if (buildVersionImpl.isTiramisuOrAbove()) {
            // request permission
            permissionArray = new String[] {
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
            rationalMessage = requireContext().getString(R.string.Tiramisu_or_above_permission_message_1) + "\n\n" +
                              requireContext().getString(R.string.Tiramisu_or_above_permission_message_2);
        } else {
            // request permission
            permissionArray = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            rationalMessage = requireContext().getString(R.string.less_than_Tiramisu_permission_message);
        }

        boolean shouldShowRationale = !checkStoragePermissions();

        if (shouldShowRationale) {
            createAlertDialog(permissionArray, rationalMessage);
        }
    }

    // UI Dialog
    public void createAlertDialog(String[] permissionArray, String rationalMessage) {
        MaterialAlertDialogBuilder alertDialogBuilder =
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.requesting_permission)
                    .setMessage(rationalMessage)
                    .setCancelable(false)
                    .setPositiveButton(R.string.allow, (dialog, which) -> {
                        // request permissions
                        requestPermissionLauncher.launch(permissionArray);
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                });

        AlertDialog dialog = alertDialogBuilder.create();

        dialog.show();

        // get theme secondary color
        TypedArray typedArray = requireContext().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorSecondary});
        int colorSecondary = typedArray.getColor(0, 0);
        typedArray.recycle();

        // set Allow button color
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setTextColor(colorSecondary);

        // set Cancel button color
        Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negativeButton.setTextColor(colorSecondary);
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {

                        boolean allPermissionsGranted = true;

                        for (Boolean isGranted : permissions.values()) {
                            if (!isGranted) {
                                allPermissionsGranted = false;
                                break;
                            }
                        }

                        if(allPermissionsGranted) {
                            navigateToCurrentList();
                        }
                        else {
                            handleDeniedStoragePermissions();
                        }
                    });

    public void handleDeniedStoragePermissions() {
        int deniedTimes = sharedData.getDeniedTimes();
        deniedTimes += 1;
        sharedData.setDeniedTimes(deniedTimes);
        if (deniedTimes < 2) {
            requestStoragePermissions();
            Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
        } else {
            openAppSettings();
            Toast.makeText(requireContext(), R.string.permission_denied_twice, Toast.LENGTH_LONG).show();
        }
    }

    public void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
        intent.setData(uri);

        startActivity(intent);
    }


    // UI navigation
    public void navigateToCurrentList() {
        Navigation.findNavController(requireView())
                .navigate(R.id.currentListFragment);
    }

}