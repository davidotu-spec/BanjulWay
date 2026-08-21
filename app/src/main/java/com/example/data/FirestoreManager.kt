package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreManager {
    private const val TAG = "FirestoreManager"

    // Safely retrieve the Firestore instance. If Google Services (google-services.json) is 
    // missing or unconfigured, logging is provided without crashing the client app.
    val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.i(TAG, "Firebase Firestore holds pending configuration. Running simulated offline-first fallback. Info: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Stores or updates passenger or driver profile information in the 'users' collection in Firestore.
     */
    fun saveUserProfileToFirestore(
        userId: String,
        displayName: String,
        phoneNumber: String,
        email: String,
        role: String = "PASSENGER",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanUid = userId.ifBlank { "user_${System.currentTimeMillis()}" }
        val userData = hashMapOf<String, Any>(
            "uid" to cleanUid,
            "displayName" to displayName,
            "phoneNumber" to phoneNumber,
            "email" to email,
            "role" to role,
            "updatedAt" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis()
        )

        val db = firestore
        if (db == null) {
            Log.w(TAG, "Simulating Local Firestore Save for user profile in 'users' collection: $cleanUid ($displayName)")
            onComplete(true)
            return
        }

        try {
            Log.d(TAG, "Writing profile to Firestore 'users' collection for UID: $cleanUid")
            db.collection("users")
                .document(cleanUid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.i(TAG, "Firestore 'users' document written successfully for: $cleanUid")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore write to 'users' collection failure", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore saveUserProfile exception: ${e.localizedMessage}")
            onComplete(false)
        }
    }

    /**
     * Stashes driver application onboarding details to the Firebase Firestore 'driver_onboardings' collection.
     */
    fun saveDriverOnboarding(
        driverId: String,
        name: String,
        phone: String,
        vehicleType: String,
        vehiclePlate: String,
        driverLicense: String,
        verificationInfo: String,
        onComplete: (Boolean) -> Unit
    ) {
        val onboardingData = hashMapOf(
            "id" to driverId,
            "name" to name,
            "phone" to phone,
            "vehicleType" to vehicleType,
            "vehiclePlate" to vehiclePlate,
            "driverLicense" to driverLicense,
            "verificationInfo" to verificationInfo,
            "approvalStatus" to "PENDING",
            "isVerified" to false,
            "rating" to 5.0,
            "onboardedAt" to System.currentTimeMillis()
        )

        val db = firestore
        if (db == null) {
            Log.w(TAG, "Simulating Local Firestore Save Success for Document ID: $driverId")
            onComplete(true) // offline / local simulation success
            return
        }

        try {
            Log.d(TAG, "Writing driver onboarding registry to cloud Firestore collection...")
            db.collection("driver_onboardings")
                .document(driverId)
                .set(onboardingData)
                .addOnSuccessListener {
                    Log.i(TAG, "Firestore document written successfully: $driverId")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore write execution failure", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore security context or execution exception: ${e.localizedMessage}")
            onComplete(false)
        }
    }

    /**
     * Stashes completed trip details to the Firebase Firestore 'trips_history' collection.
     */
    fun saveTripToFirestore(
        trip: TripEntity,
        onComplete: (Boolean) -> Unit
    ) {
        val tripData = hashMapOf<String, Any?>(
            "id" to trip.id,
            "passengerName" to trip.passengerName,
            "driverId" to trip.driverId,
            "driverName" to (trip.driverName ?: "Gambia Partner Driver"),
            "vehicleType" to trip.vehicleType,
            "vehiclePlate" to (trip.vehiclePlate ?: "BJL Registered"),
            "pickupName" to trip.pickupName,
            "dropoffName" to trip.dropoffName,
            "pickupLat" to trip.pickupLat,
            "pickupLng" to trip.pickupLng,
            "dropoffLat" to trip.dropoffLat,
            "dropoffLng" to trip.dropoffLng,
            "fareGmd" to trip.fareGmd,
            "paymentMethod" to trip.paymentMethod,
            "status" to trip.status,
            "rating" to trip.rating,
            "reviewComment" to trip.reviewComment,
            "reviewTags" to trip.reviewTags,
            "tipGmd" to trip.tipGmd,
            "timestamp" to trip.timestamp,
            "commissionGmd" to trip.commissionGmd
        )

        val db = firestore
        if (db == null) {
            Log.w(TAG, "Simulating Local Firestore Save Trip Success for Document ID: ${trip.id}")
            onComplete(true) // local simulation success
            return
        }

        try {
            Log.d(TAG, "Writing trip details to Firestore trips_history collection...")
            db.collection("trips_history")
                .document(trip.id)
                .set(tripData)
                .addOnSuccessListener {
                    Log.i(TAG, "Firestore trip written successfully: ${trip.id}")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore write trip failed", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore security context/write error: ${e.localizedMessage}")
            onComplete(false)
        }
    }

    /**
     * Stashes passenger driver rating feedback directly to the Firebase Firestore 'trip_ratings' collection.
     */
    fun saveTripRatingToFirestore(
        tripId: String,
        driverId: String,
        driverName: String,
        passengerName: String,
        stars: Int,
        reviewComment: String,
        reviewTags: String,
        tipGmd: Int,
        onComplete: (Boolean) -> Unit
    ) {
        val ratingData = hashMapOf<String, Any?>(
            "ratingId" to "rating_$tripId",
            "tripId" to tripId,
            "driverId" to driverId,
            "driverName" to driverName,
            "passengerName" to passengerName,
            "stars" to stars,
            "reviewComment" to reviewComment,
            "reviewTags" to reviewTags,
            "tipGmd" to tipGmd,
            "timestamp" to System.currentTimeMillis()
        )

        val db = firestore
        if (db == null) {
            Log.w(TAG, "Simulating Local Firestore Save Rating Success for Document ID: rating_$tripId")
            onComplete(true)
            return
        }

        try {
            Log.d(TAG, "Writing passenger rating to Firestore trip_ratings collection...")
            db.collection("trip_ratings")
                .document("rating_$tripId")
                .set(ratingData)
                .addOnSuccessListener {
                    Log.i(TAG, "Firestore trip rating document written successfully")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore write trip rating failed", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore write trip rating error: ${e.localizedMessage}")
            onComplete(false)
        }
    }

    /**
     * Fetches trip history from the Firebase Firestore 'trips_history' collection.
     */
    fun fetchTripHistoryFromFirestore(
        onComplete: (List<TripEntity>) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            Log.w(TAG, "Firestore is unconfigured/offline. Returning simulation fallback code.")
            val simulatedHistory = listOf(
                TripEntity(
                    id = "trip_cloud_1",
                    passengerName = "John Doe",
                    driverId = "drv_alieu",
                    driverName = "Alieu Ceesay",
                    vehicleType = "CAR",
                    vehiclePlate = "BJL 4821 C",
                    pickupName = "Kairaba Avenue, Serrekunda",
                    dropoffName = "Albert Market, Banjul",
                    pickupLat = 13.4470,
                    pickupLng = -16.6790,
                    dropoffLat = 13.4530,
                    dropoffLng = -16.5760,
                    fareGmd = 350,
                    paymentMethod = "WAVE",
                    status = "COMPLETED",
                    rating = 5,
                    reviewComment = "Wonderful trip! Extremely polite driver.",
                    reviewTags = "Safe, Polite, Clean Vehicle",
                    timestamp = System.currentTimeMillis() - 12 * 3600 * 1000
                ),
                TripEntity(
                    id = "trip_cloud_2",
                    passengerName = "John Doe",
                    driverId = "drv_mariama",
                    driverName = "Mariama Jallow",
                    vehicleType = "TRICYCLE",
                    vehiclePlate = "KM 9312 T",
                    pickupName = "Senegambia Strip, Kololi",
                    dropoffName = "Kairaba Avenue, Serrekunda",
                    pickupLat = 13.4380,
                    pickupLng = -16.7120,
                    dropoffLat = 13.4470,
                    dropoffLng = -16.6790,
                    fareGmd = 150,
                    paymentMethod = "CASH",
                    status = "COMPLETED",
                    rating = 4,
                    reviewComment = "Quick ride, highly recommended for short distance.",
                    reviewTags = "Quick, Great Music",
                    timestamp = System.currentTimeMillis() - 2 * 24 * 3600 * 1000
                ),
                TripEntity(
                    id = "trip_cloud_3",
                    passengerName = "John Doe",
                    driverId = "drv_bakary",
                    driverName = "Bakary Touray",
                    vehicleType = "CAR",
                    vehiclePlate = "WCR 7431 B",
                    pickupName = "Brikama Market, Brikama",
                    dropoffName = "Senegambia Strip, Kololi",
                    pickupLat = 13.2720,
                    pickupLng = -16.6540,
                    dropoffLat = 13.4380,
                    dropoffLng = -16.7120,
                    fareGmd = 550,
                    paymentMethod = "AFRICELL",
                    status = "COMPLETED",
                    rating = 5,
                    reviewComment = "Excellent service and safe driving.",
                    reviewTags = "Safe, Polite",
                    timestamp = System.currentTimeMillis() - 5 * 24 * 3600 * 1000
                )
            )
            onComplete(simulatedHistory)
            return
        }

        try {
            db.collection("trips_history")
                .get()
                .addOnSuccessListener { result ->
                    val tripsList = mutableListOf<TripEntity>()
                    for (doc in result) {
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val passengerName = doc.getString("passengerName") ?: "Passenger"
                            val driverId = doc.getString("driverId")
                            val driverName = doc.getString("driverName") ?: "Gambia Partner Driver"
                            val vehicleType = doc.getString("vehicleType") ?: "CAR"
                            val vehiclePlate = doc.getString("vehiclePlate") ?: "BJL Registered"
                            val pickupName = doc.getString("pickupName") ?: "Standard Pickup"
                            val dropoffName = doc.getString("dropoffName") ?: "Standard Dropoff"
                            val pickupLat = doc.getDouble("pickupLat") ?: 13.45
                            val pickupLng = doc.getDouble("pickupLng") ?: -16.68
                            val dropoffLat = doc.getDouble("dropoffLat") ?: 13.45
                            val dropoffLng = doc.getDouble("dropoffLng") ?: -16.68
                            val fareGmd = doc.getLong("fareGmd")?.toInt() ?: 150
                            val paymentMethod = doc.getString("paymentMethod") ?: "CASH"
                            val status = doc.getString("status") ?: "COMPLETED"
                            val rating = doc.getLong("rating")?.toInt() ?: 0
                            val reviewComment = doc.getString("reviewComment") ?: ""
                            val reviewTags = doc.getString("reviewTags") ?: ""
                            val tipGmd = doc.getLong("tipGmd")?.toInt() ?: 0
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val commissionGmd = doc.getLong("commissionGmd")?.toInt() ?: (fareGmd * 0.15).toInt()

                            tripsList.add(
                                TripEntity(
                                    id = id,
                                    passengerName = passengerName,
                                    driverId = driverId,
                                    driverName = driverName,
                                    vehicleType = vehicleType,
                                    vehiclePlate = vehiclePlate,
                                    pickupName = pickupName,
                                    dropoffName = dropoffName,
                                    pickupLat = pickupLat,
                                    pickupLng = pickupLng,
                                    dropoffLat = dropoffLat,
                                    dropoffLng = dropoffLng,
                                    fareGmd = fareGmd,
                                    paymentMethod = paymentMethod,
                                    status = status,
                                    rating = rating,
                                    reviewComment = reviewComment,
                                    reviewTags = reviewTags,
                                    tipGmd = tipGmd,
                                    timestamp = timestamp,
                                    commissionGmd = commissionGmd
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing Firestore document: ${e.localizedMessage}")
                        }
                    }
                    tripsList.sortByDescending { it.timestamp }
                    onComplete(tripsList)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching from Firestore", e)
                    onComplete(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore query exception: ${e.localizedMessage}")
            onComplete(emptyList())
        }
    }

    /**
     * Sends a chat message to Firestore under a trip-specific chat sub-collection.
     */
    fun sendChatMessage(
        msg: ChatMessage,
        onComplete: (Boolean) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            Log.w(TAG, "Unconfigured Firestore. Message simulated locally.")
            onComplete(true)
            return
        }

        try {
            val msgData = hashMapOf(
                "id" to msg.id,
                "tripId" to msg.tripId,
                "senderId" to msg.senderId,
                "senderName" to msg.senderName,
                "senderRole" to msg.senderRole,
                "message" to msg.message,
                "timestamp" to msg.timestamp
            )

            db.collection("trip_chats")
                .document(msg.tripId)
                .collection("messages")
                .document(msg.id)
                .set(msgData)
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener {
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.localizedMessage}")
            onComplete(false)
        }
    }

    /**
     * Listens real-time to messages for a specific ride/trip.
     */
    fun listenToChatMessages(
        tripId: String,
        onUpdate: (List<ChatMessage>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        val db = firestore ?: return null

        return try {
            db.collection("trip_chats")
                .document(tripId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed for chat: $tripId", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val messages = mutableListOf<ChatMessage>()
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val tId = doc.getString("tripId") ?: tripId
                                val sId = doc.getString("senderId") ?: ""
                                val sName = doc.getString("senderName") ?: ""
                                val sRole = doc.getString("senderRole") ?: ""
                                val msgTxt = doc.getString("message") ?: ""
                                val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                messages.add(
                                    ChatMessage(
                                        id = id,
                                        tripId = tId,
                                        senderId = sId,
                                        senderName = sName,
                                        senderRole = sRole,
                                        message = msgTxt,
                                        timestamp = ts
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing chat message doc: ${e.localizedMessage}")
                            }
                        }
                        onUpdate(messages)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error registration for chat: ${e.localizedMessage}")
            null
        }
    }
}

/**
 * Data representation for Passenger-Driver real-time coordinate chat messages.
 */
data class ChatMessage(
    val id: String = "",
    val tripId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "", // "PASSENGER" or "DRIVER"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

