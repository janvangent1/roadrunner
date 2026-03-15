package com.roadrunner.app.data.local

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationSessionManager @Inject constructor() {
    private var sessionToken: String? = null
    private var sessionExpiresAtMillis: Long = 0L

    fun storeSession(token: String, sessionExpiresAt: String) {
        sessionToken = token
        sessionExpiresAtMillis = Instant.parse(sessionExpiresAt).toEpochMilli()
    }

    fun clearSession() {
        sessionToken = null
        sessionExpiresAtMillis = 0L
    }

    /** Returns true when the server-issued session window has elapsed. Uses System.currentTimeMillis().
     *  This is the grace period check — the server already validated at session start.
     *  Device clock tampering here only extends/shrinks the 1-hour grace; the server blocked new sessions. */
    fun isSessionExpired(): Boolean = sessionToken != null && System.currentTimeMillis() > sessionExpiresAtMillis

    fun hasActiveSession(): Boolean = sessionToken != null && !isSessionExpired()
}
