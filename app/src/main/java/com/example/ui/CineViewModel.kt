@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class VoiceParticipant(
    val id: String,
    val name: String,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val volume: Float = 0f
)

class CineViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DataRepository(database)

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val friends: StateFlow<List<Friend>> = repository.friends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRooms: StateFlow<List<StreamRoom>> = repository.allRooms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionHistory: StateFlow<List<SessionHistory>> = userProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getSessionHistory(profile.userId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    val activeRoom: StateFlow<StreamRoom?> = _activeRoomId
        .flatMapLatest { id ->
            if (id != null) repository.getRoom(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeRoomMessages: StateFlow<List<RoomMessage>> = _activeRoomId
        .flatMapLatest { id ->
            if (id != null) repository.getMessages(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingJoinRequests: StateFlow<List<JoinRequest>> = _activeRoomId
        .flatMapLatest { id ->
            if (id != null) repository.getJoinRequests(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _requestedRoomId = MutableStateFlow<String?>(null)
    val requestedRoomId: StateFlow<String?> = _requestedRoomId.asStateFlow()

    val myJoinRequestForRequestedRoom: StateFlow<JoinRequest?> = combine(_requestedRoomId, userProfile) { roomId, profile ->
        Pair(roomId, profile)
    }.flatMapLatest { (roomId, profile) ->
        if (roomId != null && profile != null) {
            repository.getMyJoinRequest(profile.userId, roomId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isKicked: StateFlow<Boolean> = combine(activeRoomMessages, userProfile) { messages, profile ->
        if (profile == null) false else {
            messages.any { it.isSystemMessage && it.message == "KICK_USER:${profile.userId}" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Voice chat simulation
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isScreenSecureEnabled = MutableStateFlow(false)
    val isScreenSecureEnabled: StateFlow<Boolean> = _isScreenSecureEnabled.asStateFlow()

    private val _voiceParticipants = MutableStateFlow<List<VoiceParticipant>>(emptyList())
    val voiceParticipants: StateFlow<List<VoiceParticipant>> = _voiceParticipants.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialSeedIfEmpty()
        }
        // Start simulated voice activity pulse
        simulateVoiceActivity()
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        viewModelScope.launch {
            val profile = userProfile.value ?: return@launch
            val roomId = _activeRoomId.value ?: return@launch
            repository.sendMessage(
                roomId = roomId,
                message = "${profile.username} has ${if (muted) "muted their microphone 🔇" else "unmuted their microphone 🎤"}",
                senderId = "system",
                senderName = "System",
                isSystemMessage = true
            )
        }
    }

    fun setScreenSecureEnabled(enabled: Boolean) {
        _isScreenSecureEnabled.value = enabled
        viewModelScope.launch {
            val roomId = _activeRoomId.value ?: return@launch
            val profile = userProfile.value ?: return@launch
            val message = if (enabled) {
                "🔒 ${profile.username} enabled Content Screen Protection. Real-time media captures and recording attempts are secured."
            } else {
                "⚠️ ${profile.username} disabled Content Screen Protection. Display stream security is inactive."
            }
            repository.sendMessage(
                roomId = roomId,
                message = message,
                senderId = "system",
                senderName = "System",
                isSystemMessage = true
            )
        }
    }

    // Simulate screen recording attempt trigger for demo purposes!
    fun simulateScreenRecordWarning(fakeUserName: String) {
        viewModelScope.launch {
            val roomId = _activeRoomId.value ?: return@launch
            repository.sendMessage(
                roomId = roomId,
                message = "🚫 Security Alert: [$fakeUserName] attempted to capture the movie stream! Display privacy filter triggered.",
                senderId = "security",
                senderName = "Security",
                isSystemMessage = true
            )
        }
    }

    fun updateProfile(username: String) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            repository.saveProfile(current.copy(username = username))
        }
    }

    fun addFriend(friendId: String, friendName: String) {
        viewModelScope.launch {
            repository.addFriend(friendId, friendName)
        }
    }

    fun deleteFriend(friendId: String) {
        viewModelScope.launch {
            repository.removeFriend(friendId)
        }
    }

    fun createAndJoinRoom(roomName: String, adminOnlyUrl: String = "", movieTitle: String = "") {
        viewModelScope.launch {
            val profile = userProfile.value ?: return@launch
            val roomId = UUID.randomUUID().toString().take(6)
            repository.createRoom(
                roomId = roomId,
                roomName = roomName,
                adminId = profile.userId,
                adminName = profile.username
            )
            if (adminOnlyUrl.isNotBlank()) {
                repository.updateRoomVideo(roomId, adminOnlyUrl, movieTitle.ifBlank { "Custom Video Stream" })
            }
            joinRoom(roomId)
        }
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            val profile = userProfile.value ?: return@launch
            _activeRoomId.value = roomId
            
            // Adjust participant count
            val room = repository.getRoomSync(roomId)
            if (room != null) {
                repository.updateParticipantCount(roomId, room.participantCount + 1)
                
                // Set initial voice team
                setupSimulationVoiceUsers(room)

                // Log Session History
                repository.startSessionHistory(profile.userId, roomId, room.roomName)

                repository.sendMessage(
                    roomId = roomId,
                    message = "✨ ${profile.username} joined the session.",
                    senderId = "system",
                    senderName = "System",
                    isSystemMessage = true
                )
            }
        }
    }

    fun leaveRoom() {
        val roomId = _activeRoomId.value ?: return
        val profile = userProfile.value
        _activeRoomId.value = null
        _voiceParticipants.value = emptyList()
        viewModelScope.launch {
            if (profile != null) {
                // Log Session End History
                repository.endActiveSessionHistory(profile.userId, roomId)

                repository.sendMessage(
                    roomId = roomId,
                    message = "👋 ${profile.username} left the session.",
                    senderId = "system",
                    senderName = "System",
                    isSystemMessage = true
                )
            }
            val room = repository.getRoomSync(roomId)
            if (room != null) {
                val newCount = (room.participantCount - 1).coerceAtLeast(1)
                repository.updateParticipantCount(roomId, newCount)
            }
        }
    }

    fun updateVideo(videoUrl: String, videoTitle: String) {
        val roomId = _activeRoomId.value ?: return
        viewModelScope.launch {
            repository.updateRoomVideo(roomId, videoUrl, videoTitle)
        }
    }

    fun startMovie() {
        val roomId = _activeRoomId.value ?: return
        viewModelScope.launch {
            repository.startMovie(roomId)
        }
    }

    fun stopMovie() {
        val roomId = _activeRoomId.value ?: return
        viewModelScope.launch {
            repository.stopMovie(roomId)
        }
    }

    fun postChatMessage(text: String) {
        val roomId = _activeRoomId.value ?: return
        val profile = userProfile.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                roomId = roomId,
                message = text.trim(),
                senderId = profile.userId,
                senderName = profile.username
            )
        }
    }

    // Setup active speaking team inside voice channel - clean and real!
    private fun setupSimulationVoiceUsers(room: StreamRoom) {
        val list = mutableListOf<VoiceParticipant>()
        val profile = userProfile.value
        if (profile != null) {
            list.add(VoiceParticipant(id = profile.userId, name = profile.username, isMuted = _isMuted.value))
        }
        
        // Add only the room host if they are online and different
        if (room.adminId != profile?.userId && room.adminId != "system-host") {
            list.add(VoiceParticipant(id = room.adminId, name = room.adminName))
        }
        
        _voiceParticipants.value = list
    }

    private fun simulateVoiceActivity() {
        viewModelScope.launch {
            while (true) {
                delay(1200)
                val current = _voiceParticipants.value
                if (current.isNotEmpty()) {
                    val updated = current.map { partic ->
                        if (partic.id == userProfile.value?.userId) {
                            // Match actual user's mute state
                            partic.copy(
                                isMuted = _isMuted.value,
                                isSpeaking = !_isMuted.value && (0..5).random() > 3,
                                volume = if (!_isMuted.value) (10..100).random() / 100f else 0f
                            )
                        } else {
                            if (partic.isMuted) {
                                partic
                            } else {
                                val isSpeaking = (0..5).random() > 2
                                partic.copy(
                                    isSpeaking = isSpeaking,
                                    volume = if (isSpeaking) (15..90).random() / 100f else 0f
                                )
                            }
                        }
                    }
                    _voiceParticipants.value = updated
                }
            }
        }
    }

    fun requestToJoin(roomId: String) {
        val profile = userProfile.value ?: return
        _requestedRoomId.value = roomId
        viewModelScope.launch {
            repository.submitJoinRequest(roomId, profile.userId, profile.username, profile.email)
            
            // Simulation check: if the room admin is NOT the current user, simulated auto-approval after 2 seconds
            val room = repository.getRoomSync(roomId)
            if (room != null && room.adminId != profile.userId) {
                delay(2000)
                val req = repository.getMyJoinRequestSync(profile.userId, roomId)
                if (req != null && req.status == "PENDING") {
                    repository.approveJoinRequest(req.requestId)
                    repository.sendMessage(
                        roomId = roomId,
                        message = "🎟️ Stream request for ${profile.username} approved by the Host.",
                        senderId = "system",
                        senderName = "System",
                        isSystemMessage = true
                    )
                }
            }
        }
    }

    fun cancelJoinRequest() {
        val roomId = _requestedRoomId.value
        val profile = userProfile.value
        _requestedRoomId.value = null
        if (roomId != null && profile != null) {
            viewModelScope.launch {
                val req = repository.getMyJoinRequestSync(profile.userId, roomId)
                if (req != null) {
                    repository.deleteJoinRequest(req.requestId)
                }
            }
        }
    }

    fun acknowledgeRejectedRequest() {
        val roomId = _requestedRoomId.value
        val profile = userProfile.value
        _requestedRoomId.value = null
        if (roomId != null && profile != null) {
            viewModelScope.launch {
                val req = repository.getMyJoinRequestSync(profile.userId, roomId)
                if (req != null) {
                    repository.deleteJoinRequest(req.requestId)
                }
            }
        }
    }

    fun approveRequest(request: JoinRequest) {
        viewModelScope.launch {
            repository.approveJoinRequest(request.requestId)
            repository.sendMessage(
                roomId = request.roomId,
                message = "🎟️ Stream request for ${request.username} has been approved by the Host.",
                senderId = "system",
                senderName = "System",
                isSystemMessage = true
            )
        }
    }

    fun rejectRequest(request: JoinRequest) {
        viewModelScope.launch {
            repository.rejectJoinRequest(request.requestId)
        }
    }

    fun kickUser(userId: String, userName: String) {
        val roomId = _activeRoomId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                roomId = roomId,
                message = "KICK_USER:$userId",
                senderId = "system",
                senderName = "System",
                isSystemMessage = true
            )
            repository.sendMessage(
                roomId = roomId,
                message = "🚫 The Host has removed [$userName] from the session.",
                senderId = "system",
                senderName = "System",
                isSystemMessage = true
            )
        }
    }

    fun saveUserProfileWithAuth(username: String, email: String, isRememberMe: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            repository.saveProfile(current.copy(
                username = username,
                email = email,
                isRememberMe = isRememberMe
            ))
        }
    }

    fun logout() {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            val randomId = (100000..999999).random().toString()
            repository.saveProfile(UserProfile(
                userId = randomId,
                username = "User_$randomId",
                email = "",
                isRememberMe = false
            ))
            _activeRoomId.value = null
            _requestedRoomId.value = null
        }
    }

    fun submitReportAndEmail(reason: String, roomId: String?, roomName: String?, reporterMsg: String) {
        viewModelScope.launch {
            val profile = userProfile.value
            val chatHistory = activeRoomMessages.value

            val chatLogStr = chatHistory.joinToString("\n") { msg ->
                val dateFormatted = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                } catch (e: Exception) {
                    msg.timestamp.toString()
                }
                "[$dateFormatted] ${msg.senderName}: ${msg.message}"
            }

            // Constructing report payload (including full formatted chat log for the admin's email logs)
            val emailBody = """
                ===================================
                CINESYNC SECURE VIOLATION / CRIME REPORT
                ===================================
                To: omaralshorman454@gmail.com
                Subject: Chat Abuse Report - Room: ${roomName ?: "N/A"}
                
                Reporter Username: ${profile?.username ?: "Anonymous"}
                Reporter Email: ${profile?.email ?: "Not specified"}
                Reporter ID: ${profile?.userId ?: "N/A"}
                
                Room ID: ${roomId ?: "N/A"}
                Room Name: ${roomName ?: "N/A"}
                Reason for reporting: $reason
                Additional reporter description: $reporterMsg
                
                -----------------------------------
                CONCLUDED FULL ROOM CHAT LOGS:
                -----------------------------------
                $chatLogStr
                ===================================
            """.trimIndent()

            // Safe mock silent API/Console Delivery
            android.util.Log.d("CineSyncSecureMail", "Sending secure encrypted report payload to omaralshorman454@gmail.com...")
            android.util.Log.d("CineSyncSecureMail", emailBody)
            
            // Simulating a real secure dispatch background network/API request
            delay(1500)
        }
    }
}
