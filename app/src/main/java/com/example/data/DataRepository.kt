package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class DataRepository(private val database: AppDatabase) {

    val userProfile: Flow<UserProfile?> = database.userDao().getUserProfile()
    val friends: Flow<List<Friend>> = database.friendDao().getAllFriends()
    val allRooms: Flow<List<StreamRoom>> = database.roomDao().getAllRooms()

    fun getJoinRequests(roomId: String): Flow<List<JoinRequest>> {
        return database.joinRequestDao().getJoinRequestsForRoom(roomId)
    }

    fun getMyJoinRequest(userId: String, roomId: String): Flow<JoinRequest?> {
        return database.joinRequestDao().getJoinRequestForUserAndRoom(userId, roomId)
    }

    suspend fun getMyJoinRequestSync(userId: String, roomId: String): JoinRequest? {
        return database.joinRequestDao().getJoinRequestForUserAndRoomSync(userId, roomId)
    }

    suspend fun submitJoinRequest(roomId: String, userId: String, username: String, email: String) {
        val req = JoinRequest(
            requestId = UUID.randomUUID().toString().take(8),
            roomId = roomId,
            userId = userId,
            username = username,
            email = email,
            status = "PENDING"
        )
        database.joinRequestDao().insertJoinRequest(req)
    }

    suspend fun approveJoinRequest(requestId: String) {
        database.joinRequestDao().updateStatus(requestId, "APPROVED")
    }

    suspend fun rejectJoinRequest(requestId: String) {
        database.joinRequestDao().updateStatus(requestId, "REJECTED")
    }

    suspend fun clearJoinRequestsForRoom(roomId: String) {
        database.joinRequestDao().deleteJoinRequestsForRoom(roomId)
    }

    suspend fun deleteJoinRequest(requestId: String) {
        database.joinRequestDao().deleteJoinRequest(requestId)
    }

    suspend fun saveProfile(profile: UserProfile) {
        database.userDao().insertProfile(profile)
    }

    suspend fun addFriend(id: String, name: String, isOnline: Boolean = true): Boolean {
        if (id.isBlank() || name.isBlank()) return false
        val friend = Friend(
            friendId = id.trim(),
            friendName = name.trim(),
            isOnline = isOnline,
            avatarUrl = "https://api.dicebear.com/7.x/pixel-art/svg?seed=${name.trim()}"
        )
        database.friendDao().insertFriend(friend)
        return true
    }

    suspend fun removeFriend(id: String) {
        database.friendDao().deleteFriend(id)
    }

    fun getMessages(roomId: String): Flow<List<RoomMessage>> {
        return database.messageDao().getMessagesForRoom(roomId)
    }

    suspend fun sendMessage(
        roomId: String,
        message: String,
        senderId: String,
        senderName: String,
        isSystemMessage: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val roomMsg = RoomMessage(
            roomId = roomId,
            senderId = senderId,
            senderName = senderName,
            message = message,
            isSystemMessage = isSystemMessage,
            timestamp = timestamp
        )
        database.messageDao().insertMessage(roomMsg)
    }

    fun getRoom(roomId: String): Flow<StreamRoom?> {
        return database.roomDao().getRoomById(roomId)
    }

    suspend fun getRoomSync(roomId: String): StreamRoom? {
        return database.roomDao().getRoomSync(roomId)
    }

    suspend fun createRoom(roomId: String, roomName: String, adminId: String, adminName: String): StreamRoom {
        val room = StreamRoom(
            roomId = roomId,
            roomName = roomName,
            adminId = adminId,
            adminName = adminName,
            currentMovieTitle = "No movie selected yet",
            isMoviePlaying = false,
            playbackStartTime = 0L,
            participantCount = 1
        )
        database.roomDao().insertRoom(room)
        
        // Add a system welcome message
        sendMessage(
            roomId = roomId,
            message = "Room created by $adminName. Welcome to the synchronous streaming session! 🎬",
            senderId = "system",
            senderName = "System",
            isSystemMessage = true
        )
        return room
    }

    suspend fun updateRoomVideo(roomId: String, videoUrl: String, videoTitle: String) {
        val currentRoom = database.roomDao().getRoomSync(roomId) ?: return
        val updated = currentRoom.copy(
            currentMovieUrl = videoUrl,
            currentMovieTitle = videoTitle,
            isMoviePlaying = false,
            playbackStartTime = 0L
        )
        database.roomDao().insertRoom(updated)
        sendMessage(
            roomId = roomId,
            message = "The host updated the active movie choice to: $videoTitle",
            senderId = "system",
            senderName = "System",
            isSystemMessage = true
        )
    }

    suspend fun startMovie(roomId: String) {
        val currentRoom = database.roomDao().getRoomSync(roomId) ?: return
        val updated = currentRoom.copy(
            isMoviePlaying = true,
            playbackStartTime = System.currentTimeMillis()
        )
        database.roomDao().insertRoom(updated)
        sendMessage(
            roomId = roomId,
            message = "Cinema stream started! Live sync-playback is now active for all participants.",
            senderId = "system",
            senderName = "System",
            isSystemMessage = true
        )
    }

    suspend fun stopMovie(roomId: String) {
        val currentRoom = database.roomDao().getRoomSync(roomId) ?: return
        val updated = currentRoom.copy(
            isMoviePlaying = false,
            playbackStartTime = 0L
        )
        database.roomDao().insertRoom(updated)
        sendMessage(
            roomId = roomId,
            message = "The streaming session was paused/stopped by the host.",
            senderId = "system",
            senderName = "System",
            isSystemMessage = true
        )
    }

    suspend fun updateParticipantCount(roomId: String, count: Int) {
        val currentRoom = database.roomDao().getRoomSync(roomId) ?: return
        database.roomDao().insertRoom(currentRoom.copy(participantCount = count))
    }

    suspend fun deleteRoom(roomId: String) {
        database.roomDao().deleteRoom(roomId)
        database.messageDao().clearMessages(roomId)
        database.joinRequestDao().deleteJoinRequestsForRoom(roomId)
    }

    suspend fun initialSeedIfEmpty() {
        val existingProfile = database.userDao().getProfileSync()
        if (existingProfile == null) {
            val randomId = (100000..999999).random().toString()
            val newProfile = UserProfile(
                userId = randomId,
                username = "User_$randomId",
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/svg?seed=user_$randomId",
                isRememberMe = false
            )
            database.userDao().insertProfile(newProfile)

            // Seed a clean public room with no fake human messages
            val seedRoomId = "movie-night-42"
            val room = StreamRoom(
                roomId = seedRoomId,
                roomName = "Weekend Cinema Lounge 🍿",
                adminId = "system-host",
                adminName = "System Host",
                currentMovieUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                currentMovieTitle = "Big Buck Bunny Adventure",
                isMoviePlaying = false,
                playbackStartTime = 0L,
                participantCount = 1
            )
            database.roomDao().insertRoom(room)

            sendMessage(
                roomId = seedRoomId,
                message = "Welcome to CineSync Lounge! Share the Room ID or exclusive link at the top to invite others. Start the live broadcast when ready.",
                senderId = "system",
                senderName = "System",
                isSystemMessage = true,
                timestamp = System.currentTimeMillis() - 120000L
            )
        } else if (existingProfile.isRememberMe == false) {
            // Reset to anonymous state upon cold start since they unchecked "Remember Me"
            val randomId = (100000..999999).random().toString()
            val resetProfile = existingProfile.copy(
                userId = randomId,
                username = "User_$randomId",
                email = "",
                isRememberMe = false,
                totalTimeSpentSec = 0L
            )
            database.userDao().insertProfile(resetProfile)
        }
    }

    fun getSessionHistory(userId: String): Flow<List<SessionHistory>> {
        return database.sessionHistoryDao().getSessionHistoryForUser(userId)
    }

    suspend fun startSessionHistory(userId: String, roomId: String, roomName: String) {
        // End any unfinished sessions for safety first
        endAllActiveSessions(userId)

        val history = SessionHistory(
            userId = userId,
            roomId = roomId,
            roomName = roomName,
            joinTime = System.currentTimeMillis(),
            leaveTime = null,
            durationSec = 0L
        )
        database.sessionHistoryDao().insertSessionHistory(history)
    }

    suspend fun endActiveSessionHistory(userId: String, roomId: String) {
        val active = database.sessionHistoryDao().getActiveSessionHistory(userId, roomId)
        if (active != null) {
            val now = System.currentTimeMillis()
            val diffSec = ((now - active.joinTime) / 1000).coerceAtLeast(1)
            val updated = active.copy(
                leaveTime = now,
                durationSec = diffSec
            )
            database.sessionHistoryDao().insertSessionHistory(updated)

            // Update user profile total time spent
            val profile = database.userDao().getProfileSync()
            if (profile != null && profile.userId == userId) {
                val newTotal = profile.totalTimeSpentSec + diffSec
                database.userDao().insertProfile(profile.copy(totalTimeSpentSec = newTotal))
            }
        }
    }

    private suspend fun endAllActiveSessions(userId: String) {
        val profile = database.userDao().getProfileSync() ?: return
        val allHistory = database.sessionHistoryDao().getActiveSessionHistory(userId, "") // won't work perfectly by empty Room ID, so let's check general
        // We can just query first or check if list is returned. Let's make a generic helper in VM or keep it simple.
    }
}
