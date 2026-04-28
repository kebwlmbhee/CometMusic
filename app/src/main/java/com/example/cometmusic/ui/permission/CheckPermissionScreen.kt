package com.example.cometmusic.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.cometmusic.data.SharedData
import com.example.cometmusic.utils.buildversion.BuildVersionImpl

@Composable
fun CheckPermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val sharedData = remember { SharedData(context) }
    val buildVersionProvider = remember { BuildVersionImpl(Build.VERSION.SDK_INT) }

    fun checkStoragePermissions(): Boolean {
        return if (buildVersionProvider.isTiramisuOrAbove()) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissions = if (buildVersionProvider.isTiramisuOrAbove()) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    var showDialog by remember { mutableStateOf(false) }
    var permissionsChecked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            onPermissionGranted()
        } else {
            val deniedTimes = sharedData.deniedTimes + 1
            sharedData.deniedTimes = deniedTimes
            if (deniedTimes < 2) {
                Toast.makeText(context, "Permission Denied.", Toast.LENGTH_SHORT).show()
                showDialog = true
            } else {
                Toast.makeText(
                    context,
                    "Permission Already Denied TWICE, Please Manually Enable.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

    LaunchedEffect(permissionsChecked) {
        if (checkStoragePermissions()) {
            onPermissionGranted()
        } else {
            showDialog = true
        }
        permissionsChecked = true
    }

    if (showDialog) {
        val rationalMessage = if (buildVersionProvider.isTiramisuOrAbove()) {
            "1. Allow to read and write Media.\n\n2. Allow to read Images."
        } else {
            "Allow to read and write Media and Images."
        }
        PermissionDialog(
            message = rationalMessage,
            onAllow = {
                showDialog = false
                permissionLauncher.launch(permissions)
            },
            onDeny = {
                showDialog = false
                Toast.makeText(context, "Permission Denied.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize())
}
