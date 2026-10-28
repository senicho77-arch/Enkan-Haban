@file:OptIn(ExperimentalForeignApi::class)

package com.example.enkan_haban.ui.sell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@Composable
actual fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): (maxItems: Int) -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    // Held here because the picker keeps only a weak reference to its delegate.
    val delegate = remember { PhotoPickerDelegate { currentOnPicked(it) } }

    return remember(delegate) {
        { maxItems: Int ->
            if (maxItems > 0) {
                val config = PHPickerConfiguration()
                config.selectionLimit = maxItems.toLong()
                config.filter = PHPickerFilter.imagesFilter
                val picker = PHPickerViewController(configuration = config)
                picker.delegate = delegate
                topViewController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private class PhotoPickerDelegate(
    private val onPicked: (List<ByteArray>) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) return

        // Results load in parallel; keep them in the order the user picked them.
        val loaded = arrayOfNulls<ByteArray>(results.size)
        var pending = results.size
        results.forEachIndexed { index, result ->
            result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                val bytes = data?.let { UIImage(data = it) }?.toScaledJpeg()
                dispatch_async(dispatch_get_main_queue()) {
                    loaded[index] = bytes
                    pending--
                    if (pending == 0) onPicked(loaded.filterNotNull())
                }
            }
        }
    }
}

/** Redraws the image at most [PHOTO_MAX_SIDE] on its longest side (this also applies its rotation). */
private fun UIImage.toScaledJpeg(): ByteArray? {
    val (width, height) = size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = minOf(1.0, PHOTO_MAX_SIDE / maxOf(width, height))
    val newWidth = width * scale
    val newHeight = height * scale

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(newWidth, newHeight), false, 1.0)
    drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
    val scaled = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return scaled?.let { UIImageJPEGRepresentation(it, 0.85) }?.toByteArray()
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}
