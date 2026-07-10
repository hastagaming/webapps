package com.web.apps.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.web.apps.ui.common.MarkdownText
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val markdownContent: String
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        markdownContent = """
            # Welcome to WebApps 👋

            Run multiple websites as **isolated containers**, each with its own:

            - Cookies & sessions
            - Local storage
            - Login state

            No more mixing accounts between tabs.
        """.trimIndent()
    ),
    OnboardingPage(
        markdownContent = """
            # Organize with Groups 📁

            Keep things tidy:

            - Create **Groups** to bundle related containers
            - **Pin** your favorites to the top
            - Reorder containers with **Move Up / Move Down**
            - Move containers between groups anytime

            ~~Messy tab chaos~~ → Organized workspace.
        """.trimIndent()
    ),
    OnboardingPage(
        markdownContent = """
            # Stays Running 🔄

            WebApps keeps your containers alive via a **Foreground Service**:

            - [x] Sessions persist in the background
            - [x] No unexpected logouts
            - [x] Notification shows active containers

            Just like Termux's persistent session.
        """.trimIndent()
    ),
    OnboardingPage(
        markdownContent = """
            # Sign In to Sync ☁️

            Sign in to back up your data:

            | Feature | Benefit |
            |---|---|
            | Cloud Sync | Never lose your setup |
            | Multi-device | Access from anywhere |
            | Auto Backup | Peace of mind |

            Ready to get started?
        """.trimIndent()
    )
)

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val item = ONBOARDING_PAGES[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MarkdownText(
                        markdown = item.markdownContent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(ONBOARDING_PAGES.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                            .fillMaxWidth(if (isSelected) 0.06f else 0.03f)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onFinished) {
                    Text("Skip")
                }

                Button(onClick = {
                    if (pagerState.currentPage < ONBOARDING_PAGES.lastIndex) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinished()
                    }
                }) {
                    Text(if (pagerState.currentPage == ONBOARDING_PAGES.lastIndex) "Get Started" else "Next")
                }
            }
        }
    }
}