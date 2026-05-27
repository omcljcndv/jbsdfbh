package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfileSync(): UserProfile?
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY friendName ASC")
    fun getAllFriends(): Flow<List<Friend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Query("DELETE FROM friends WHERE friendId = :friendId")
    suspend fun deleteFriend(friendId: String)
    
    @Query("SELECT * FROM friends WHERE friendId = :friendId LIMIT 1")
    suspend fun getFriendById(friendId: String): Friend?
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM room_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<RoomMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: RoomMessage)

    @Query("DELETE FROM room_messages WHERE roomId = :roomId")
    suspend fun clearMessages(roomId: String)
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM stream_rooms")
    fun getAllRooms(): Flow<List<StreamRoom>>

    @Query("SELECT * FROM stream_rooms WHERE roomId = :roomId LIMIT 1")
    fun getRoomById(roomId: String): Flow<StreamRoom?>

    @Query("SELECT * FROM stream_rooms WHERE roomId = :roomId LIMIT 1")
    suspend fun getRoomSync(roomId: String): StreamRoom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: StreamRoom)

    @Query("DELETE FROM stream_rooms WHERE roomId = :roomId")
    suspend fun deleteRoom(roomId: String)
}

@Dao
interface JoinRequestDao {
    @Query("SELECT * FROM join_requests WHERE roomId = :roomId")
    fun getJoinRequestsForRoom(roomId: String): Flow<List<JoinRequest>>

    @Query("SELECT * FROM join_requests WHERE roomId = :roomId AND userId = :userId LIMIT 1")
    fun getJoinRequestForUserAndRoom(userId: String, roomId: String): Flow<JoinRequest?>

    @Query("SELECT * FROM join_requests WHERE roomId = :roomId AND userId = :userId LIMIT 1")
    suspend fun getJoinRequestForUserAndRoomSync(userId: String, roomId: String): JoinRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoinRequest(request: JoinRequest)

    @Query("UPDATE join_requests SET status = :status WHERE requestId = :requestId")
    suspend fun updateStatus(requestId: String, status: String)

    @Query("DELETE FROM join_requests WHERE requestId = :requestId")
    suspend fun deleteJoinRequest(requestId: String)

    @Query("DELETE FROM join_requests WHERE roomId = :roomId")
    suspend fun deleteJoinRequestsForRoom(roomId: String)
}

@Dao
interface SessionHistoryDao {
    @Query("SELECT * FROM session_history WHERE userId = :userId ORDER BY joinTime DESC")
    fun getSessionHistoryForUser(userId: String): Flow<List<SessionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionHistory(history: SessionHistory): Long

    @Query("SELECT * FROM session_history WHERE userId = :userId AND roomId = :roomId AND leaveTime IS NULL ORDER BY joinTime DESC LIMIT 1")
    suspend fun getActiveSessionHistory(userId: String, roomId: String): SessionHistory?
}
