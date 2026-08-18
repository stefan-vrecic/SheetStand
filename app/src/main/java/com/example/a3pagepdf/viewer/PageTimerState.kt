package com.example.a3pagepdf.viewer

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * State for the "jump to next page after N seconds" timer feature, with
 * per-page durations. Setting page 1's duration also propagates as the
 * default for any later page that hasn't been manually overridden.
 */
class PageTimerState {
    var isActive by mutableStateOf(false)
    val pageDurations = mutableStateMapOf<Int, Int>()
    private val manuallySetPages = mutableStateSetOf<Int>()
    var timeRemaining by mutableIntStateOf(0)
    var menuExpanded by mutableStateOf(false)

    fun setPageDuration(page: Int, seconds: Int, pageCount: Int) {
        pageDurations[page] = seconds
        if (page == 1) {
            manuallySetPages.add(1)
            for (p in 2..pageCount) {
                if (!manuallySetPages.contains(p)) {
                    pageDurations[p] = seconds
                }
            }
        } else {
            manuallySetPages.add(page)
        }
    }

    fun clearPageDuration(page: Int) {
        pageDurations.remove(page)
    }

    fun durationForPage(page: Int): Int = pageDurations[page] ?: 20
}

@Composable
fun rememberPageTimerState(): PageTimerState = remember { PageTimerState() }

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/** Drives the countdown-and-jump loop as a side effect. */
@Composable
fun PageTimerEffect(
    state: PageTimerState,
    listState: LazyListState,
    currentVisiblePage: Int,
    pageCount: Int
) {
    LaunchedEffect(state.isActive, currentVisiblePage) {
        if (!state.isActive) return@LaunchedEffect
        state.timeRemaining = state.durationForPage(currentVisiblePage)
        while (state.isActive && state.timeRemaining > 0) {
            delay(1000)
            state.timeRemaining--
        }
        if (state.isActive && state.timeRemaining <= 0) {
            if (currentVisiblePage < pageCount) {
                listState.scrollToItem(currentVisiblePage)
            } else {
                state.isActive = false
            }
        }
    }
}