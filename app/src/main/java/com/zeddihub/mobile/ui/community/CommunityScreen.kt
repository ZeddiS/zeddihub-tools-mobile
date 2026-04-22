package com.zeddihub.mobile.ui.community

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.common.PullToRefreshBox
import kotlinx.coroutines.delay

@Composable
fun CommunityScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    fun open(url: String) {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(600)
            refreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { refreshing = true },
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.community_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Spacer(Modifier.size(16.dp))

        LinkCard(
            icon = Icons.Default.Forum,
            title = stringResource(R.string.community_discord),
            desc = stringResource(R.string.community_discord_desc),
            onClick = { open(BuildConfig.DISCORD_URL) }
        )
        Spacer(Modifier.size(10.dp))
        LinkCard(
            icon = Icons.Default.Language,
            title = stringResource(R.string.community_web),
            desc = stringResource(R.string.community_web_desc),
            onClick = { open("https://zeddihub.eu/") }
        )
        Spacer(Modifier.size(10.dp))
        LinkCard(
            icon = Icons.Default.Code,
            title = stringResource(R.string.community_github),
            desc = stringResource(R.string.community_github_desc),
            onClick = { open("https://github.com/ZeddiS/zeddihub-tools-mobile") }
        )
        Spacer(Modifier.size(10.dp))
        LinkCard(
            icon = Icons.Default.Favorite,
            title = stringResource(R.string.community_donate),
            desc = stringResource(R.string.community_donate_desc),
            onClick = { open("https://zeddihub.eu/donate") }
        )
    }
    }
}

@Composable
private fun LinkCard(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(icon, contentDescription = null, tint = colors.primary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                Text(desc, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
