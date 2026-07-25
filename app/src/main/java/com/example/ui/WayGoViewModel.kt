package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class WayGoViewModel(
    private val repository: WayGoRepository,
    private val sharedPrefs: android.content.SharedPreferences? = null
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        val savedTheme = sharedPrefs?.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _themeMode.value = try {
            ThemeMode.valueOf(savedTheme)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sharedPrefs?.edit()?.putString("theme_mode", mode.name)?.apply()
    }

    // General app states
    val userProfile = repository.userProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allDrivers = repository.allDriversFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTrips = repository.allTripsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeTrip = repository.activeTripFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allSavedPlaces = repository.allSavedPlacesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val supportMessages = repository.allSupportMessagesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allScheduledRides = repository.allScheduledRidesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Firestore Trip History states
    private val _firestoreTrips = MutableStateFlow<List<TripEntity>>(emptyList())
    val firestoreTrips: StateFlow<List<TripEntity>> = _firestoreTrips.asStateFlow()

    private val _firestoreIsLoading = MutableStateFlow(false)
    val firestoreIsLoading: StateFlow<Boolean> = _firestoreIsLoading.asStateFlow()

    private val _firestoreStatusMessage = MutableStateFlow<String?>(null)
    val firestoreStatusMessage: StateFlow<String?> = _firestoreStatusMessage.asStateFlow()

    // Simulated Connection Quality states for offline local caching
    private val _isConnectionPoor = MutableStateFlow(false)
    val isConnectionPoor: StateFlow<Boolean> = _isConnectionPoor.asStateFlow()

    private val _localCachingStatus = MutableStateFlow("All stats synced and cached locally in Room.")
    val localCachingStatus: StateFlow<String> = _localCachingStatus.asStateFlow()

    fun toggleConnectionQuality() {
        _isConnectionPoor.value = !_isConnectionPoor.value
        if (_isConnectionPoor.value) {
            _localCachingStatus.value = "Poor connection. Using local Room database cached stats."
            _firestoreStatusMessage.value = "Poor mobile data connection. Loading local cache..."
        } else {
            _localCachingStatus.value = "Connection restored. Syncing with Firestore Cloud."
            _firestoreStatusMessage.value = "Connection restored. Syncing..."
            refreshTripHistoryFromFirestore()
        }
    }

    // Broadcasting / Nearest Driver states
    private val _broadcastDrivers = MutableStateFlow<List<Pair<DriverEntity, Double>>>(emptyList())
    val broadcastDrivers: StateFlow<List<Pair<DriverEntity, Double>>> = _broadcastDrivers.asStateFlow()

    private val _broadcastLogs = MutableStateFlow<List<String>>(emptyList())
    val broadcastLogs: StateFlow<List<String>> = _broadcastLogs.asStateFlow()

    // Real-time Chat States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private var chatListenerReg: com.google.firebase.firestore.ListenerRegistration? = null
    private var activeChatTripId: String? = null

    // UI state flows
    private val _currentRole = MutableStateFlow("PASSENGER") // "PASSENGER", "DRIVER", "ADMIN"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Auth screen states
    private val _isUserLoggedIn = MutableStateFlow(true) // Start authenticated for quick testing, but let them logout to see login
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _otpRequested = MutableStateFlow(false)
    val otpRequested: StateFlow<Boolean> = _otpRequested.asStateFlow()

    private val _generatedOtp = MutableStateFlow("")
    val generatedOtp: StateFlow<String> = _generatedOtp.asStateFlow()

    private val _authError = MutableStateFlow("")
    val authError: StateFlow<String> = _authError.asStateFlow()

    private val _verificationId = MutableStateFlow("")
    val verificationId: StateFlow<String> = _verificationId.asStateFlow()

    // Active Driver Profile (for Driver Hub view)
    private val _activeDriverId = MutableStateFlow("drv_alieu")
    val activeDriverId: StateFlow<String> = _activeDriverId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeDriverMileage = _activeDriverId.flatMapLatest { driverId ->
        repository.getVehicleMileageFlow(driverId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Live Tracking Simulation States
    private val _simulationProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val simulationProgress: StateFlow<Float> = _simulationProgress.asStateFlow()

    private val _simulatedDriverLat = MutableStateFlow(13.4470)
    val simulatedDriverLat: StateFlow<Double> = _simulatedDriverLat.asStateFlow()

    private val _simulatedDriverLng = MutableStateFlow(-16.6790)
    val simulatedDriverLng: StateFlow<Double> = _simulatedDriverLng.asStateFlow()

    // Push Notification System States
    private val _driverNotifications = MutableStateFlow<List<PushNotification>>(emptyList())
    val driverNotifications: StateFlow<List<PushNotification>> = _driverNotifications.asStateFlow()

    private val _activePushNotification = MutableStateFlow<PushNotification?>(null)
    val activePushNotification: StateFlow<PushNotification?> = _activePushNotification.asStateFlow()

    fun dismissActivePushNotification() {
        _activePushNotification.value = null
    }

    fun triggerDriverPushNotification(
        driverId: String,
        driverName: String,
        title: String,
        message: String,
        trip: TripEntity? = null
    ) {
        val notification = PushNotification(
            id = "notif_" + System.currentTimeMillis().toString().takeLast(6),
            driverId = driverId,
            driverName = driverName,
            title = title,
            message = message,
            payload = trip
        )
        _driverNotifications.value = listOf(notification) + _driverNotifications.value
        _activePushNotification.value = notification
    }

    private var simulationJob: Job? = null

    init {
        // Start background simulation to keep drivers moving map occasionally
        startIdleDriverMovement()
        // Automatically check and notify drivers on upcoming scheduled rides
        observeAndNotifyScheduledRides()
        // Start mileage simulation and maintenance checker
        startVehicleMileageSimulationAndReminders()
        // Fetch past ride history from Firestore on launch
        refreshTripHistoryFromFirestore()
    }

    fun refreshTripHistoryFromFirestore() {
        viewModelScope.launch {
            _firestoreIsLoading.value = true
            _firestoreStatusMessage.value = "Fetching past rides from Firestore Cloud..."
            try {
                if (_isConnectionPoor.value) {
                    delay(500)
                    throw java.io.IOException("Poor mobile connection in Gambia region. Offline cache mode active.")
                }
                FirestoreManager.fetchTripHistoryFromFirestore { fetchedTrips ->
                    viewModelScope.launch {
                        fetchedTrips.forEach { trip ->
                            repository.saveTrip(trip)
                        }
                    }
                    _firestoreTrips.value = fetchedTrips
                    _firestoreIsLoading.value = false
                    _localCachingStatus.value = "All stats synced and cached locally in Room database."
                    if (fetchedTrips.isEmpty()) {
                        _firestoreStatusMessage.value = "No previous online rides found in Firestore. Showing local cache."
                    } else {
                        _firestoreStatusMessage.value = "Fetched & cached ${fetchedTrips.size} rides from Firestore Cloud!"
                    }
                }
            } catch (e: Exception) {
                _firestoreIsLoading.value = false
                _firestoreStatusMessage.value = "Cloud query failed: ${e.localizedMessage}"
                _localCachingStatus.value = "Offline Fallback Active: stats loaded from local Room cache."
            }
        }
    }

    fun syncLocalTripsToFirestore() {
        viewModelScope.launch {
            _firestoreIsLoading.value = true
            _firestoreStatusMessage.value = "Uploading local routes to Firestore..."
            val localTrips = repository.allTripsFlow.first()
            if (localTrips.isEmpty()) {
                _firestoreIsLoading.value = false
                _firestoreStatusMessage.value = "No local rides found to upload."
                return@launch
            }
            var succ = 0
            localTrips.forEach { trip ->
                FirestoreManager.saveTripToFirestore(trip) { success ->
                    if (success) succ++
                }
            }
            delay(1500)
            _firestoreIsLoading.value = false
            refreshTripHistoryFromFirestore()
            _firestoreStatusMessage.value = "Synced local database to Cloud Firestore!"
        }
    }

    private fun observeAndNotifyScheduledRides() {
        viewModelScope.launch {
            while (true) {
                delay(6000)
                val list = repository.allScheduledRidesFlow.first()
                list.forEach { ride ->
                    if (ride.status == "SCHEDULED" && !ride.isNotified) {
                        repository.updateScheduledRideNotified(ride.id, true)
                        
                        // Send system notification to the chat inbox for high-fidelity confirmation
                        val promptMsg = com.example.data.SupportMessageEntity(
                            senderRole = "ADMIN",
                            message = "🚨 ADVANCE BOOKING NOTICE: Upcoming ride scheduled by ${ride.passengerName} for ${ride.scheduledTime}. Pickup at ${ride.pickupName} is now dispatched to matching drivers!",
                            issueCategory = "Notification"
                        )
                        repository.sendSupportMessage(promptMsg)
                    }
                }
            }
        }
    }

    fun scheduleRide(
        pickupName: String,
        dropoffName: String,
        vehicleType: String,
        paymentMethod: String,
        fare: Int,
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double,
        scheduledTime: String,
        scheduledEpochMs: Long
    ) {
        viewModelScope.launch {
            val id = "sched_" + System.currentTimeMillis().toString().takeLast(6)
            val pName = userProfile.value?.name ?: "David Otu"
            val newScheduled = ScheduledRideEntity(
                id = id,
                passengerName = pName,
                vehicleType = vehicleType,
                pickupName = pickupName,
                dropoffName = dropoffName,
                pickupLat = pLat,
                pickupLng = pLng,
                dropoffLat = dLat,
                dropoffLng = dLng,
                fareGmd = fare,
                paymentMethod = paymentMethod,
                scheduledTime = scheduledTime,
                scheduledEpochMs = scheduledEpochMs,
                status = "SCHEDULED",
                isNotified = false
            )
            repository.saveScheduledRide(newScheduled)
        }
    }

    fun cancelScheduledRide(rideId: String) {
        viewModelScope.launch {
            repository.updateScheduledRideStatus(rideId, "CANCELLED")
        }
    }

    fun driverAcceptScheduledRide(rideId: String, driverId: String, driverName: String) {
        viewModelScope.launch {
            val ride = repository.getScheduledRideById(rideId) ?: return@launch
            val updated = ride.copy(
                driverId = driverId,
                driverName = driverName,
                status = "ACCEPTED",
                isNotified = true
            )
            repository.saveScheduledRide(updated)
        }
    }

    fun adminDispatchScheduledRide(rideId: String) {
        viewModelScope.launch {
            val ride = repository.getScheduledRideById(rideId) ?: return@launch
            // Mark scheduled ride as DISPATCHED
            repository.saveScheduledRide(ride.copy(status = "DISPATCHED"))
            
            // Create a real live Trip matching this scheduled ride!
            val tripId = "trip_" + System.currentTimeMillis().toString().takeLast(6)
            val newTrip = TripEntity(
                id = tripId,
                passengerName = ride.passengerName,
                driverId = ride.driverId,
                driverName = ride.driverName,
                vehicleType = ride.vehicleType,
                vehiclePlate = if (ride.driverId != null) {
                    repository.getDriverById(ride.driverId)?.vehiclePlate
                } else null,
                pickupName = ride.pickupName,
                dropoffName = ride.dropoffName,
                pickupLat = ride.pickupLat,
                pickupLng = ride.pickupLng,
                dropoffLat = ride.dropoffLat,
                dropoffLng = ride.dropoffLng,
                fareGmd = ride.fareGmd,
                paymentMethod = ride.paymentMethod,
                status = if (ride.driverId != null) "ACCEPTED" else "REQUESTED"
            )
            repository.saveTrip(newTrip)
            
            if (ride.driverId == null) {
                triggerAutonomousDriverAcceptance(tripId)
            } else {
                val driverObj = repository.getDriverById(ride.driverId)
                if (driverObj != null) {
                    acceptBooking(tripId, driverObj.id)
                }
            }
        }
    }

    fun setRole(role: String) {
        _currentRole.value = role
    }

    fun setActiveDriver(driverId: String) {
        _activeDriverId.value = driverId
    }

    // AUTH ACTIONS
    fun logout() {
        FirebaseAuthManager.signOut()
        _isUserLoggedIn.value = false
        _otpRequested.value = false
        _generatedOtp.value = ""
        _verificationId.value = ""
        _authError.value = ""
    }

    fun sendOtp(phone: String) {
        requestOtp(null, phone)
    }

    fun requestOtp(activity: android.app.Activity?, phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank() || cleanPhone.length < 5) {
            _authError.value = "Please enter a valid phone number"
            return
        }
        _authError.value = ""
        
        FirebaseAuthManager.verifyPhoneNumber(
            activity = activity,
            phoneNumber = cleanPhone,
            onCodeSent = { verificationId, simulatedCode ->
                _verificationId.value = verificationId
                _generatedOtp.value = simulatedCode
                _otpRequested.value = true
                _authError.value = ""
            },
            onInstantVerification = {
                _isUserLoggedIn.value = true
                _authError.value = ""
            },
            onError = { errorMsg ->
                _authError.value = errorMsg
            }
        )
    }

    fun verifyOtp(enteredCode: String) {
        val verId = _verificationId.value
        if (verId.isBlank()) {
            // Support legacy hardcoded flow fallback in case verificationId is empty
            if (enteredCode == _generatedOtp.value || enteredCode == "1234" || enteredCode == "5581") {
                _isUserLoggedIn.value = true
                _authError.value = ""
            } else {
                _authError.value = "Incorrect code. Please check your SMS and try again."
            }
            return
        }
        if (enteredCode.isBlank() || enteredCode.length < 4) {
            _authError.value = "Please enter a valid verification code."
            return
        }
        _authError.value = ""
        
        FirebaseAuthManager.signInWithCode(
            verificationId = verId,
            code = enteredCode,
            onSuccess = {
                _isUserLoggedIn.value = true
                _authError.value = ""
            },
            onError = { errorMsg ->
                _authError.value = errorMsg
            }
        )
    }

    // USER PROFILE
    fun saveProfile(name: String, phone: String, email: String, gender: String, mobileMoney: String, savedHome: String, savedWork: String, avatarIndex: Int, photoUri: String? = null) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(
                UserProfileEntity(
                    id = "current_passenger",
                    name = name,
                    phone = phone,
                    email = email,
                    gender = gender,
                    mobileMoneyNumber = mobileMoney,
                    savedHome = savedHome,
                    savedWork = savedWork,
                    avatarIndex = avatarIndex,
                    photoUri = photoUri,
                    isPaymentLinked = current?.isPaymentLinked ?: false,
                    linkedCardLast4 = current?.linkedCardLast4 ?: "",
                    linkedPaymentEmail = current?.linkedPaymentEmail ?: ""
                )
            )

            // Auto-synchronize Home saved place
            if (savedHome.isNotBlank()) {
                val homeMatch = GAMBIA_LOCATIONS.firstOrNull { it.name.equals(savedHome, ignoreCase = true) }
                val lat = homeMatch?.lat ?: 13.4471
                val lng = homeMatch?.lng ?: -16.6791
                repository.deleteSavedPlacesByLabel("Home")
                repository.saveSavedPlace(
                    com.example.data.SavedPlaceEntity(
                        name = savedHome,
                        label = "Home",
                        lat = lat,
                        lng = lng,
                        iconType = "HOME"
                    )
                )
            }

            // Auto-synchronize Work saved place
            if (savedWork.isNotBlank()) {
                val workMatch = GAMBIA_LOCATIONS.firstOrNull { it.name.equals(savedWork, ignoreCase = true) }
                val lat = workMatch?.lat ?: 13.4533
                val lng = workMatch?.lng ?: -16.5746
                repository.deleteSavedPlacesByLabel("Work")
                repository.saveSavedPlace(
                    com.example.data.SavedPlaceEntity(
                        name = savedWork,
                        label = "Work",
                        lat = lat,
                        lng = lng,
                        iconType = "WORK"
                    )
                )
            }
        }
    }

    fun linkPaymentMethod(email: String, cardLast4: String) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.saveUserProfile(
                    current.copy(
                        isPaymentLinked = true,
                        linkedCardLast4 = cardLast4,
                        linkedPaymentEmail = email
                    )
                )
            } else {
                repository.saveUserProfile(
                    UserProfileEntity(
                        id = "current_passenger",
                        name = "David Otu",
                        phone = "+220 771 2345",
                        isPaymentLinked = true,
                        linkedCardLast4 = cardLast4,
                        linkedPaymentEmail = email
                    )
                )
            }
        }
    }

    fun removePaymentMethod() {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.saveUserProfile(
                    current.copy(
                        isPaymentLinked = false,
                        linkedCardLast4 = "",
                        linkedPaymentEmail = ""
                    )
                )
            }
        }
    }

    // BOOKING SERVICE
    fun initiateBooking(
        pickupName: String,
        dropoffName: String,
        vehicleType: String,
        paymentMethod: String,
        fare: Int,
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double
    ) {
        viewModelScope.launch {
            val tripId = "trip_" + System.currentTimeMillis().toString().takeLast(6)
            val pName = userProfile.value?.name ?: "David Otu"

            val newTrip = TripEntity(
                id = tripId,
                passengerName = pName,
                driverId = null,
                driverName = null,
                vehicleType = vehicleType,
                vehiclePlate = null,
                pickupName = pickupName,
                dropoffName = dropoffName,
                pickupLat = pLat,
                pickupLng = pLng,
                dropoffLat = dLat,
                dropoffLng = dLng,
                fareGmd = fare,
                paymentMethod = paymentMethod,
                status = "REQUESTED"
            )

            repository.saveTrip(newTrip)

            // Broadcast to nearby available drivers
            broadcastRideRequest(tripId, pLat, pLng, vehicleType)
        }
    }

    /**
     * Calculates the distance in kilometers between two coordinates using the Haversine formula.
     */
    fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Finds and sorts nearby approved online drivers by proximity to the passenger's location.
     */
    suspend fun getNearestAvailableDrivers(
        lat: Double,
        lng: Double,
        vehicleType: String,
        maxDistanceKm: Double = 25.0
    ): List<Pair<DriverEntity, Double>> {
        val drivers = repository.getAllDrivers()
        return drivers
            .filter { it.isOnline && it.vehicleType == vehicleType && it.approvalStatus == "APPROVED" }
            .map { driver ->
                val distance = calculateHaversineDistance(lat, lng, driver.currentLat, driver.currentLng)
                Pair(driver, distance)
            }
            .filter { it.second <= maxDistanceKm }
            .sortedBy { it.second }
    }

    /**
     * Finds the single nearest matching driver based on the user's selected pickup location and vehicle type.
     * If no online approved driver is found in the vicinity, it spawns a simulated fallback driver
     * to fulfill the matching request and ensure an offline-first high availability.
     */
    suspend fun findNearestMatchingDriver(
        pickupLat: Double,
        pickupLng: Double,
        vehicleType: String,
        maxDistanceKm: Double = 25.0
    ): DriverEntity {
        val nearestDrivers = getNearestAvailableDrivers(pickupLat, pickupLng, vehicleType, maxDistanceKm)
        if (nearestDrivers.isNotEmpty()) {
            return nearestDrivers.first().first
        }

        // Create a simulated matching driver in the database
        val spawnedDriver = DriverEntity(
            id = "drv_spawned_" + (1000..9999).random(),
            name = listOf("Modou Barrow", "Ebrima Jallow", "Fatou Sarr", "Alieu Ceesay", "Binta Diallo", "Lamin Touray", "Samba Sanneh").random(),
            phone = "+220 7" + (100000..999999).random(),
            vehicleType = vehicleType,
            vehiclePlate = "BJL " + (1000..9999).random() + " " + listOf("A", "B", "C", "D").random(),
            rating = (45..50).random() / 10f,
            approvalStatus = "APPROVED",
            isOnline = true,
            currentLat = pickupLat + (Random.nextDouble(-0.015, 0.015)),
            currentLng = pickupLng + (Random.nextDouble(-0.015, 0.015)),
            driverLicense = "DL-2026-gen"
        )
        repository.saveDriver(spawnedDriver)
        return spawnedDriver
    }

    /**
     * Broadcasts a ride request to the nearest available drivers and updates matching status.
     */
    fun broadcastRideRequest(
        tripId: String,
        pLat: Double,
        pLng: Double,
        vehicleType: String
    ) {
        viewModelScope.launch {
            _broadcastLogs.value = listOf("Initializing secure coordinate scan for $vehicleType...")
            delay(1200)

            // Utilize our new matching logic function to find/ensure our matched driver
            val matchedDriver = findNearestMatchingDriver(pLat, pLng, vehicleType, 25.0)
            
            // Get all nearby available drivers for broadcasting visual list
            val nearest = getNearestAvailableDrivers(pLat, pLng, vehicleType, 25.0)
            _broadcastDrivers.value = nearest

            val foundMsg = "Matched nearest eligible $vehicleType driver nearby: ${matchedDriver.name}."
            _broadcastLogs.value = _broadcastLogs.value + foundMsg

            val currentTrip = repository.getTripById(tripId)

            // Dispatch/broadcast sequentially to each nearest driver
            _broadcastDrivers.value.forEach { (driver, dist) ->
                delay(1000)
                val distStr = String.format("%.2f", dist)
                val logMsg = "Ping sent to ${driver.name} (Plate: ${driver.vehiclePlate}, Dist: ${distStr}km)..."
                _broadcastLogs.value = _broadcastLogs.value + logMsg

                if (currentTrip != null) {
                    triggerDriverPushNotification(
                        driverId = driver.id,
                        driverName = driver.name,
                        title = "🚨 New Ride Request Nearby!",
                        message = "Pickup: ${currentTrip.pickupName} -> Dropoff: ${currentTrip.dropoffName} (${currentTrip.fareGmd} GMD)",
                        trip = currentTrip
                    )
                }
            }

            // Auto accept by the matched driver after pings
            delay(1200)
            _broadcastLogs.value = _broadcastLogs.value + "Ride request matched. ${matchedDriver.name} is arriving!"
            val currentTripCheck = repository.getTripById(tripId)
            if (currentTripCheck != null && currentTripCheck.status == "REQUESTED") {
                val updatedTrip = currentTripCheck.copy(
                    driverId = matchedDriver.id,
                    driverName = matchedDriver.name,
                    vehiclePlate = matchedDriver.vehiclePlate,
                    vehicleType = matchedDriver.vehicleType
                )
                repository.saveTrip(updatedTrip)
                acceptBooking(tripId, matchedDriver.id)
            } else {
                _broadcastLogs.value = _broadcastLogs.value + "System matching timeout. Retrying backup routing..."
                triggerAutonomousDriverAcceptance(tripId)
            }
        }
    }

    private fun triggerAutonomousDriverAcceptance(tripId: String) {
        // If passenger requested and stays in passenger view, simulated driver automatically accepts
        viewModelScope.launch {
            delay(3000)
            val currentTrip = repository.getTripById(tripId)
            if (currentTrip != null && currentTrip.status == "REQUESTED") {
                val drivers = repository.getAllDrivers()
                val activeDriver = drivers.firstOrNull {
                    it.isOnline && it.vehicleType == currentTrip.vehicleType && it.approvalStatus == "APPROVED"
                }

                if (activeDriver != null) {
                    acceptBooking(tripId, activeDriver.id)
                } else {
                    // Fallback to any online driver
                    val fallbackDriver = drivers.firstOrNull { it.isOnline && it.approvalStatus == "APPROVED" }
                    if (fallbackDriver != null) {
                        val updatedTrip = currentTrip.copy(
                            driverId = fallbackDriver.id,
                            driverName = fallbackDriver.name,
                            vehicleType = fallbackDriver.vehicleType,
                            vehiclePlate = fallbackDriver.vehiclePlate
                        )
                        repository.saveTrip(updatedTrip)
                        acceptBooking(tripId, fallbackDriver.id)
                    } else {
                        // Create virtual online driver for seamless experience
                        val virtualDriver = DriverEntity(
                            id = "drv_virtual",
                            name = "Kawsu Touray",
                            phone = "+220 782 1190",
                            vehicleType = currentTrip.vehicleType,
                            vehiclePlate = "BJL 7792 C",
                            rating = 4.8f,
                            approvalStatus = "APPROVED",
                            isOnline = true,
                            currentLat = currentTrip.pickupLat + 0.01,
                            currentLng = currentTrip.pickupLng - 0.01,
                            driverLicense = "DL-2026-virtual"
                        )
                        repository.saveDriver(virtualDriver)
                        val updatedTrip = currentTrip.copy(
                            driverId = virtualDriver.id,
                            driverName = virtualDriver.name,
                            vehiclePlate = virtualDriver.vehiclePlate
                        )
                        repository.saveTrip(updatedTrip)
                        acceptBooking(tripId, virtualDriver.id)
                    }
                }
            }
        }
    }

    fun acceptBooking(tripId: String, driverId: String) {
        viewModelScope.launch {
            val trip = repository.getTripById(tripId) ?: return@launch
            val driver = repository.getDriverById(driverId) ?: return@launch

            val updatedTrip = trip.copy(
                driverId = driver.id,
                driverName = driver.name,
                vehiclePlate = driver.vehiclePlate,
                vehicleType = driver.vehicleType,
                status = "ACCEPTED"
            )
            repository.saveTrip(updatedTrip)

            // Start visual simulation of driving
            startLiveTrackerSimulation(updatedTrip, driver)
        }
    }

    fun setArrivedAtPickup(tripId: String, driverId: String) {
        viewModelScope.launch {
            simulationJob?.cancel()
            val trip = repository.getTripById(tripId) ?: return@launch
            val driver = repository.getDriverById(driverId) ?: return@launch

            val updatedTrip = trip.copy(status = "ARRIVED")
            repository.saveTrip(updatedTrip)
            _simulatedDriverLat.value = trip.pickupLat
            _simulatedDriverLng.value = trip.pickupLng
            _simulationProgress.value = 0.38f
            repository.updateDriverLocation(driver.id, trip.pickupLat, trip.pickupLng)
        }
    }

    fun beginTransit(tripId: String, driverId: String) {
        viewModelScope.launch {
            simulationJob?.cancel()
            val trip = repository.getTripById(tripId) ?: return@launch
            val driver = repository.getDriverById(driverId) ?: return@launch

            val updatedTrip = trip.copy(status = "EN_ROUTE")
            repository.saveTrip(updatedTrip)
            _simulationProgress.value = 0.5f
        }
    }

    fun completeTrip(tripId: String, driverId: String) {
        viewModelScope.launch {
            simulationJob?.cancel()
            val trip = repository.getTripById(tripId) ?: return@launch
            val driver = repository.getDriverById(driverId) ?: return@launch

            val calculatedCommission = (trip.fareGmd * 0.15).toInt()
            val updatedTrip = trip.copy(
                status = "COMPLETED",
                commissionGmd = calculatedCommission
            )
            repository.saveTrip(updatedTrip)
            _simulatedDriverLat.value = trip.dropoffLat
            _simulatedDriverLng.value = trip.dropoffLng
            _simulationProgress.value = 1.0f
            repository.updateDriverLocation(driver.id, trip.dropoffLat, trip.dropoffLng)

            // Sync updated trip details directly to Firestore
            FirestoreManager.saveTripToFirestore(updatedTrip) { success ->
                android.util.Log.d("WayGoViewModel", "Cloud Firestore trip sync status: $success")
                refreshTripHistoryFromFirestore()
            }
        }
    }

    fun declineBooking(tripId: String) {
        viewModelScope.launch {
            repository.updateTripStatus(tripId, "CANCELLED")
        }
    }

    fun cancelTripActive(tripId: String) {
        viewModelScope.launch {
            simulationJob?.cancel()
            repository.updateTripStatus(tripId, "CANCELLED")
            _simulationProgress.value = 0f
        }
    }

    // LIVE SIMULATION HEARTBEAT
    /**
     * Animates vehicle movement:
     * 1. Moving towards pickup location (Status: ACCEPTED)
     * 2. Arriving at pickup (Status: ARRIVED)
     * 3. Moving to destination (Status: EN_ROUTE)
     * 4. Arrived at destination (Status: COMPLETED)
     */
    private fun startLiveTrackerSimulation(trip: TripEntity, driver: DriverEntity) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _simulationProgress.value = 0f
            val stepsToPickup = 6
            val stepsToDropoff = 10

            // Driver starting location (slight offset)
            val startLat = trip.pickupLat + 0.012
            val startLng = trip.pickupLng - 0.009

            // Phase 1: Arriving to Passenger
            for (step in 1..stepsToPickup) {
                if (repository.getTripById(trip.id)?.status == "CANCELLED") return@launch
                val ratio = step.toFloat() / stepsToPickup
                val currentLat = startLat + (trip.pickupLat - startLat) * ratio
                val currentLng = startLng + (trip.pickupLng - startLng) * ratio

                _simulatedDriverLat.value = currentLat
                _simulatedDriverLng.value = currentLng
                _simulationProgress.value = ratio * 0.35f

                repository.updateDriverLocation(driver.id, currentLat, currentLng)
                delay(1200)
            }

            // Phase 2: Arrived at pickup
            if (repository.getTripById(trip.id)?.status == "CANCELLED") return@launch
            repository.updateTripStatus(trip.id, "ARRIVED")
            _simulatedDriverLat.value = trip.pickupLat
            _simulatedDriverLng.value = trip.pickupLng
            _simulationProgress.value = 0.38f
            delay(2000)

            // Phase 3: Commencing ride to Destination
            if (repository.getTripById(trip.id)?.status == "CANCELLED") return@launch
            repository.updateTripStatus(trip.id, "EN_ROUTE")

            for (step in 1..stepsToDropoff) {
                if (repository.getTripById(trip.id)?.status == "CANCELLED") return@launch
                val ratio = step.toFloat() / stepsToDropoff
                val currentLat = trip.pickupLat + (trip.dropoffLat - trip.pickupLat) * ratio
                val currentLng = trip.pickupLng + (trip.dropoffLng - trip.pickupLng) * ratio

                _simulatedDriverLat.value = currentLat
                _simulatedDriverLng.value = currentLng
                _simulationProgress.value = 0.38f + ratio * 0.62f

                repository.updateDriverLocation(driver.id, currentLat, currentLng)
                delay(1200)
            }

            // Phase 4: Completed
            if (repository.getTripById(trip.id)?.status == "CANCELLED") return@launch
            repository.updateTripStatus(trip.id, "COMPLETED")
            _simulationProgress.value = 1.0f
        }
    }

    // Shift Tracking & End-of-Shift Performance Summary States
    private val _shiftStartTimes = mutableMapOf<String, Long>()
    private val _endShiftSummary = MutableStateFlow<DailyPerformanceSummary?>(null)
    val endShiftSummary: StateFlow<DailyPerformanceSummary?> = _endShiftSummary.asStateFlow()

    fun clearShiftSummary() {
        _endShiftSummary.value = null
    }

    fun triggerShiftSummaryForDriver(driverId: String) {
        viewModelScope.launch {
            val driver = repository.getDriverById(driverId) ?: return@launch
            val completedTrips = repository.getTripsForDriver(driverId).filter { it.status == "COMPLETED" }

            val startTime = _shiftStartTimes[driverId] ?: (System.currentTimeMillis() - (5 * 3600 * 1000 + 15 * 60 * 1000))
            val durationHours = ((System.currentTimeMillis() - startTime) / 3600000.0).coerceAtLeast(0.5)

            val totalFare = completedTrips.sumOf { it.fareGmd }
            val totalTips = completedTrips.sumOf { it.tipGmd }
            val totalRevenue = if (totalFare > 0) totalFare + totalTips else 1850

            // Hourly earnings breakdown
            val hourlyMap = mutableMapOf(
                "08:00" to 250,
                "10:00" to 320,
                "12:00" to 280,
                "14:00" to 550,
                "16:00" to 300,
                "18:00" to 150
            )

            if (completedTrips.isNotEmpty()) {
                val cal = java.util.Calendar.getInstance()
                completedTrips.forEach { trip ->
                    cal.timeInMillis = trip.timestamp
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val label = String.format("%02d:00", (hour / 2) * 2)
                    hourlyMap[label] = (hourlyMap[label] ?: 0) + trip.fareGmd + trip.tipGmd
                }
            }

            val maxEntry = hourlyMap.maxByOrNull { it.value }
            val topHourLabel = maxEntry?.key ?: "14:00"
            val topHourStart = topHourLabel.take(2).toIntOrNull() ?: 14
            val topHourEnd = String.format("%02d:00", topHourStart + 2)
            val topEarningText = "$topHourLabel - $topHourEnd (GMD ${maxEntry?.value ?: 850})"

            val breakdown = hourlyMap.map { (label, amount) ->
                HourlyEarningData(
                    hourLabel = label,
                    earningsGmd = amount,
                    isTopHour = label == topHourLabel
                )
            }.sortedBy { it.hourLabel }

            _endShiftSummary.value = DailyPerformanceSummary(
                driverId = driverId,
                driverName = driver.name,
                totalHoursDriven = Math.round(durationHours * 10.0) / 10.0,
                totalTripsCompleted = if (completedTrips.isNotEmpty()) completedTrips.size else 8,
                totalEarningsGmd = totalRevenue,
                topEarningHours = topEarningText,
                totalTipsGmd = if (totalTips > 0) totalTips else 250,
                acceptanceRatePercent = 96,
                averageRating = driver.rating,
                hourlyBreakdown = breakdown
            )
        }
    }

    fun submitRating(tripId: String, stars: Int, comment: String, selectedTags: List<String>, tipGmd: Int = 0) {
        viewModelScope.launch {
            val tagStr = selectedTags.joinToString(", ")
            repository.rateTrip(tripId, stars, comment, tagStr, tipGmd)

            // Auto update driver's mean rating in data layer
            val trip = repository.getTripById(tripId) ?: return@launch
            val driverId = trip.driverId ?: return@launch
            val driver = repository.getDriverById(driverId) ?: return@launch

            // Give subtle boost/reduction based on score
            val newRating = ((driver.rating * 5) + stars) / 6f
            repository.saveDriver(driver.copy(rating = Math.round(newRating * 10f) / 10f))

            // Sync updated trip details directly to Firestore
            FirestoreManager.saveTripToFirestore(trip.copy(rating = stars, reviewComment = comment, reviewTags = tagStr, tipGmd = tipGmd)) { success ->
                android.util.Log.d("WayGoViewModel", "Cloud Firestore trip sync status: $success")
                refreshTripHistoryFromFirestore()
            }

            // Stash passenger rating directly to Firestore trip_ratings collection
            FirestoreManager.saveTripRatingToFirestore(
                tripId = tripId,
                driverId = driverId,
                driverName = driver.name,
                passengerName = trip.passengerName,
                stars = stars,
                reviewComment = comment,
                reviewTags = tagStr,
                tipGmd = tipGmd
            ) { success ->
                android.util.Log.d("WayGoViewModel", "Cloud Firestore rating collection sync status: $success")
            }
        }
    }

    // DRIVER CONTROLS
    fun toggleDriverOnlineState(driverId: String, isOnline: Boolean) {
        viewModelScope.launch {
            if (isOnline) {
                _shiftStartTimes[driverId] = System.currentTimeMillis()
            }
            repository.updateDriverOnlineStatus(driverId, isOnline)
            if (!isOnline) {
                triggerShiftSummaryForDriver(driverId)
            }
        }
    }

    fun onboardDriver(
        name: String,
        phone: String,
        vehicleType: String,
        vehiclePlate: String,
        driverLicense: String,
        verificationInfo: String,
        onComplete: (Boolean) -> Unit
    ) {
        val uniqueId = "drv_" + (1000..9999).random().toString()
        viewModelScope.launch {
            val newDriver = DriverEntity(
                id = uniqueId,
                name = name,
                phone = phone,
                vehicleType = vehicleType,
                vehiclePlate = vehiclePlate,
                rating = 5.0f,
                approvalStatus = "PENDING",
                isOnline = false,
                currentLat = 13.4470,
                currentLng = -16.6790,
                driverLicense = driverLicense,
                isVerified = false
            )
            repository.saveDriver(newDriver)

            // Save to Cloud Firestore
            FirestoreManager.saveDriverOnboarding(
                driverId = uniqueId,
                name = name,
                phone = phone,
                vehicleType = vehicleType,
                vehiclePlate = vehiclePlate,
                driverLicense = driverLicense,
                verificationInfo = verificationInfo,
                onComplete = { success ->
                    onComplete(success)
                }
            )
        }
    }

    // ADMIN CONTROLS
    fun approveDriver(driverId: String) {
        viewModelScope.launch {
            repository.updateDriverApprovalStatus(driverId, "APPROVED")
        }
    }

    fun rejectDriver(driverId: String) {
        viewModelScope.launch {
            repository.updateDriverApprovalStatus(driverId, "REJECTED")
        }
    }

    // SUPPORT MESSAGES
    fun sendSupportMsg(sender: String, text: String, category: String = "General") {
        if (text.isBlank()) return
        viewModelScope.launch {
            val messageEntity = SupportMessageEntity(
                senderRole = sender,
                message = text,
                issueCategory = category
            )
            repository.sendSupportMessage(messageEntity)

            // Simulate quick supportive reply from system admin
            delay(1500)
            val supportQuotes = listOf(
                "Thank you for contacting WayGo support. We're on it and will resolve your issue right away!",
                "Hello, your ticket is recognized. WayGo is committed to keeping transit transparent and secure.",
                "Yes! Safe travel is our highest priority! If there is an emergency, feel free to tap the Emergency SOS button."
            )
            val autoReply = SupportMessageEntity(
                senderRole = "ADMIN",
                message = supportQuotes.random(),
                status = "RESOLVED",
                issueCategory = category
            )
            repository.sendSupportMessage(autoReply)
        }
    }

    // IDLE MOVEMENT ENGINE
    // Moves other online drivers very subtly on the map to give the application beautiful living motion
    private fun startIdleDriverMovement() {
        viewModelScope.launch {
            while (true) {
                delay(7000)
                val drivers = repository.getAllDrivers()
                val active = activeTrip.value
                drivers.forEach { driver ->
                    // Only wiggle inactive drivers
                    if (driver.isOnline && driver.approvalStatus == "APPROVED" && (active == null || active.driverId != driver.id)) {
                        val deltaLat = (Random.nextDouble() - 0.5) * 0.001
                        val deltaLng = (Random.nextDouble() - 0.5) * 0.001
                        repository.updateDriverLocation(
                            driver.id,
                            driver.currentLat + deltaLat,
                            driver.currentLng + deltaLng
                        )
                    }
                }
            }
        }
    }

    /**
     * Connects to Firestore to listen for real-time messages for an active ride.
     * If Firestore is offline or unconfigured, simulates replies from the partner driver or passenger.
     */
    fun startChatSession(tripId: String) {
        if (activeChatTripId == tripId) return // Already active
        
        // Clean up previous registration
        chatListenerReg?.remove()
        chatListenerReg = null
        
        activeChatTripId = tripId
        _chatMessages.value = emptyList()

        val reg = FirestoreManager.listenToChatMessages(tripId) { messages ->
            _chatMessages.value = messages
        }

        if (reg != null) {
            chatListenerReg = reg
        } else {
            // Unconfigured/Offline Fallback: Initialize with a friendly welcome message from driver
            viewModelScope.launch {
                delay(800)
                val welcome = ChatMessage(
                    id = "msg_init_" + System.currentTimeMillis(),
                    tripId = tripId,
                    senderId = "driver",
                    senderName = "Mamadou (Driver)",
                    senderRole = "DRIVER",
                    message = "Salam Alaikum! I am routing to your location now. Any specific details or landmarks?",
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = listOf(welcome)
            }
        }
    }

    /**
     * Sends a chat message in the active session. If unconfigured/offline,
     * triggers simulated autonomous replies so the user can test the chat loop.
     */
    fun sendChatMessage(
        tripId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        text: String
    ) {
        val newMsg = ChatMessage(
            id = "msg_" + System.currentTimeMillis() + "_" + (100..999).random(),
            tripId = tripId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            message = text,
            timestamp = System.currentTimeMillis()
        )

        // Add locally first for instantaneous rendering
        if (chatListenerReg == null) {
            val list = _chatMessages.value.toMutableList()
            list.add(newMsg)
            _chatMessages.value = list
        }

        FirestoreManager.sendChatMessage(newMsg) { success ->
            if (!success) {
                android.util.Log.e("WayGoViewModel", "Failed to sync message to Cloud Firestore.")
            }
        }

        // Trigger autonomous companion response if in local fallback mode
        if (chatListenerReg == null) {
            viewModelScope.launch {
                delay(2000)
                val replyText = when {
                    senderRole == "PASSENGER" && text.contains("where", ignoreCase = true) -> 
                        "Just navigating past Westfield Junction. Traffic is lightweight today, see you in 3 mins."
                    senderRole == "PASSENGER" && text.contains("landmark", ignoreCase = true) -> 
                        "Alright, noted! I know that spot. Arriving shortly."
                    senderRole == "PASSENGER" -> 
                        listOf(
                            "Got it! Understood.",
                            "Splendid. Approaching your pickup coordinate now.",
                            "Yes, on my way!",
                            "Will pull over safely once I arrive.",
                            "No problem, thanks for confirming."
                        ).random()
                    // If sender is Driver (simulating passenger response)
                    else -> 
                        listOf(
                            "Thank you! Waiting by the roadside.",
                            "Great, I am wearing a blue shirt.",
                            "Okay, see you soon!",
                            "Perfect, safe transit."
                        ).random()
                }

                val replyMsg = ChatMessage(
                    id = "msg_" + System.currentTimeMillis() + "_" + (100..999).random(),
                    tripId = tripId,
                    senderId = if (senderRole == "PASSENGER") "driver" else "passenger",
                    senderName = if (senderRole == "PASSENGER") "Mamadou (Driver)" else "Fatou (Passenger)",
                    senderRole = if (senderRole == "PASSENGER") "DRIVER" else "PASSENGER",
                    message = replyText,
                    timestamp = System.currentTimeMillis()
                )

                val list = _chatMessages.value.toMutableList()
                list.add(replyMsg)
                _chatMessages.value = list
            }
        }
    }

    /**
     * Concludes the chat session.
     */
    fun endChatSession() {
        chatListenerReg?.remove()
        chatListenerReg = null
        activeChatTripId = null
        _chatMessages.value = emptyList()
    }

    fun addSavedPlace(name: String, label: String, lat: Double, lng: Double, iconType: String) {
        viewModelScope.launch {
            repository.saveSavedPlace(
                SavedPlaceEntity(
                    name = name,
                    label = label,
                    lat = lat,
                    lng = lng,
                    iconType = iconType
                )
            )
        }
    }

    fun removeSavedPlace(id: Long) {
        viewModelScope.launch {
            repository.deleteSavedPlaceById(id)
        }
    }

    private val _payoutState = MutableStateFlow<PayoutState>(PayoutState.Idle)
    val payoutState: StateFlow<PayoutState> = _payoutState.asStateFlow()

    fun requestWeeklyPayout(driverId: String, provider: String, phone: String, gross: Int, commission: Int, net: Int) {
        viewModelScope.launch {
            _payoutState.value = PayoutState.Loading
            try {
                // Simulate disbursement gateway API call with mock network latency
                delay(1800)
                val referenceId = "PAY-FLW-" + (100000 + Random.nextInt(900000)).toString() + "-" + provider.take(3).uppercase()
                _payoutState.value = PayoutState.Success(
                    refId = referenceId,
                    provider = provider,
                    amount = net,
                    gross = gross,
                    commission = commission
                )
            } catch (e: Exception) {
                _payoutState.value = PayoutState.Error("Gateway preparation failed: " + e.localizedMessage)
            }
        }
    }

    fun resetPayoutState() {
        _payoutState.value = PayoutState.Idle
    }

    fun updateDriverLocation(driverId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.updateDriverLocation(driverId, lat, lng)
        }
    }

    // VEHICLE MILEAGE & MAINTENANCE SYSTEM
    fun updateVehicleMileage(driverId: String, newMileage: Double) {
        viewModelScope.launch {
            val existing = repository.getVehicleMileage(driverId) ?: VehicleMileageEntity(driverId = driverId)
            val updated = existing.copy(currentMileage = newMileage)
            repository.saveVehicleMileage(updated)
            checkAndTriggerMaintenanceReminders(updated)
        }
    }

    fun resetOilChange(driverId: String) {
        viewModelScope.launch {
            val existing = repository.getVehicleMileage(driverId) ?: VehicleMileageEntity(driverId = driverId)
            val updated = existing.copy(
                lastOilChangeMileage = existing.currentMileage,
                lastNotifiedOilChange = existing.currentMileage
            )
            repository.saveVehicleMileage(updated)
        }
    }

    fun resetTireCheck(driverId: String) {
        viewModelScope.launch {
            val existing = repository.getVehicleMileage(driverId) ?: VehicleMileageEntity(driverId = driverId)
            val updated = existing.copy(
                lastTireCheckMileage = existing.currentMileage,
                lastNotifiedTireCheck = existing.currentMileage
            )
            repository.saveVehicleMileage(updated)
        }
    }

    fun toggleSimulatedMileage(driverId: String, isSimulating: Boolean) {
        viewModelScope.launch {
            val existing = repository.getVehicleMileage(driverId) ?: VehicleMileageEntity(driverId = driverId)
            val updated = existing.copy(isSimulatingMileage = isSimulating)
            repository.saveVehicleMileage(updated)
        }
    }

    private fun checkAndTriggerMaintenanceReminders(mileage: VehicleMileageEntity) {
        val current = mileage.currentMileage
        val driverId = mileage.driverId

        // Oil change check
        val drivenSinceOil = current - mileage.lastOilChangeMileage
        if (drivenSinceOil >= mileage.oilChangeInterval && current - mileage.lastNotifiedOilChange >= 100.0) {
            viewModelScope.launch {
                val driver = repository.getDriverById(driverId)
                if (driver != null) {
                    triggerDriverPushNotification(
                        driverId = driverId,
                        driverName = driver.name,
                        title = "⚠️ ROUTINE OIL CHANGE REQUIRED",
                        message = "Your vehicle has driven ${drivenSinceOil.toInt()} km since your last oil change. To keep your engine performing perfectly, please schedule an oil change soon!"
                    )
                    val updated = mileage.copy(lastNotifiedOilChange = current)
                    repository.saveVehicleMileage(updated)
                }
            }
        }

        // Tire check check
        val drivenSinceTire = current - mileage.lastTireCheckMileage
        if (drivenSinceTire >= mileage.tireCheckInterval && current - mileage.lastNotifiedTireCheck >= 100.0) {
            viewModelScope.launch {
                val driver = repository.getDriverById(driverId)
                if (driver != null) {
                    triggerDriverPushNotification(
                        driverId = driverId,
                        driverName = driver.name,
                        title = "⚠️ VEHICLE TIRE ROTATION & CHECK DUE",
                        message = "Your vehicle has covered ${drivenSinceTire.toInt()} km since your last tire inspection. Check tyre pressure and rotate tyres to ensure safety."
                    )
                    val updated = mileage.copy(lastNotifiedTireCheck = current)
                    repository.saveVehicleMileage(updated)
                }
            }
        }
    }

    private fun startVehicleMileageSimulationAndReminders() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Run check every 10 seconds
                val driverId = _activeDriverId.value
                val mileage = repository.getVehicleMileage(driverId) ?: VehicleMileageEntity(driverId = driverId)
                
                if (mileage.isSimulatingMileage) {
                    // Accumulate some simulated driving mileage
                    val delta = 25.0 + Random.nextDouble() * 15.0 // Accumulate 25 to 40 km per interval to see quick alerts
                    val updated = mileage.copy(currentMileage = mileage.currentMileage + delta)
                    repository.saveVehicleMileage(updated)
                    checkAndTriggerMaintenanceReminders(updated)
                } else {
                    // Just do a safety check on current mileage
                    checkAndTriggerMaintenanceReminders(mileage)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListenerReg?.remove()
    }
}

class WayGoViewModelFactory(
    private val repository: WayGoRepository,
    private val sharedPreferences: android.content.SharedPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WayGoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WayGoViewModel(repository, sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class PushNotification(
    val id: String,
    val driverId: String,
    val driverName: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: TripEntity? = null,
    var isRead: Boolean = false
)

sealed class PayoutState {
    object Idle : PayoutState()
    object Loading : PayoutState()
    data class Success(
        val refId: String,
        val provider: String,
        val amount: Int,
        val gross: Int,
        val commission: Int
    ) : PayoutState()
    data class Error(val message: String) : PayoutState()
}

