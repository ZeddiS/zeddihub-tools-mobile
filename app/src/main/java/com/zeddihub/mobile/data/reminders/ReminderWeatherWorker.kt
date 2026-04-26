package com.zeddihub.mobile.data.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zeddihub.mobile.data.reminders.ComparisonOp
import com.zeddihub.mobile.data.reminders.WeatherCondition
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Periodic job that polls Open-Meteo for one weather-triggered reminder
 * and fires the notification when the configured condition becomes true.
 *
 * Why Open-Meteo: free, no API key, generous rate limit, trustworthy
 * forecast data. We hit the 'current' endpoint (not the forecast) so we
 * react to *now*, not predicted future weather — predictive triggers
 * could go either way ("warn me before rain") and would deserve a
 * separate UI mode rather than overloading this trigger.
 *
 * Deduplication: the worker stores a 'last fire timestamp' per reminder
 * in app prefs, and refuses to fire again within an hour. Without this
 * a reminder set to "TEMP_C > 30 °C" would fire on every 15-min poll
 * during a heatwave, which is exactly the spam pattern we want to avoid.
 */
@HiltWorker
class ReminderWeatherWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val store: ReminderStore,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val rule = store.load().firstOrNull { it.id == id } ?: return Result.success()
        if (!rule.enabled) return Result.success()
        val t = rule.trigger as? ReminderTrigger.Weather ?: return Result.success()

        val sample = fetchCurrent(t.lat, t.lng) ?: return Result.retry()
        val current = when (t.condition) {
            WeatherCondition.TEMP_C -> sample.tempC
            WeatherCondition.RAIN_MM -> sample.precipMm
            WeatherCondition.WIND_KMH -> sample.windKmh
            WeatherCondition.HUMIDITY_PCT -> sample.humidity
        } ?: return Result.success()

        val matches = when (t.operator) {
            ComparisonOp.GREATER_THAN -> current > t.threshold
            ComparisonOp.LESS_THAN -> current < t.threshold
        }
        if (!matches) return Result.success()

        val prefs = applicationContext.getSharedPreferences("reminder_weather_dedup", Context.MODE_PRIVATE)
        val last = prefs.getLong(id, 0L)
        val now = System.currentTimeMillis()
        if (now - last < TimeUnit.HOURS.toMillis(1)) return Result.success()
        prefs.edit().putLong(id, now).apply()

        scheduler.postNotification(rule)
        return Result.success()
    }

    private fun fetchCurrent(lat: Double, lng: Double): WeatherSample? {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lng" +
            "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m"
        val req = Request.Builder().url(url).build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val current = JSONObject(body).optJSONObject("current") ?: return@use null
                WeatherSample(
                    tempC = current.opt("temperature_2m")?.toFloatOrNull(),
                    humidity = current.opt("relative_humidity_2m")?.toFloatOrNull(),
                    precipMm = current.opt("precipitation")?.toFloatOrNull(),
                    windKmh = current.opt("wind_speed_10m")?.toFloatOrNull(),
                )
            }
        }.getOrNull()
    }

    private data class WeatherSample(
        val tempC: Float?, val humidity: Float?,
        val precipMm: Float?, val windKmh: Float?,
    )

    companion object {
        const val KEY_ID = "id"
    }
}

private fun Any?.toFloatOrNull(): Float? = when (this) {
    is Number -> toFloat()
    is String -> toFloatOrNull()
    else -> null
}
