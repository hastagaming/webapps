package com.web.apps.core.security

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafeBrowsingChecker @Inject constructor() {

    private val knownDangerousPatterns = setOf(
        "malware-test.com",
        "phishing-test.com",
        "eicar.org/download"
    )

    private val suspiciousTlds = setOf(".zip", ".mov", ".xyz.ru")

    fun isKnownDangerous(url: String): Boolean {
        val host = try {
            URI(url).host?.lowercase() ?: return false
        } catch (e: Exception) {
            return true
        }

        if (knownDangerousPatterns.any { host.contains(it) }) {
            return true
        }

        if (containsHomographAttack(host)) {
            return true
        }

        return false
    }

    private fun containsHomographAttack(host: String): Boolean {
        val cyrillicRange = '\u0400'..'\u04FF'
        return host.any { it in cyrillicRange }
    }

    fun isSuspiciousDownloadUrl(url: String): Boolean {
        return suspiciousTlds.any { url.endsWith(it, ignoreCase = true) }
    }
}