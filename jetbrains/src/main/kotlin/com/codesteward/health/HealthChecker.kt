package com.codesteward.health

import com.google.gson.Gson
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.net.HttpURLConnection
import java.net.URI
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CopyOnWriteArrayList

data class HealthStatus(
    val reachable: Boolean,
    val version: String? = null,
    val error: String? = null,
)

fun interface HealthStatusListener {
    fun onStatusChange(status: HealthStatus)
}

@Service(Service.Level.PROJECT)
class HealthChecker(private val project: Project) : Disposable {
    private var timer: Timer? = null
    private var lastStatus = HealthStatus(reachable = false, error = "Not checked yet")
    private var hasNotifiedFailure = false
    private val listeners = CopyOnWriteArrayList<HealthStatusListener>()
    private val gson = Gson()

    fun addListener(listener: HealthStatusListener) {
        listeners.add(listener)
    }

    fun getLastStatus(): HealthStatus = lastStatus

    fun start(proxyUrl: String, intervalSeconds: Int) {
        stop()
        hasNotifiedFailure = false
        LOG.info("Starting health checks against $proxyUrl every ${intervalSeconds}s")

        timer = Timer("CodestewardHealthCheck", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    val status = checkHealth(proxyUrl)
                    val changed = status.reachable != lastStatus.reachable ||
                            status.version != lastStatus.version
                    lastStatus = status

                    if (changed) {
                        listeners.forEach { it.onStatusChange(status) }
                    }

                    if (!status.reachable && !hasNotifiedFailure) {
                        hasNotifiedFailure = true
                        notifyError(proxyUrl, status.error)
                    }

                    if (status.reachable) {
                        hasNotifiedFailure = false
                    }
                }
            }, 0L, intervalSeconds * 1000L)
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }

    fun checkHealth(proxyUrl: String): HealthStatus {
        LOG.debug("Checking health at $proxyUrl/healthz")
        return try {
            val url = URI("$proxyUrl/healthz").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                try {
                    val parsed = gson.fromJson(body, Map::class.java)
                    val status = HealthStatus(
                        reachable = true,
                        version = parsed["version"] as? String,
                    )
                    LOG.info("Proxy healthy: ${status.version ?: "no version"}")
                    status
                } catch (_: Exception) {
                    LOG.info("Proxy reachable but response not parseable")
                    HealthStatus(reachable = true)
                }
            } else {
                val error = "HTTP $responseCode"
                LOG.warn("Proxy health check failed: $error")
                HealthStatus(reachable = false, error = error)
            }
        } catch (e: Exception) {
            val error = "${e.javaClass.simpleName}: ${e.message}"
            LOG.warn("Proxy health check failed: $error")
            HealthStatus(reachable = false, error = error)
        }
    }

    private fun notifyError(proxyUrl: String, error: String?) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Codesteward")
                .createNotification(
                    "Codesteward: Proxy Unreachable",
                    "Cannot connect to $proxyUrl — ${error ?: "unknown error"}. " +
                            "Check that the proxy is running and the URL is correct.",
                    NotificationType.WARNING,
                )
                .notify(project)
        }
    }

    override fun dispose() {
        stop()
        listeners.clear()
    }

    companion object {
        private val LOG = Logger.getInstance(HealthChecker::class.java)

        fun getInstance(project: Project): HealthChecker =
            project.getService(HealthChecker::class.java)
    }
}
