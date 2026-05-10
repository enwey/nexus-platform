import Foundation
import WebKit

final class NexusSchemeHandler: NSObject, WKURLSchemeHandler {
    private let fileResolver: SandboxFileResolver
    private var activeTaskIDs = Set<ObjectIdentifier>()
    private var cancelledTaskIDs = Set<ObjectIdentifier>()
    private let lock = NSLock()

    init(fileResolver: SandboxFileResolver = SandboxFileResolver()) {
        self.fileResolver = fileResolver
        super.init()
    }

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        let taskID = ObjectIdentifier(urlSchemeTask)
        markActive(taskID: taskID)
        defer { clearState(taskID: taskID) }

        guard let requestURL = urlSchemeTask.request.url else {
            urlSchemeTask.didFailWithError(URLError(.badURL))
            return
        }

        guard let fileURL = fileResolver.resolveFileURL(for: requestURL) else {
            let response = HTTPURLResponse(url: requestURL, statusCode: 404, httpVersion: "HTTP/1.1", headerFields: nil)
            if let response {
                urlSchemeTask.didReceive(response)
            }
            urlSchemeTask.didFailWithError(URLError(.fileDoesNotExist))
            return
        }

        do {
            let values = try fileURL.resourceValues(forKeys: [.fileSizeKey])
            let data = try Data(contentsOf: fileURL, options: [.mappedIfSafe])
            guard !isCancelled(taskID: taskID) else {
                return
            }

            var headers = ["Content-Type": MIMETypeResolver.resolve(for: fileURL.pathExtension)]
            if let fileSize = values.fileSize, fileSize > 0 {
                headers["Content-Length"] = String(fileSize)
            }
            let response = HTTPURLResponse(
                url: requestURL,
                statusCode: 200,
                httpVersion: "HTTP/1.1",
                headerFields: headers
            )

            if let response {
                urlSchemeTask.didReceive(response)
            }
            urlSchemeTask.didReceive(data)
            urlSchemeTask.didFinish()
        } catch {
            urlSchemeTask.didFailWithError(error)
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        let taskID = ObjectIdentifier(urlSchemeTask)
        lock.lock()
        if activeTaskIDs.contains(taskID) {
            cancelledTaskIDs.insert(taskID)
        } else {
            cancelledTaskIDs.remove(taskID)
        }
        lock.unlock()
    }

    private func markActive(taskID: ObjectIdentifier) {
        lock.lock()
        activeTaskIDs.insert(taskID)
        cancelledTaskIDs.remove(taskID)
        lock.unlock()
    }

    private func clearState(taskID: ObjectIdentifier) {
        lock.lock()
        activeTaskIDs.remove(taskID)
        cancelledTaskIDs.remove(taskID)
        lock.unlock()
    }

    private func isCancelled(taskID: ObjectIdentifier) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return cancelledTaskIDs.contains(taskID)
    }
}
