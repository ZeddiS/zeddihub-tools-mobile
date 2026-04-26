package com.zeddihub.mobile.ui.helpers

import android.content.Context
import android.hardware.ConsumerIrManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R

/**
 * Multi-Remote — IR blaster control surface.
 *
 * Most modern phones don't ship with a ConsumerIrManager (Samsung
 * killed it after the Galaxy S6, and most Pixel/Xiaomi/OnePlus never
 * had one). When the API isn't there we say so up front — there's
 * no point pretending to send commands that won't reach a TV.
 *
 * For phones that DO have IR, we ship a small built-in catalogue of
 * NEC-protocol codes for the most common power / volume / channel
 * functions across half a dozen TV brands. Real LIRC-database depth
 * (~6000 device profiles, each ~10 KB) lands in v1.0.0 as a bundled
 * asset; the v0.9.0 catalogue is what fits in the source file.
 *
 * Network-side control (Wake-on-LAN, ADB, Cast, vendor APIs like
 * Samsung WSS) is not in scope here — that's a different surface
 * (server pairing, certificates, etc.) that deserves its own screen
 * once we have a TV-app companion.
 */
@Composable
fun MultiRemoteScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val ir = remember { ctx.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager }
    var brand by remember { mutableStateOf(IrBrand.Samsung) }
    var lastSent by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.mr_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (ir == null || !ir.hasIrEmitter()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.mr_no_ir_title),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(stringResource(R.string.mr_no_ir_body),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            return@Column
        }

        Text(stringResource(R.string.mr_brand_label), fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(IrBrand.values().toList()) { b ->
                AssistChip(onClick = { brand = b }, label = { Text(b.label) })
            }
        }

        Text(stringResource(R.string.mr_buttons_label),
            fontWeight = FontWeight.SemiBold)

        val freq = brand.carrierHz
        for (rowKeys in brand.layout) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (k in rowKeys) {
                    Button(onClick = {
                        runCatching {
                            ir.transmit(freq, k.pattern)
                            lastSent = "${brand.label} – ${k.label}"
                        }
                    }) { Text(k.label) }
                }
            }
        }

        lastSent?.let {
            Text("✓ $it", color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class IrKey(val label: String, val pattern: IntArray)

/**
 * NEC protocol pattern (38 kHz carrier): 9000 µs lead high, 4500 low,
 * then 32 data bits where 1 = 560/1690 µs, 0 = 560/560 µs, ending in
 * a 560 µs trailing pulse. Only patterns commonly bundled with TVs
 * shipped 2010-onwards live below — anything wildly older is rare on
 * the IR-bearing phone install base.
 *
 * We bake the patterns as compile-time IntArrays so transmit() is a
 * single syscall — no math, no allocation in the click handler.
 */
private fun nec(addr: Int, cmd: Int): IntArray {
    val bits = mutableListOf<Int>()
    bits.add(9000); bits.add(4500)
    fun byte(v: Int) {
        for (b in 0 until 8) {
            bits.add(560)
            bits.add(if (((v shr b) and 1) == 1) 1690 else 560)
        }
    }
    byte(addr)
    byte(addr.inv() and 0xff)
    byte(cmd)
    byte(cmd.inv() and 0xff)
    bits.add(560)
    return bits.toIntArray()
}

private enum class IrBrand(val label: String, val carrierHz: Int, val layout: List<List<IrKey>>) {
    Samsung("Samsung", 38000, listOf(
        listOf(IrKey("⏻", nec(0x07, 0x02)), IrKey("Mute", nec(0x07, 0x0F))),
        listOf(IrKey("Vol+", nec(0x07, 0x07)), IrKey("Vol-", nec(0x07, 0x0B))),
        listOf(IrKey("Ch+", nec(0x07, 0x12)), IrKey("Ch-", nec(0x07, 0x10))),
        listOf(IrKey("Source", nec(0x07, 0x01))),
    )),
    Sony("Sony", 40000, listOf(
        listOf(IrKey("⏻", nec(0x01, 0x15))),
        listOf(IrKey("Vol+", nec(0x01, 0x12)), IrKey("Vol-", nec(0x01, 0x13))),
        listOf(IrKey("Ch+", nec(0x01, 0x10)), IrKey("Ch-", nec(0x01, 0x11))),
    )),
    LG("LG", 38000, listOf(
        listOf(IrKey("⏻", nec(0x04, 0x08)), IrKey("Mute", nec(0x04, 0x09))),
        listOf(IrKey("Vol+", nec(0x04, 0x02)), IrKey("Vol-", nec(0x04, 0x03))),
        listOf(IrKey("Ch+", nec(0x04, 0x00)), IrKey("Ch-", nec(0x04, 0x01))),
    )),
    Panasonic("Panasonic", 36700, listOf(
        listOf(IrKey("⏻", nec(0x40, 0x3D)), IrKey("Mute", nec(0x40, 0x32))),
        listOf(IrKey("Vol+", nec(0x40, 0x20)), IrKey("Vol-", nec(0x40, 0x21))),
        listOf(IrKey("Ch+", nec(0x40, 0x22)), IrKey("Ch-", nec(0x40, 0x23))),
    )),
    Philips("Philips", 36000, listOf(
        listOf(IrKey("⏻", nec(0x00, 0x0C))),
        listOf(IrKey("Vol+", nec(0x00, 0x10)), IrKey("Vol-", nec(0x00, 0x11))),
        listOf(IrKey("Ch+", nec(0x00, 0x20)), IrKey("Ch-", nec(0x00, 0x21))),
    )),
    Generic("Generic NEC", 38000, listOf(
        listOf(IrKey("Power", nec(0x00, 0x45)), IrKey("Mute", nec(0x00, 0x4C))),
        listOf(IrKey("Vol+", nec(0x00, 0x46)), IrKey("Vol-", nec(0x00, 0x15))),
        listOf(IrKey("Ch+", nec(0x00, 0x47)), IrKey("Ch-", nec(0x00, 0x44))),
    )),
}
