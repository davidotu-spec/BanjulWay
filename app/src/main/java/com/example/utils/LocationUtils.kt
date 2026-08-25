package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * State class holding location data, permission status, and identified pick-up point metadata for the user/passenger.
 */
data class UserLocationState(
    val latitude: Double = DEFAULT_GAMBIA_LAT,
    val longitude: Double = DEFAULT_GAMBIA_LNG,
    val locationName: String = DEFAULT_LOCATION_NAME,
    val isPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isFallbackLocation: Boolean = true,
    val detectedZone: String = "Banjul City Center",
    val closestPickupPointName: String = "Albert Market Main Gate, Banjul",
    val pickupGuidance: String = "Wait by the designated passenger taxi shelter.",
    val distanceToPickupMeters: Double = 0.0,
    val recommendedPickups: List<PickupPointProximity> = emptyList()
) {
    val coordinatesString: String
        get() = String.format(java.util.Locale.US, "%.5f° N, %.5f° W", latitude, -longitude)

    companion object {
        const val DEFAULT_GAMBIA_LAT = 13.4549
        const val DEFAULT_GAMBIA_LNG = -16.5790
        const val DEFAULT_LOCATION_NAME = "Albert Market Main Gate, Banjul"
    }
}

/**
 * Utility functions for checking location permissions, tracking live location,
 * and resolving pick-up points across Banjul and Kanifing.
 */
object LocationUtils {

    /**
     * Checks if the app has FINE or COARSE location permission granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Retrieves current location using FusedLocationProviderClient and identifies
     * the optimal designated pick-up point in Banjul or Kanifing.
     */
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onResult: (UserLocationState) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            val pickupInfo = LocationTrackingService.identifyPickupPoint(
                UserLocationState.DEFAULT_GAMBIA_LAT,
                UserLocationState.DEFAULT_GAMBIA_LNG
            )
            onResult(
                UserLocationState(
                    latitude = UserLocationState.DEFAULT_GAMBIA_LAT,
                    longitude = UserLocationState.DEFAULT_GAMBIA_LNG,
                    locationName = pickupInfo.closestPickupPoint.name,
                    isPermissionGranted = false,
                    isLoading = false,
                    errorMessage = "Location permission denied. Using default Banjul pickup point.",
                    isFallbackLocation = true,
                    detectedZone = pickupInfo.zoneName,
                    closestPickupPointName = pickupInfo.closestPickupPoint.name,
                    pickupGuidance = pickupInfo.closestPickupPoint.pickupGuidance,
                    distanceToPickupMeters = pickupInfo.distanceMeters,
                    recommendedPickups = LocationTrackingService.computeRecommendations(
                        UserLocationState.DEFAULT_GAMBIA_LAT,
                        UserLocationState.DEFAULT_GAMBIA_LNG
                    )
                )
            )
            return
        }

        try {
            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val pickupInfo = LocationTrackingService.identifyPickupPoint(location.latitude, location.longitude)
                    val recs = LocationTrackingService.computeRecommendations(location.latitude, location.longitude)
                    onResult(
                        UserLocationState(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            locationName = pickupInfo.suggestedPickupName,
                            isPermissionGranted = true,
                            isLoading = false,
                            errorMessage = null,
                            isFallbackLocation = false,
                            detectedZone = pickupInfo.zoneName,
                            closestPickupPointName = pickupInfo.closestPickupPoint.name,
                            pickupGuidance = pickupInfo.closestPickupPoint.pickupGuidance,
                            distanceToPickupMeters = pickupInfo.distanceMeters,
                            recommendedPickups = recs
                        )
                    )
                } else {
                    // Try last known location as fallback
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            val pickupInfo = LocationTrackingService.identifyPickupPoint(lastLoc.latitude, lastLoc.longitude)
                            val recs = LocationTrackingService.computeRecommendations(lastLoc.latitude, lastLoc.longitude)
                            onResult(
                                UserLocationState(
                                    latitude = lastLoc.latitude,
                                    longitude = lastLoc.longitude,
                                    locationName = pickupInfo.suggestedPickupName,
                                    isPermissionGranted = true,
                                    isLoading = false,
                                    errorMessage = null,
                                    isFallbackLocation = false,
                                    detectedZone = pickupInfo.zoneName,
                                    closestPickupPointName = pickupInfo.closestPickupPoint.name,
                                    pickupGuidance = pickupInfo.closestPickupPoint.pickupGuidance,
                                    distanceToPickupMeters = pickupInfo.distanceMeters,
                                    recommendedPickups = recs
                                )
                            )
                        } else {
                            val pickupInfo = LocationTrackingService.identifyPickupPoint(
                                UserLocationState.DEFAULT_GAMBIA_LAT,
                                UserLocationState.DEFAULT_GAMBIA_LNG
                            )
                            onResult(
                                UserLocationState(
                                    latitude = UserLocationState.DEFAULT_GAMBIA_LAT,
                                    longitude = UserLocationState.DEFAULT_GAMBIA_LNG,
                                    locationName = pickupInfo.closestPickupPoint.name,
                                    isPermissionGranted = true,
                                    isLoading = false,
                                    errorMessage = "GPS signal unavailable. Defaulting to Banjul Center.",
                                    isFallbackLocation = true,
                                    detectedZone = pickupInfo.zoneName,
                                    closestPickupPointName = pickupInfo.closestPickupPoint.name,
                                    pickupGuidance = pickupInfo.closestPickupPoint.pickupGuidance,
                                    distanceToPickupMeters = pickupInfo.distanceMeters,
                                    recommendedPickups = LocationTrackingService.computeRecommendations(
                                        UserLocationState.DEFAULT_GAMBIA_LAT,
                                        UserLocationState.DEFAULT_GAMBIA_LNG
                                    )
                                )
                            )
                        }
                    }.addOnFailureListener {
                        val pickupInfo = LocationTrackingService.identifyPickupPoint(
                            UserLocationState.DEFAULT_GAMBIA_LAT,
                            UserLocationState.DEFAULT_GAMBIA_LNG
                        )
                        onResult(
                            UserLocationState(
                                latitude = UserLocationState.DEFAULT_GAMBIA_LAT,
                                longitude = UserLocationState.DEFAULT_GAMBIA_LNG,
                                locationName = pickupInfo.closestPickupPoint.name,
                                isPermissionGranted = true,
                                isLoading = false,
                                errorMessage = "Failed to fetch GPS location. Defaulting to Banjul Center.",
                                isFallbackLocation = true,
                                detectedZone = pickupInfo.zoneName,
                                closestPickupPointName = pickupInfo.closestPickupPoint.name,
                                pickupGuidance = pickupInfo.closestPickupPoint.pickupGuidance,
                                distanceToPickupMeters = pickupInfo.distanceMeters,
                                recommendedPickups = LocationTrackingService.computeRecommendations(
                                    UserLocationState.DEFAULT_GAMBIA_LAT,
                                    UserLocationState.DEFAULT_GAMBIA_LNG
                                )
                            )
                        )
                    }
                }
            }.addOnFailureListener { e ->
                val pickupInfo = LocationTrackingService.identifyPickupPoint(
                    UserLocationState.DEFAULT_GAMBIA_LAT,
                    UserLocationState.DEFAULT_GAMBIA_LNG
                )
                onResult(
                    UserLocationState(
                        latitude = UserLocationState.DEFAULT_GAMBIA_LAT,
                        longitude = UserLocationState.DEFAULT_GAMBIA_LNG,
                        locationName = pickupInfo.closestPickupPoint.name,
                        isPermissionGranted = true,
                        isLoading = false,
                        errorMessage = "Location error: ${e.localizedMessage ?: "Unknown error"}. Using fallback location.",
                        isFallbackLocation = true,
                        detectedZone = pickupInfo.zoneName,
                        closestPickupPointName = pickupInfo.closestPickupPoint.name,
                        pickupGuidance = pickupInfo.closestPickupPoint.pickupGuidance,
                        distanceToPickupMeters = pickupInfo.distanceMeters,
                        recommendedPickups = LocationTrackingService.computeRecommendations(
                            UserLocationState.DEFAULT_GAMBIA_LAT,
                            UserLocationState.DEFAULT_GAMBIA_LNG
                        )
                    )
                )
            }
        } catch (e: Exception) {
            val pickupInfo = LocationTrackingService.identifyPickupPoint(
                UserLocationState.DEFAULT_GAMBIA_LAT,
                UserLocationState.DEFAULT_GAMBIA_LNG
            )
            onResult(
                UserLocationState(
                    latitude = UserLocationState.DEFAULT_GAMBIA_LAT,
                    longitude = UserLocationState.DEFAULT_GAMBIA_LNG,
                    locationName = pickupInfo.closestPickupPoint.name,
                    isPermissionGranted = false,
                    isLoading = false,
                    errorMessage = "Error initializing GPS: ${e.localizedMessage}",
                    isFallbackLocation = true,
                    detectedZone = pickupInfo.zoneName,
                    closestPickupPointName = pickupInfo.closestPickupPoint.name,
                    pickupGuidance = pickupInfo.closestPickupPoint.pickupGuidance,
                    distanceToPickupMeters = pickupInfo.distanceMeters,
                    recommendedPickups = LocationTrackingService.computeRecommendations(
                        UserLocationState.DEFAULT_GAMBIA_LAT,
                        UserLocationState.DEFAULT_GAMBIA_LNG
                    )
                )
            )
        }
    }

    /**
     * Calculates an estimated fare based on the distance between the user's current location
     * and a selected destination before a ride is requested.
     */
    fun calculateEstimatedFare(
        currentLat: Double,
        currentLng: Double,
        destLat: Double,
        destLng: Double,
        vehicleType: String = "CAR",
        surchargeTier: String = "STANDARD"
    ): FareEstimateResult {
        return TripFareEstimationService.estimateFare(
            pLat = currentLat,
            pLng = currentLng,
            dLat = destLat,
            dLng = destLng,
            vehicleType = vehicleType,
            surchargeTier = surchargeTier
        )
    }

    /**
     * Convenience function to calculate fare using current [UserLocationState] and destination coordinates.
     */
    fun calculateEstimatedFare(
        userLocationState: UserLocationState,
        destLat: Double,
        destLng: Double,
        vehicleType: String = "CAR",
        surchargeTier: String = "STANDARD"
    ): FareEstimateResult {
        return calculateEstimatedFare(
            currentLat = userLocationState.latitude,
            currentLng = userLocationState.longitude,
            destLat = destLat,
            destLng = destLng,
            vehicleType = vehicleType,
            surchargeTier = surchargeTier
        )
    }
}

/**
 * Controller handle returned by [rememberLocationState] allowing UI components to
 * request location permissions or refresh location manually.
 */
class LocationHelperController internal constructor(
    val state: State<UserLocationState>,
    val requestPermissionAndLocation: () -> Unit,
    val refreshLocation: () -> Unit
)

/**
 * Composable helper function to request runtime location permissions and manage
 * the current GPS location state gracefully.
 */
@Composable
fun rememberLocationState(): LocationHelperController {
    val context = LocalContext.current
    val locationState = remember {
        mutableStateOf(
            UserLocationState(
                isPermissionGranted = LocationUtils.hasLocationPermission(context)
            )
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            locationState.value = locationState.value.copy(
                isLoading = true,
                isPermissionGranted = true,
                errorMessage = null
            )
            LocationUtils.fetchCurrentLocation(context) { newState ->
                locationState.value = newState
            }
        } else {
            locationState.value = UserLocationState(
                latitude = UserLocationState.DEFAULT_GAMBIA_LAT,
                longitude = UserLocationState.DEFAULT_GAMBIA_LNG,
                locationName = UserLocationState.DEFAULT_LOCATION_NAME,
                isPermissionGranted = false,
                isLoading = false,
                errorMessage = "Location permission was denied. Using default Banjul pickup point.",
                isFallbackLocation = true
            )
        }
    }

    val requestPermissionAndLocation: () -> Unit = remember(context) {
        {
            if (LocationUtils.hasLocationPermission(context)) {
                locationState.value = locationState.value.copy(isLoading = true)
                LocationUtils.fetchCurrentLocation(context) { newState ->
                    locationState.value = newState
                }
            } else {
                locationState.value = locationState.value.copy(isLoading = true)
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    val refreshLocation: () -> Unit = remember(context) {
        {
            if (LocationUtils.hasLocationPermission(context)) {
                locationState.value = locationState.value.copy(isLoading = true)
                LocationUtils.fetchCurrentLocation(context) { newState ->
                    locationState.value = newState
                }
            } else {
                locationState.value = UserLocationState(
                    latitude = UserLocationState.DEFAULT_GAMBIA_LAT,
                    longitude = UserLocationState.DEFAULT_GAMBIA_LNG,
                    locationName = UserLocationState.DEFAULT_LOCATION_NAME,
                    isPermissionGranted = false,
                    isLoading = false,
                    errorMessage = "Location permission not granted.",
                    isFallbackLocation = true
                )
            }
        }
    }

    // Automatically check and fetch location if permission was already granted on launch
    LaunchedEffect(Unit) {
        if (LocationUtils.hasLocationPermission(context)) {
            locationState.value = locationState.value.copy(isLoading = true)
            LocationUtils.fetchCurrentLocation(context) { newState ->
                locationState.value = newState
            }
        }
    }

    return remember(locationState) {
        LocationHelperController(
            state = locationState,
            requestPermissionAndLocation = requestPermissionAndLocation,
            refreshLocation = refreshLocation
        )
    }
}
