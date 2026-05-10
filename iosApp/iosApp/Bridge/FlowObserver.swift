import Foundation
import Combine
import ComposeApp

final class FlowJob {
    private var job: Kotlinx_coroutines_coreJob?

    init(_ job: Kotlinx_coroutines_coreJob?) {
        self.job = job
    }

    deinit { cancel() }

    func cancel() {
        job?.cancel(cause: nil)
        job = nil
    }
}

@discardableResult
func observe<T>(_ flow: Kotlinx_coroutines_coreFlow,
                onValue: @escaping (T) -> Void) -> FlowJob {
    let job = FlowAdapterKt.collectFlow(flow: flow) { value in
        if let typed = value as? T {
            DispatchQueue.main.async { onValue(typed) }
        } else if value == nil, let nilTyped = (Optional<Any>.none as? T) {
            DispatchQueue.main.async { onValue(nilTyped) }
        }
    }
    return FlowJob(job)
}
