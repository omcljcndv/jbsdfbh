package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val userId: String,
    val username: String,
    val avatarUrl: String = "",
    val isOnline: Boolean = true,
    val email: String = "",
    val isRememberMe: Boolean = false,
    val totalTimeSpentSec: Long = 0L
)

@Entity(tableName = "session_history")
data class SessionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val roomId: String,
    val roomName: String,
    val joinTime: Long,
    val leaveTime: Long? = null,
    val durationSec: Long = 0L
)

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val friendId: String,
    val friendName: String,
    val avatarUrl: String = "",
    val isOnline: Boolean = false,
    val currentRoomId: String? = null
)

@Entity(tableName = "room_messages")
data class RoomMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystemMessage: Boolean = false
)

@Entity(tableName = "stream_rooms")
data class StreamRoom(
    @PrimaryKey val roomId: String,
    val roomName: String,
    val adminId: String,
    val adminName: String,
    val currentMovieUrl: String = "",
    val currentMovieTitle: String = "",
    val isMoviePlaying: Boolean = false,
    val playbackStartTime: Long = 0L, // timestamp when play clicked
    val participantCount: Int = 1
)

@Entity(tableName = "join_requests")
data class JoinRequest(
    @PrimaryKey val requestId: String,
    val roomId: String,
    val userId: String,
    val username: String,
    val email: String,
    val status: String // "PENDING", "APPROVED", "REJECTED"
)
