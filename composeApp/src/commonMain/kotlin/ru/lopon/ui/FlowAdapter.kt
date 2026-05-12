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

class FlowCancelHandle internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

@Suppress("UNCHECKED_CAST")
fun collectFlowAny(flow: Any, onValue: (Any?) -> Unit): FlowCancelHandle {
    val typed = flow as Flow<Any?>
    val job = typed.onEach { onValue(it) }.launchIn(FlowAdapter.mainScope)
    return FlowCancelHandle(job)
}
