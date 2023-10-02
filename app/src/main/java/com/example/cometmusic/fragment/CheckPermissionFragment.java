package com.example.cometmusic.fragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.cometmusic.R;
import com.example.cometmusic.model.SharedData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class CheckPermissionFragment extends Fragment {

    private SharedData sharedData;


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

    private void requestStoragePermissions() {
        if (checkStoragePermissions())
            navigateToCurrentList();
        else
            userResponses();
    }

    public boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return requireActivity().checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                && requireActivity().checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;

        } else {
            int write = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void userResponses() {
        String[] permissionArray;

        String rationalMessage;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // request permission
            permissionArray = new String[]{
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
            };
            rationalMessage = "1. Allow to read and write Media. \n\n" +
                    "2. Allow to read Images.\n";
        } else {
            // request permission
            permissionArray = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            rationalMessage = "Allow to read and write Media and Images.";
        }

        boolean shouldShowRationale = !checkStoragePermissions();

        if (shouldShowRationale) {

            MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Requesting Permission")
                    .setMessage(rationalMessage)
                    .setCancelable(false)
                    .setPositiveButton("Allow", (dialog, which) -> {
                        // request permissions
                        requestPermissionLauncher.launch(permissionArray);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Toast.makeText(requireContext(), "Permission Denied.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });

            AlertDialog dialog = alertDialog.create();

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

    private void handleDeniedStoragePermissions() {
        sharedData.setDeniedTimes(sharedData.getDeniedTimes() + 1);
        if (sharedData.getDeniedTimes() < 2) {
            requestStoragePermissions();
            Toast.makeText(requireContext(), "Permissions Denied.", Toast.LENGTH_SHORT).show();
        } else {
            openAppSettings();
            Toast.makeText(requireContext(), "Permission Already Denied TWICE, Please Manually Enable.", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }


    private void navigateToCurrentList() {
        Navigation.findNavController(requireView())
                .navigate(R.id.currentListFragment);
    }

}