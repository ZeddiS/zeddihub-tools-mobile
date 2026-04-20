package com.zeddihub.mobile.fcm

/**
 * Ready-to-enable FCM scaffold.
 *
 * Steps to activate:
 *   1. Add `google-services.json` to `app/` directory.
 *   2. Uncomment the Firebase plugin and dependencies in `app/build.gradle.kts`.
 *   3. Uncomment the <service> entry in `AndroidManifest.xml`.
 *   4. Uncomment the class body below and remove the stub.
 *
 * When activated, the service receives FCM pushes from the desktop/server
 * and writes them to the local AlertRepository — the UI reacts via Flow and
 * a system notification is posted through the "alerts" channel.
 *
 * Expected FCM data payload (non-notification message):
 *   severity: "info" | "warn" | "critical"
 *   source:   "desktop" | "server" | "system"
 *   title:    String
 *   body:     String
 *   ts:       Long (millis, optional)
 */

/*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zeddihub.mobile.data.alerts.AlertRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ZeddiFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var alerts: AlertRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(msg: RemoteMessage) {
        val d = msg.data
        val severity = d["severity"] ?: "info"
        val source = d["source"] ?: "server"
        val title = d["title"] ?: msg.notification?.title ?: "ZeddiHub"
        val body = d["body"] ?: msg.notification?.body.orEmpty()
        val ts = d["ts"]?.toLongOrNull() ?: System.currentTimeMillis()
        scope.launch { alerts.ingest(severity, source, title, body, ts) }
    }

    override fun onNewToken(token: String) {
        // TODO: send token to ZeddiHub server so it can push to this device
    }
}
*/
