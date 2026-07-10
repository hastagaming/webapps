package com.web.apps.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val description: String
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        title = "Welcome to WebApps",
        description = "Run multiple websites as isolated containers, each with its own cookies, sessions, and settings."
    ),
    OnboardingPage(
        title = "Organize with Groups",
        description = "Group your containers together and pin your favorites for quick access."
    ),
    OnboardingPage(
        title = "Stays Running",
        description = "Containers keep running in the background via a foreground service, so you never lose your session."
    ),
    OnboardingPage(
        title = "Sign In to Sync",
        description = "Sign in to back up your containers and groups to the cloud, so they're never lost."
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
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
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                            .fillMaxWidth(if (isSelected) 0.06f else 0.03f)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
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