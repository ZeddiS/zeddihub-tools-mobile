package com.zeddihub.mobile.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Finds other ZeddiHub-branded packages installed on the device that share
 * the same applicationId "family" but are NOT this app's own package.
 *
 * A "duplicate install" is typically:
 *   • an older APK uploaded with a different signing key that was kept when
 *     the user sideloaded this newer release (Android could not upgrade it
 *     because the signatures don't match, so it installed alongside),
 *   • a leftover debug build on a release install (or vice versa),
 *   • a package cloned by an OEM "dual apps / parallel space" feature.
 *
 * We treat everything whose base package (with a trailing ".debug" stripped)
 * matches our own base package as "us", and everything else under the
 * com.zeddihub.* namespace as a duplicate that should be uninstalled.
 */
object DuplicateInstallDetector {

    data class Duplicate(
        val packageName: String,
        val label: String
    )

    fun find(context: Context): List<Duplicate> {
        val pm = context.packageManager
        val selfPkg = context.packageName
        val selfBase = selfPkg.removeSuffix(".debug")

        return runCatching {
            @Suppress("DEPRECATION")
            val installed = pm.getInstalledPackages(0)
            installed.asSequence()
                .map { it.packageName }
                .filter { it != selfPkg }
                .filter { it.startsWith("com.zeddihub.") }
                .filter { it.removeSuffix(".debug") != selfBase }
                .map { pkg ->
                    val label = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    }.getOrDefault(pkg)
                    Duplicate(pkg, label)
                }
                .toList()
        }.getOrDefault(emptyList())
    }

    /**
     * Launch the system uninstall prompt for the given package. Returns false
     * if no uninstall activity could be resolved.
     */
    fun requestUninstall(context: Context, packageName: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
