package com.zeddihub.mobile.data.reminders

import org.json.JSONArray
import org.json.JSONObject

/**
 * Single reminder rule.
 *
 * The trigger discriminator (`trigger.kind`) decides which fields are
 * meaningful — keep this in mind when serialising. We don't enforce
 * variant exclusivity at the type level on purpose: serialising a
 * sealed hierarchy through a hand-rolled JSON encoder costs more code
 * than the safety pays back in a 200-LOC store.
 */
data class Reminder(
    val id: String,
    val title: String,
    val body: String,
    val enabled: Boolean,
    val trigger: ReminderTrigger,
    val createdAt: Long,
)

sealed class ReminderTrigger {
    /** One-shot at a wall-clock instant. */
    data class TimeAt(val epochMs: Long) : ReminderTrigger()

    /**
     * Recurring weekly. `daysMask` packs Mon=bit0..Sun=bit6;
     * `minuteOfDay` is local time.
     */
    data class TimeWeekly(val daysMask: Int, val minuteOfDay: Int) : ReminderTrigger()

    /** Geofence — fires on enter or exit. */
    data class Geofence(
        val lat: Double, val lng: Double, val radiusM: Float,
        val onEnter: Boolean, val onExit: Boolean
    ) : ReminderTrigger()

    /** WiFi SSID match on connect / disconnect. */
    data class WifiSsid(val ssid: String, val onConnect: Boolean, val onDisconnect: Boolean) : ReminderTrigger()

    /** Bluetooth device address match on connect / disconnect. */
    data class BluetoothDevice(val address: String, val name: String, val onConnect: Boolean, val onDisconnect: Boolean) : ReminderTrigger()

    /** Weather match — checked periodically via Open-Meteo. */
    data class Weather(
        val lat: Double, val lng: Double,
        val condition: WeatherCondition, val operator: ComparisonOp, val threshold: Float,
    ) : ReminderTrigger()
}

enum class WeatherCondition { TEMP_C, RAIN_MM, WIND_KMH, HUMIDITY_PCT }
enum class ComparisonOp { GREATER_THAN, LESS_THAN }

object ReminderJson {
    fun toJson(list: List<Reminder>): String {
        val arr = JSONArray()
        for (r in list) arr.put(reminderToJson(r))
        return arr.toString()
    }

    fun fromJson(text: String): List<Reminder> = runCatching {
        val arr = JSONArray(text)
        (0 until arr.length()).mapNotNull { jsonToReminder(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun reminderToJson(r: Reminder) = JSONObject().apply {
        put("id", r.id)
        put("title", r.title)
        put("body", r.body)
        put("enabled", r.enabled)
        put("createdAt", r.createdAt)
        put("trigger", triggerToJson(r.trigger))
    }

    private fun triggerToJson(t: ReminderTrigger): JSONObject = when (t) {
        is ReminderTrigger.TimeAt -> JSONObject()
            .put("kind", "time_at").put("epoch", t.epochMs)
        is ReminderTrigger.TimeWeekly -> JSONObject()
            .put("kind", "time_weekly").put("days", t.daysMask).put("minute", t.minuteOfDay)
        is ReminderTrigger.Geofence -> JSONObject()
            .put("kind", "geofence").put("lat", t.lat).put("lng", t.lng)
            .put("radius", t.radiusM.toDouble())
            .put("enter", t.onEnter).put("exit", t.onExit)
        is ReminderTrigger.WifiSsid -> JSONObject()
            .put("kind", "wifi").put("ssid", t.ssid)
            .put("connect", t.onConnect).put("disconnect", t.onDisconnect)
        is ReminderTrigger.BluetoothDevice -> JSONObject()
            .put("kind", "bt").put("address", t.address).put("name", t.name)
            .put("connect", t.onConnect).put("disconnect", t.onDisconnect)
        is ReminderTrigger.Weather -> JSONObject()
            .put("kind", "weather").put("lat", t.lat).put("lng", t.lng)
            .put("condition", t.condition.name)
            .put("op", t.operator.name)
            .put("threshold", t.threshold.toDouble())
    }

    private fun jsonToReminder(obj: JSONObject): Reminder? = runCatching {
        Reminder(
            id = obj.getString("id"),
            title = obj.optString("title"),
            body = obj.optString("body"),
            enabled = obj.optBoolean("enabled", true),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            trigger = jsonToTrigger(obj.getJSONObject("trigger")) ?: return@runCatching null,
        )
    }.getOrNull()

    private fun jsonToTrigger(obj: JSONObject): ReminderTrigger? = when (obj.optString("kind")) {
        "time_at" -> ReminderTrigger.TimeAt(obj.getLong("epoch"))
        "time_weekly" -> ReminderTrigger.TimeWeekly(obj.getInt("days"), obj.getInt("minute"))
        "geofence" -> ReminderTrigger.Geofence(
            obj.getDouble("lat"), obj.getDouble("lng"),
            obj.getDouble("radius").toFloat(),
            obj.getBoolean("enter"), obj.getBoolean("exit"),
        )
        "wifi" -> ReminderTrigger.WifiSsid(
            obj.getString("ssid"),
            obj.optBoolean("connect", true), obj.optBoolean("disconnect", false)
        )
        "bt" -> ReminderTrigger.BluetoothDevice(
            obj.getString("address"), obj.optString("name"),
            obj.optBoolean("connect", true), obj.optBoolean("disconnect", false)
        )
        "weather" -> ReminderTrigger.Weather(
            obj.getDouble("lat"), obj.getDouble("lng"),
            runCatching { WeatherCondition.valueOf(obj.getString("condition")) }
                .getOrDefault(WeatherCondition.TEMP_C),
            runCatching { ComparisonOp.valueOf(obj.getString("op")) }
                .getOrDefault(ComparisonOp.GREATER_THAN),
            obj.getDouble("threshold").toFloat(),
        )
        else -> null
    }
}
