package com.codesteward.health

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
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
class HealthChecker : Disposable {
    private var timer: Timer? = null
    private var lastStatus = HealthStatus(reachable = false)
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
                    HealthStatus(
                        reachable = true,
                        version = parsed["version"] as? String,
                    )
                } catch (_: Exception) {
                    HealthStatus(reachable = true)
                }
            } else {
                HealthStatus(reachable = false, error = "HTTP $responseCode")
            }
        } catch (e: Exception) {
            HealthStatus(reachable = false, error = e.message)
        }
    }

    override fun dispose() {
        stop()
        listeners.clear()
    }

    companion object {
        fun getInstance(project: Project): HealthChecker =
            project.getService(HealthChecker::class.java)
    }
}
