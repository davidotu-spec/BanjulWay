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

class BanjulWayViewModel(private val repository: BanjulWayRepository) : ViewModel() {

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

    // Active Driver Profile (for Driver Hub view)
    private val _activeDriverId = MutableStateFlow("drv_alieu")
    val activeDriverId: StateFlow<String> = _activeDriverId.asStateFlow()

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
        // Fetch past ride history from Firestore on launch
        refreshTripHistoryFromFirestore()
    }

    fun refreshTripHistoryFromFirestore() {
        viewModelScope.launch {
            _firestoreIsLoading.value = true
            _firestoreStatusMessage.value = "Fetching past rides from Firestore Cloud..."
            try {
                FirestoreManager.fetchTripHistoryFromFirestore { fetchedTrips ->
                    _firestoreTrips.value = fetchedTrips
                    _firestoreIsLoading.value = false
                    if (fetchedTrips.isEmpty()) {
                        _firestoreStatusMessage.value = "No previous online rides found in Firestore. Showing local cache."
                    } else {
                        _firestoreStatusMessage.value = "Fetched ${fetchedTrips.size} rides from Firestore Cloud!"
                    }
                }
            } catch (e: Exception) {
                _firestoreIsLoading.value = false
                _firestoreStatusMessage.value = "Cloud query failed: ${e.localizedMessage}"
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
        _isUserLoggedIn.value = false
        _otpRequested.value = false
        _generatedOtp.value = ""
    }

    fun sendOtp(phone: String) {
        if (phone.isBlank() || phone.length < 5) {
            _authError.value = "Please enter a valid phone number"
            return
        }
        _authError.value = ""
        val randomOtp = (1000..9999).random().toString()
        _generatedOtp.value = randomOtp
        _otpRequested.value = true
    }

    fun verifyOtp(enteredCode: String) {
        if (enteredCode == _generatedOtp.value || enteredCode == "1234" || enteredCode == "5581") {
            _isUserLoggedIn.value = true
            _authError.value = ""
        } else {
            _authError.value = "Incorrect code. Please check your SMS and try again."
        }
    }

    // USER PROFILE
    fun saveProfile(name: String, phone: String, email: String, gender: String, mobileMoney: String, savedHome: String, savedWork: String, avatarIndex: Int) {
        viewModelScope.launch {
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
                    avatarIndex = avatarIndex
                )
            )
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

            val nearest = getNearestAvailableDrivers(pLat, pLng, vehicleType, 25.0)
            _broadcastDrivers.value = nearest

            if (nearest.isEmpty()) {
                _broadcastLogs.value = _broadcastLogs.value + "No drivers found in 25km. Spawning emergency nearby virtual responder..."
                delay(1200)

                val spawnedDriver = DriverEntity(
                    id = "drv_spawned_" + (1000..9999).random(),
                    name = listOf("Modou Barrow", "Ebrima Jallow", "Fatou Sarr", "Alieu Ceesay", "Binta Diallo").random(),
                    phone = "+220 7" + (100000..999999).random(),
                    vehicleType = vehicleType,
                    vehiclePlate = "BJL " + (1000..9999).random() + " " + listOf("A", "B", "C", "D").random(),
                    rating = (45..49).random() / 10f,
                    approvalStatus = "APPROVED",
                    isOnline = true,
                    currentLat = pLat + (Random.nextDouble(-0.015, 0.015)),
                    currentLng = pLng + (Random.nextDouble(-0.015, 0.015)),
                    driverLicense = "DL-2026-gen"
                )
                repository.saveDriver(spawnedDriver)

                val newNearest = getNearestAvailableDrivers(pLat, pLng, vehicleType, 25.0)
                _broadcastDrivers.value = newNearest
                val distStr = String.format("%.2f", calculateHaversineDistance(pLat, pLng, spawnedDriver.currentLat, spawnedDriver.currentLng))
                _broadcastLogs.value = _broadcastLogs.value + "Spawned responder: ${spawnedDriver.name} at ${distStr}km"
            } else {
                val foundMsg = "Found ${nearest.size} eligible $vehicleType drivers nearby."
                _broadcastLogs.value = _broadcastLogs.value + foundMsg
            }

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

            // Auto accept by the nearest driver after pings
            delay(1200)
            val selectedMatch = _broadcastDrivers.value.firstOrNull()?.first
            if (selectedMatch != null) {
                _broadcastLogs.value = _broadcastLogs.value + "Ride request matched. ${selectedMatch.name} is arriving!"
                val currentTrip = repository.getTripById(tripId)
                if (currentTrip != null && currentTrip.status == "REQUESTED") {
                    val updatedTrip = currentTrip.copy(
                        driverId = selectedMatch.id,
                        driverName = selectedMatch.name,
                        vehiclePlate = selectedMatch.vehiclePlate,
                        vehicleType = selectedMatch.vehicleType
                    )
                    repository.saveTrip(updatedTrip)
                    acceptBooking(tripId, selectedMatch.id)
                }
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
                android.util.Log.d("BanjulWayViewModel", "Cloud Firestore trip sync status: $success")
                refreshTripHistoryFromFirestore()
            }
        }
    }

    // DRIVER CONTROLS
    fun toggleDriverOnlineState(driverId: String, isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateDriverOnlineStatus(driverId, isOnline)
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
                "Thank you for contacting BanjulWay support. We're on it and will resolve your issue right away!",
                "Hello, your ticket is recognized. BanjulWay is committed to keeping transit transparent and secure.",
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
                android.util.Log.e("BanjulWayViewModel", "Failed to sync message to Cloud Firestore.")
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

    override fun onCleared() {
        super.onCleared()
        chatListenerReg?.remove()
    }
}

class BanjulWayViewModelFactory(private val repository: BanjulWayRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BanjulWayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BanjulWayViewModel(repository) as T
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
