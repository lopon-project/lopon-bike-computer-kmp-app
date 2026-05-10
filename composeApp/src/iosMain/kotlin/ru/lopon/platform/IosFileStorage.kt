package ru.lopon.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeXML
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosFileStorage : FileStorage {

    private val fileManager = NSFileManager.defaultManager

    override suspend fun readText(path: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val absolute = resolveAbsolutePath(path)
            val content = NSString.stringWithContentsOfFile(
                path = absolute,
                encoding = NSUTF8StringEncoding,
                error = null
            ) as String?
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("Cannot read file: $path"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun writeText(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                val absolute = resolveAbsolutePath(path)
                ensureParentDirectoryExists(absolute)
                val ns = NSString.create(string = content)
                val ok = ns.writeToFile(
                    path = absolute,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null
                )
                if (ok) Result.success(Unit) else Result.failure(Exception("Write failed: $path"))
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.Default) {
        fileManager.fileExistsAtPath(resolveAbsolutePath(path))
    }

    override suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val absolute = resolveAbsolutePath(path)
            if (fileManager.fileExistsAtPath(absolute)) {
                fileManager.removeItemAtPath(absolute, error = null)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(directory: String): List<String> =
        withContext(Dispatchers.Default) {
            val absolute = resolveAbsolutePath(directory)
            if (!fileManager.fileExistsAtPath(absolute)) return@withContext emptyList()
            @Suppress("UNCHECKED_CAST")
            (fileManager.contentsOfDirectoryAtPath(absolute, error = null) as? List<String>)
                ?: emptyList()
        }

    suspend fun importGpxFile(): Result<String> {
        val deferred = CompletableDeferred<Result<String>>()
        val pickerDelegate = DocPickerDelegate(deferred)
        activePickerDelegate = pickerDelegate

        val gpxType = UTType.typeWithFilenameExtension("gpx") ?: UTTypeXML
        val supportedTypes = listOf(gpxType, UTTypeXML)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = supportedTypes)
        picker.delegate = pickerDelegate
        picker.allowsMultipleSelection = false

        val rootController = topMostViewController()
        if (rootController == null) {
            activePickerDelegate = null
            return Result.failure(Exception("No active view controller to present picker"))
        }
        rootController.presentViewController(picker, animated = true, completion = null)

        return try {
            deferred.await()
        } finally {
            activePickerDelegate = null
        }
    }

    fun presentShareSheet(path: String) {
        val absolute = resolveAbsolutePath(path)
        val url = NSURL.fileURLWithPath(absolute)
        val activity = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        topMostViewController()?.presentViewController(activity, animated = true, completion = null)
    }

    private fun resolveAbsolutePath(relative: String): String {
        val docs = documentsDirectoryPath()
        val baseDir = "$docs/lopon"
        return if (relative.startsWith("/")) {
            relative
        } else {
            "$baseDir/$relative"
        }
    }

    private fun documentsDirectoryPath(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        )
        return (paths.firstOrNull() as? String) ?: ""
    }

    private fun ensureParentDirectoryExists(absolutePath: String) {
        val lastSlash = absolutePath.lastIndexOf('/')
        if (lastSlash <= 0) return
        val parent = absolutePath.substring(0, lastSlash)
        if (!fileManager.fileExistsAtPath(parent)) {
            fileManager.createDirectoryAtPath(
                path = parent,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
    }

    private fun topMostViewController(): platform.UIKit.UIViewController? {
        val app = UIApplication.sharedApplication
        val scenes = app.connectedScenes.filterIsInstance<UIWindowScene>()
        val window = scenes.flatMap { (it.windows as List<*>).filterIsInstance<platform.UIKit.UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?: scenes.flatMap { (it.windows as List<*>).filterIsInstance<platform.UIKit.UIWindow>() }
                .firstOrNull()
        var top = window?.rootViewController
        while (true) {
            val presented = top?.presentedViewController ?: break
            top = presented
        }
        return top
    }

    private companion object {
        var activePickerDelegate: DocPickerDelegate? = null
    }

    private class DocPickerDelegate(
        private val completion: CompletableDeferred<Result<String>>
    ) : NSObject(), UIDocumentPickerDelegateProtocol {

        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>
        ) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            controller.dismissViewControllerAnimated(true, completion = null)
            if (url == null) {
                completion.complete(Result.failure(Exception("No file selected")))
                return
            }
            val accessing = url.startAccessingSecurityScopedResource()
            val path = url.path
            if (path == null) {
                if (accessing) url.stopAccessingSecurityScopedResource()
                completion.complete(Result.failure(Exception("File path is null")))
                return
            }
            completion.complete(Result.success(path))
            if (accessing) url.stopAccessingSecurityScopedResource()
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            controller.dismissViewControllerAnimated(true, completion = null)
            completion.complete(Result.failure(Exception("Picker cancelled")))
        }
    }
}
