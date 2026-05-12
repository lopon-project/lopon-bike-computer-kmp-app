import Foundation
import Combine
import ComposeApp

final class FlowJob {
    private var handle: FlowCancelHandle?

    init(_ handle: FlowCancelHandle?) {
        self.handle = handle
    }

    deinit { cancel() }

    func cancel() {
        handle?.cancel()
        handle = nil
    }
}

@discardableResult
func observe<T>(_ flow: Any, onValue: @escaping (T) -> Void) -> FlowJob {
    let handle = FlowAdapterKt.collectFlowAny(flow: flow) { value in
        if let typed = value as? T {
            DispatchQueue.main.async { onValue(typed) }
        } else if value == nil, let nilTyped = (Optional<Any>.none as? T) {
            DispatchQueue.main.async { onValue(nilTyped) }
        }
    }
    return FlowJob(handle)
}
