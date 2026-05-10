package ru.lopon.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object FlowAdapter {

    val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

fun <T> collectFlow(flow: Flow<T>, onValue: (T) -> Unit): Job =
    flow.onEach { onValue(it) }.launchIn(FlowAdapter.mainScope)
