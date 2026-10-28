package com.example.enkan_haban.ui.sell

import androidx.compose.runtime.Composable

/** Most photos a seller can attach to one ad. */
const val MAX_PHOTOS = 10

/** Longest side, in pixels, that picked photos are scaled down to before they are kept. */
const val PHOTO_MAX_SIDE = 1600

/**
 * Remembers a system photo picker. Calling the returned function opens the picker, letting the
 * user choose up to `maxItems` images. [onPicked] receives each picked image as JPEG bytes,
 * already scaled down to [PHOTO_MAX_SIDE]. Images that fail to load are skipped.
 */
@Composable
expect fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): (maxItems: Int) -> Unit
