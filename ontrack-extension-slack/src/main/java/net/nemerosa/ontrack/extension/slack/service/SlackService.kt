package net.nemerosa.ontrack.extension.slack.service

interface SlackService {

    fun sendNotification(channel: String, message: String, iconEmoji: String? = null): Boolean

}