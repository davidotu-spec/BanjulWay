package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.utils.LocationUtils
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * Region categories in The Gambia.
 */
enum class GambiaRegion(val displayName: String, val description: String) {
    BANJUL("Banjul City", "Capital island & port administrative area"),
    KANIFING_MUNICIPAL("Kanifing Municipal Area (KM)", "Commercial, educational & residential corridor"),
    KOMBO_COASTAL("Kombo Coastal / Tourism", "Tourism strip, beach resorts & southern corridor")
}

/**
 * Functional category for designated pick-up points.
 */
enum class PickupCategory(val label: String) {
    TRANSIT_GARAGE("Transit Hub / Garage"),
    COMMERCIAL_MARKET("Market & Shopping Hub"),
    HISTORIC_LANDMARK("Landmark & Plaza"),
    BEACH_RESORT("Beach & Resort Hub"),
    EDUCATIONAL_INSTITUTION("University / College Gate"),
    MEDICAL_CENTER("Hospital & Medical Center"),
    GOVERNMENT_QUARTER("Government & Administrative")
}

/**
 * Designated ride pick-up point representation.
 */
data class PickupPoint(
    val id: String,
    val name: String,
    val region: GambiaRegion,
    val zone: String,
    val latitude: Double,
    val longitude: Double,
    val category: PickupCategory,
    val pickupGuidance: String,
    val isHighDemandHub: Boolean = false,
    val popularPickupRank: Int = 1
)

/**
 * Pick-up point coupled with computed distance and walking time from user's current GPS position.
 */
data class PickupPointProximity(
    val pickupPoint: PickupPoint,
    val distanceMeters: Double,
    val formattedDistance: String,
    val walkingTimeMinutes: Int,
    val isWalkable: Boolean
)

/**
 * Result of pick-up point identification from coordinates.
 */
data class IdentifiedPickupInfo(
    val detectedRegion: GambiaRegion,
    val zoneName: String,
    val closestPickupPoint: PickupPoint,
    val distanceMeters: Double,
    val isDirectlyAtHub: Boolean, // < 50m
    val formattedDistance: String,
    val suggestedPickupName: String
)

/**
 * Reactive state holding live GPS tracking and identified pick-up point recommendations.
 */
data class LocationTrackingState(
    val latitude: Double = DEFAULT_BANJUL_LAT,
    val longitude: Double = DEFAULT_BANJUL_LNG,
    val accuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val bearing: Float = 0f,
    val isTrackingActive: Boolean = false,
    val isGpsFixAcquired: Boolean = false,
    val detectedRegion: String = "Banjul City",
    val closestPickupPoint: PickupPoint? = null,
    val distanceToClosestPickupMeters: Double = 0.0,
    val recommendedPickupPoints: List<PickupPointProximity> = emptyList(),
    val lastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null,
    val isFallbackLocation: Boolean = true
) {
    val coordinatesString: String
        get() = String.format(java.util.Locale.US, "%.5f° N, %.5f° W", latitude, -longitude)

    companion object {
        const val DEFAULT_BANJUL_LAT = 13.4549
        const val DEFAULT_BANJUL_LNG = -16.5790
        const val DEFAULT_KANIFING_LAT = 13.4471
        const val DEFAULT_KANIFING_LNG = -16.6791
    }
}

/**
 * LocationTrackingService implements live GPS tracking using Google Play Services Location
 * (FusedLocationProviderClient) and automated Pick-up Point Identification across
 * Banjul and the Kanifing Municipal Area (KM).
 */
object LocationTrackingService {
    private const val TAG = "LocationTrackingService"

    // Curated catalog of verified pick-up hubs across Banjul and Kanifing
    val BANJUL_KANIFING_PICKUP_POINTS: List<PickupPoint> = listOf(
        // === BANJUL REGION PICKUP POINTS ===
        PickupPoint(
            id = "bj_albert_market",
            name = "Albert Market Main Gate, Banjul",
            region = GambiaRegion.BANJUL,
            zone = "Banjul Commercial Center",
            latitude = 13.4533,
            longitude = -16.5746,
            category = PickupCategory.COMMERCIAL_MARKET,
            pickupGuidance = "Wait by the main north entrance taxi shelter opposite Liberation Avenue.",
            isHighDemandHub = true,
            popularPickupRank = 1
        ),
        PickupPoint(
            id = "bj_arch_22",
            name = "Arch 22 Entrance Plaza, Banjul",
            region = GambiaRegion.BANJUL,
            zone = "Independence Drive Corridor",
            latitude = 13.4580,
            longitude = -16.5820,
            category = PickupCategory.HISTORIC_LANDMARK,
            pickupGuidance = "Meet driver at the visitor parking bay right beneath Arch 22.",
            isHighDemandHub = true,
            popularPickupRank = 2
        ),
        PickupPoint(
            id = "bj_ferry_terminal",
            name = "Banjul Ferry Terminal & Port",
            region = GambiaRegion.BANJUL,
            zone = "Banjul Port & Barra Ferry Hub",
            latitude = 13.4505,
            longitude = -16.5710,
            category = PickupCategory.TRANSIT_GARAGE,
            pickupGuidance = "Stand at the passenger exit lane near the port terminal arrival gate.",
            isHighDemandHub = true,
            popularPickupRank = 3
        ),
        PickupPoint(
            id = "bj_mccarthy_square",
            name = "McCarthy Square / Quadrangle, Banjul",
            region = GambiaRegion.BANJUL,
            zone = "Central Administrative District",
            latitude = 13.4550,
            longitude = -16.5780,
            category = PickupCategory.GOVERNMENT_QUARTER,
            pickupGuidance = "Wait along the paved government quadrangle sidewalk near the war memorial.",
            isHighDemandHub = false,
            popularPickupRank = 4
        ),
        PickupPoint(
            id = "bj_efsth_hospital",
            name = "EFSTH Hospital Main Entrance, Banjul",
            region = GambiaRegion.BANJUL,
            zone = "Marina Parade Medical Hub",
            latitude = 13.4565,
            longitude = -16.5762,
            category = PickupCategory.MEDICAL_CENTER,
            pickupGuidance = "Pick-up point is situated directly at the main emergency roundabout drop-off bay.",
            isHighDemandHub = true,
            popularPickupRank = 5
        ),
        PickupPoint(
            id = "bj_king_fahd_mosque",
            name = "King Fahd Mosque / Marina Parade, Banjul",
            region = GambiaRegion.BANJUL,
            zone = "Marina Parade Coastal",
            latitude = 13.4518,
            longitude = -16.5795,
            category = PickupCategory.HISTORIC_LANDMARK,
            pickupGuidance = "Wait by the palm promenade next to the mosque's eastern parking court.",
            isHighDemandHub = false,
            popularPickupRank = 6
        ),
        PickupPoint(
            id = "bj_bund_road",
            name = "Bund Road Highway Entrance, Banjul",
            region = GambiaRegion.BANJUL,
            zone = "Banjul Highway Gateway",
            latitude = 13.4475,
            longitude = -16.5890,
            category = PickupCategory.TRANSIT_GARAGE,
            pickupGuidance = "Stand at the highway entry pull-off zone before the toll bridge.",
            isHighDemandHub = false,
            popularPickupRank = 7
        ),

        // === KANIFING MUNICIPAL AREA (KM) PICKUP POINTS ===
        PickupPoint(
            id = "km_westfield_monument",
            name = "Westfield Monument & Junction, Serrekunda",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Serrekunda Central Transit Hub",
            latitude = 13.4385,
            longitude = -16.6760,
            category = PickupCategory.TRANSIT_GARAGE,
            pickupGuidance = "Wait safely inside the designated ride-hail passenger bay beside the Westfield clock monument.",
            isHighDemandHub = true,
            popularPickupRank = 1
        ),
        PickupPoint(
            id = "km_serekunda_market",
            name = "Serekunda Market / London Corner Hub",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Serekunda Commercial Corridor",
            latitude = 13.4382,
            longitude = -16.6780,
            category = PickupCategory.COMMERCIAL_MARKET,
            pickupGuidance = "Stand by the London Corner taxi rank entrance away from the main pedestrian rush.",
            isHighDemandHub = true,
            popularPickupRank = 2
        ),
        PickupPoint(
            id = "km_kairaba_traffic_lights",
            name = "Kairaba Avenue (Traffic Lights Hub)",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Kairaba Commercial & Banking Strip",
            latitude = 13.4471,
            longitude = -16.6791,
            category = PickupCategory.TRANSIT_GARAGE,
            pickupGuidance = "Meet driver at the commercial bank forecourt opposite the Traffic Lights junction.",
            isHighDemandHub = true,
            popularPickupRank = 3
        ),
        PickupPoint(
            id = "km_utg_kanifing",
            name = "University of The Gambia (UTG) Kanifing",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Kanifing Institutional Area",
            latitude = 13.4452,
            longitude = -16.6713,
            category = PickupCategory.EDUCATIONAL_INSTITUTION,
            pickupGuidance = "Wait by the UTG main security gate on MDI Road.",
            isHighDemandHub = true,
            popularPickupRank = 4
        ),
        PickupPoint(
            id = "km_independence_stadium",
            name = "Independence Stadium Main Gate, Bakau",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Bakau Stadium & Sports District",
            latitude = 13.4722,
            longitude = -16.6690,
            category = PickupCategory.HISTORIC_LANDMARK,
            pickupGuidance = "Stand at the stadium front plaza parking apron near the main entrance arch.",
            isHighDemandHub = true,
            popularPickupRank = 5
        ),
        PickupPoint(
            id = "km_senegambia_strip",
            name = "Senegambia Strip & Kololi Hub",
            region = GambiaRegion.KOMBO_COASTAL,
            zone = "Senegambia Tourist & Hospitality Hub",
            latitude = 13.4420,
            longitude = -16.7110,
            category = PickupCategory.BEACH_RESORT,
            pickupGuidance = "Meet driver at the Senegambia craft village roundabout pickup zone.",
            isHighDemandHub = true,
            popularPickupRank = 6
        ),
        PickupPoint(
            id = "km_kotu_beach",
            name = "Kotu Beach & Craft Market, Kanifing",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Kotu Coastal & Crafts Hub",
            latitude = 13.4610,
            longitude = -16.7020,
            category = PickupCategory.BEACH_RESORT,
            pickupGuidance = "Wait at the craft market entrance beside the Kotu Stream bridge.",
            isHighDemandHub = true,
            popularPickupRank = 7
        ),
        PickupPoint(
            id = "km_pipeline_hub",
            name = "Pipeline / Kairaba Mosque Road",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Pipeline Residential & Embassy Hub",
            latitude = 13.4510,
            longitude = -16.6800,
            category = PickupCategory.TRANSIT_GARAGE,
            pickupGuidance = "Stand outside the Pipeline mosque main gate pull-in bay.",
            isHighDemandHub = false,
            popularPickupRank = 8
        ),
        PickupPoint(
            id = "km_tippa_garage",
            name = "Tippa Garage & Buffer Zone, Serrekunda",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Tallinding / Buffer Zone Nexus",
            latitude = 13.4340,
            longitude = -16.6850,
            category = PickupCategory.TRANSIT_GARAGE,
            pickupGuidance = "Meet driver at the Tippa garage transit platform facing Brikama Highway.",
            isHighDemandHub = true,
            popularPickupRank = 9
        ),
        PickupPoint(
            id = "km_cape_point",
            name = "Bakau Cape Point & Sunbeach Hub",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Cape Point Resort District",
            latitude = 13.4810,
            longitude = -16.6830,
            category = PickupCategory.BEACH_RESORT,
            pickupGuidance = "Wait in front of the Cape Point tourist taxi terminal.",
            isHighDemandHub = false,
            popularPickupRank = 10
        ),
        PickupPoint(
            id = "km_latrikunda_market",
            name = "Latrikunda Sabiji Market Junction",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Latrikunda Coastal Corridor",
            latitude = 13.4280,
            longitude = -16.6750,
            category = PickupCategory.COMMERCIAL_MARKET,
            pickupGuidance = "Stand at the junction opposite the market main entrance.",
            isHighDemandHub = false,
            popularPickupRank = 11
        ),
        PickupPoint(
            id = "km_kanifing_hospital",
            name = "Kanifing General Hospital Gate",
            region = GambiaRegion.KANIFING_MUNICIPAL,
            zone = "Kanifing East Medical Zone",
            latitude = 13.4428,
            longitude = -16.6745,
            category = PickupCategory.MEDICAL_CENTER,
            pickupGuidance = "Wait by the patient drop-off roundabout at the hospital main gate.",
            isHighDemandHub = true,
            popularPickupRank = 12
        ),
        PickupPoint(
            id = "km_brusubi_turntable",
            name = "Brusubi Turntable Roundabout",
            region = GambiaRegion.KOMBO_COASTAL,
            zone = "Brusubi / Sukuta Gateway",
            latitude = 13.4020,
            longitude = -16.7180,
            category = PickupCategory.TRANSIT_GARAGE,
            pickupGuidance = "Stand near the commercial shopping complex parking at the roundabout.",
            isHighDemandHub = true,
            popularPickupRank = 13
        )
    )

    private val _trackingState = MutableStateFlow(
        LocationTrackingState(
            closestPickupPoint = BANJUL_KANIFING_PICKUP_POINTS[0],
            recommendedPickupPoints = computeRecommendations(
                LocationTrackingState.DEFAULT_BANJUL_LAT,
                LocationTrackingState.DEFAULT_BANJUL_LNG
            )
        )
    )
    val trackingState: StateFlow<LocationTrackingState> = _trackingState.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    /**
     * Calculates the great-circle distance in meters between two coordinates using the Haversine formula.
     */
    fun calculateDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }

    /**
     * Formats distance into human-friendly string (e.g. "45 m", "350 m", "1.8 km").
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000.0) {
            "${meters.roundToInt()} m"
        } else {
            String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
        }
    }

    /**
     * Estimates walking time in minutes based on average 4.8 km/h (80 m/min) pedestrian speed.
     */
    fun estimateWalkingMinutes(meters: Double): Int {
        val minutes = (meters / 80.0).roundToInt()
        return max(1, minutes)
    }

    /**
     * Detects region (Banjul vs Kanifing vs Coastal) based on latitude and longitude boundaries.
     */
    fun detectRegion(lat: Double, lng: Double): GambiaRegion {
        return when {
            // Banjul Island is bounded roughly between -16.60W and -16.56W, lat 13.43N to 13.48N
            lng > -16.6200 -> GambiaRegion.BANJUL
            // Coastal strip / Senegambia / Brusubi west of -16.705W
            lng < -16.7050 -> GambiaRegion.KOMBO_COASTAL
            // Central Kanifing / Serekunda / Bakau / Pipeline
            else -> GambiaRegion.KANIFING_MUNICIPAL
        }
    }

    /**
     * Identifies the closest designated pick-up point and surrounding zone details.
     */
    fun identifyPickupPoint(lat: Double, lng: Double): IdentifiedPickupInfo {
        val region = detectRegion(lat, lng)
        val nearest = BANJUL_KANIFING_PICKUP_POINTS.minByOrNull { point ->
            calculateDistanceMeters(lat, lng, point.latitude, point.longitude)
        } ?: BANJUL_KANIFING_PICKUP_POINTS[0]

        val distMeters = calculateDistanceMeters(lat, lng, nearest.latitude, nearest.longitude)
        val isDirectlyAtHub = distMeters <= 60.0

        val suggestedName = if (isDirectlyAtHub) {
            nearest.name
        } else if (distMeters <= 350.0) {
            "Near ${nearest.name} (${formatDistance(distMeters)})"
        } else {
            "${nearest.zone} (Pickup: ${nearest.name})"
        }

        return IdentifiedPickupInfo(
            detectedRegion = region,
            zoneName = nearest.zone,
            closestPickupPoint = nearest,
            distanceMeters = distMeters,
            isDirectlyAtHub = isDirectlyAtHub,
            formattedDistance = formatDistance(distMeters),
            suggestedPickupName = suggestedName
        )
    }

    /**
     * Computes top closest pickup points sorted by distance from current coordinates.
     */
    fun computeRecommendations(lat: Double, lng: Double, limit: Int = 5): List<PickupPointProximity> {
        return BANJUL_KANIFING_PICKUP_POINTS.map { point ->
            val dist = calculateDistanceMeters(lat, lng, point.latitude, point.longitude)
            PickupPointProximity(
                pickupPoint = point,
                distanceMeters = dist,
                formattedDistance = formatDistance(dist),
                walkingTimeMinutes = estimateWalkingMinutes(dist),
                isWalkable = dist <= 600.0
            )
        }.sortedBy { it.distanceMeters }.take(limit)
    }

    /**
     * Starts continuous high-accuracy location tracking using FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun startTracking(context: Context) {
        if (!LocationUtils.hasLocationPermission(context)) {
            Log.w(TAG, "Cannot start tracking: Location permissions not granted.")
            _trackingState.value = _trackingState.value.copy(
                errorMessage = "Location permissions required to track live position in Banjul & Kanifing.",
                isTrackingActive = false
            )
            return
        }

        try {
            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            // Clean up any existing callback
            stopTracking()

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                /* intervalMillis = */ 4000L
            ).apply {
                setMinUpdateIntervalMillis(2000L)
                setMinUpdateDistanceMeters(4f) // Update every 4 meters
                setWaitForAccurateLocation(true)
                setMaxUpdateDelayMillis(8000L)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location: Location? = locationResult.lastLocation
                    if (location != null) {
                        updateTrackingState(location)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        Log.w(TAG, "GPS location currently unavailable. Maintaining last valid pickup fix.")
                    }
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            _trackingState.value = _trackingState.value.copy(
                isTrackingActive = true,
                errorMessage = null
            )
            Log.i(TAG, "High-accuracy FusedLocation tracking initiated successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking: ${e.localizedMessage}", e)
            _trackingState.value = _trackingState.value.copy(
                errorMessage = "Tracking error: ${e.localizedMessage}",
                isTrackingActive = false
            )
        }
    }

    /**
     * Updates internal tracking state from a fresh GPS Location reading.
     */
    private fun updateTrackingState(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val pickupInfo = identifyPickupPoint(lat, lng)
        val recommendations = computeRecommendations(lat, lng, limit = 5)

        _trackingState.value = LocationTrackingState(
            latitude = lat,
            longitude = lng,
            accuracyMeters = location.accuracy,
            speedMps = location.speed,
            bearing = location.bearing,
            isTrackingActive = true,
            isGpsFixAcquired = true,
            detectedRegion = pickupInfo.detectedRegion.displayName,
            closestPickupPoint = pickupInfo.closestPickupPoint,
            distanceToClosestPickupMeters = pickupInfo.distanceMeters,
            recommendedPickupPoints = recommendations,
            lastUpdatedMillis = System.currentTimeMillis(),
            errorMessage = null,
            isFallbackLocation = false
        )

        Log.d(TAG, "Updated position: ($lat, $lng) -> Nearest Hub: ${pickupInfo.closestPickupPoint.name} (${pickupInfo.formattedDistance})")
    }

    /**
     * Stops continuous location updates to preserve battery.
     */
    fun stopTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
            locationCallback = null
            _trackingState.value = _trackingState.value.copy(isTrackingActive = false)
            Log.i(TAG, "FusedLocation tracking paused.")
        }
    }

    /**
     * Obtains a one-shot current location snapshot with immediate pick-up point identification.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocationSnapshot(
        context: Context,
        onResult: (LocationTrackingState) -> Unit
    ) {
        if (!LocationUtils.hasLocationPermission(context)) {
            val fallbackState = LocationTrackingState(
                latitude = LocationTrackingState.DEFAULT_BANJUL_LAT,
                longitude = LocationTrackingState.DEFAULT_BANJUL_LNG,
                isTrackingActive = false,
                isGpsFixAcquired = false,
                detectedRegion = "Banjul City",
                closestPickupPoint = BANJUL_KANIFING_PICKUP_POINTS[0],
                distanceToClosestPickupMeters = 0.0,
                recommendedPickupPoints = computeRecommendations(
                    LocationTrackingState.DEFAULT_BANJUL_LAT,
                    LocationTrackingState.DEFAULT_BANJUL_LNG
                ),
                errorMessage = "Location permission not granted. Defaulting to Banjul City Center.",
                isFallbackLocation = true
            )
            _trackingState.value = fallbackState
            onResult(fallbackState)
            return
        }

        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        updateTrackingState(location)
                        onResult(_trackingState.value)
                    } else {
                        // Attempt last known location fallback
                        client.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                updateTrackingState(lastLoc)
                                onResult(_trackingState.value)
                            } else {
                                onResult(_trackingState.value)
                            }
                        }.addOnFailureListener {
                            onResult(_trackingState.value)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "getCurrentLocation failed: ${e.localizedMessage}")
                    onResult(_trackingState.value)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying snapshot: ${e.localizedMessage}", e)
            onResult(_trackingState.value)
        }
    }
}
