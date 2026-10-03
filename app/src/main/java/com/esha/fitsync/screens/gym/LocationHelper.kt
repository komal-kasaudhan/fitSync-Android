package com.esha.fitsync.screens.gym

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    // Default fallback coordinate from backend seed proof: Sector 4, Bokaro
    const val DEFAULT_LAT = 23.6693
    const val DEFAULT_LNG = 86.1511
    const val DEFAULT_CITY_NAME = "Sector 4, Bokaro"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    fun showGpsDisabledDialog(context: Context, onCancel: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle("Location (GPS) is Disabled")
            .setMessage("FitSync needs GPS to find gyms nearby accurately. Please turn on Location in Settings.")
            .setPositiveButton("Turn On GPS") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
            .setNegativeButton("Use Default Location") { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }
            .setCancelable(false)
            .show()
    }

    fun showPermissionRationaleDialog(
        context: Context,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Location Permission Needed")
            .setMessage("FitSync uses your location to discover fitness centers and verified gym slots close to you.")
            .setPositiveButton("Grant Access") { _, _ -> onPositive() }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                onNegative()
            }
            .show()
    }

    fun showSettingsRedirectDialog(context: Context, onDismiss: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle("Permission Required")
            .setMessage("Location access has been permanently declined. You can enable it anytime in App Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            .show()
    }

    suspend fun getDeviceLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        return suspendCancellableCoroutine { cont ->
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            // Fallback to lastLocation if current location returned null
                            client.lastLocation.addOnSuccessListener { lastLoc ->
                                cont.resume(lastLoc)
                            }.addOnFailureListener {
                                cont.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Attempt last location on error
                        client.lastLocation.addOnSuccessListener { lastLoc ->
                            cont.resume(lastLoc)
                        }.addOnFailureListener {
                            cont.resume(null)
                        }
                    }
            } catch (e: SecurityException) {
                cont.resume(null)
            }

            cont.invokeOnCancellation {
                cts.cancel()
            }
        }
    }
}
