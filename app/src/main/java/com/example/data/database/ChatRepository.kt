package com.example.data.database

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun insertSession(session: ChatSession): Long {
        return chatDao.insertSession(session)
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun deleteSessionWithMessages(sessionId: Long) {
        chatDao.deleteSessionWithMessages(sessionId)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        chatDao.updateSessionTitle(sessionId, newTitle)
    }

    suspend fun insertUser(user: User): Long {
        return chatDao.insertUser(user)
    }

    suspend fun getUserByEmail(email: String): User? {
        return chatDao.getUserByEmail(email)
    }

    suspend fun getUserByEmailAndProvider(email: String, provider: String): User? {
        return chatDao.getUserByEmailAndProvider(email, provider)
    }

    suspend fun getUserById(id: Long): User? {
        return chatDao.getUserById(id)
    }
}
