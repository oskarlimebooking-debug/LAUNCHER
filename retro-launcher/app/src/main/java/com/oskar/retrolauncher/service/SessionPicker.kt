package com.oskar.retrolauncher.service

/**
 * Pure session-selection policy, extracted so it can be unit-tested without a
 * live `MediaController` / `NotificationListenerService`.
 *
 * Default behaviour (unchanged from the original listener): prefer the first
 * playing session, else the first available. When [preferOwn] is on (the
 * launcher's built-in player is enabled), a *playing* session owned by the
 * launcher itself wins the tiebreak — so "open my player and hit play" reliably
 * binds the launcher's own session even when an external app also has one.
 */
fun <T> pickSession(
    sessions: List<T>,
    isPlaying: (T) -> Boolean,
    isOwn: (T) -> Boolean,
    preferOwn: Boolean,
): T? {
    if (preferOwn) {
        sessions.firstOrNull { isOwn(it) && isPlaying(it) }?.let { return it }
    }
    return sessions.firstOrNull(isPlaying) ?: sessions.firstOrNull()
}
