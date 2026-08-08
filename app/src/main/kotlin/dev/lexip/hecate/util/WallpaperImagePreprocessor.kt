/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

private const val TAG = "WallpaperPreprocessor"
private const val WALLPAPER_DIRECTORY = "wallpaper_sources"
private const val JPEG_QUALITY = 90
private const val MAX_PREPARED_PIXELS = 8_000_000L

internal enum class WallpaperSlot {
	DAY,
	NIGHT
}

/** Prepares a selected image once, so screen-on changes never need the original source. */
internal fun interface WallpaperImagePreparer {
	@Throws(IOException::class)
	fun prepare(source: Uri, slot: WallpaperSlot): Uri
}

internal class WallpaperImagePreprocessor(context: Context) : WallpaperImagePreparer {
	private val appContext = context.applicationContext
	private val contentResolver = appContext.contentResolver
	private val wallpaperManager = WallpaperManager.getInstance(appContext)
	private val outputDirectory = File(appContext.filesDir, WALLPAPER_DIRECTORY)

	override fun prepare(source: Uri, slot: WallpaperSlot): Uri {
		val orientation = readExifOrientation(source)
		val bounds = BitmapFactory.Options().also { options ->
			options.inJustDecodeBounds = true
			contentResolver.openInputStream(source)?.use { stream ->
				BitmapFactory.decodeStream(stream, null, options)
			} ?: throw IOException("Could not open wallpaper source: $source")
		}
		if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
			throw IOException("Wallpaper source is not a supported image: $source")
		}

		val sourceWidth = if (orientation.rotatesDimensions) bounds.outHeight else bounds.outWidth
		val sourceHeight = if (orientation.rotatesDimensions) bounds.outWidth else bounds.outHeight
		val target = targetDimensions().fitPixelBudget()
		val decodeOptions = BitmapFactory.Options().apply {
			inSampleSize = calculateInSampleSize(
				sourceWidth = sourceWidth,
				sourceHeight = sourceHeight,
				targetWidth = target.width,
				targetHeight = target.height
			)
		}
		val decoded = contentResolver.openInputStream(source)?.use { stream ->
			BitmapFactory.decodeStream(stream, null, decodeOptions)
		} ?: throw IOException("Could not decode wallpaper source: $source")

		val oriented = decoded.applyExifOrientation(orientation)
		if (oriented !== decoded) decoded.recycle()
		val scaled = oriented.scaleDownToFill(target.width, target.height)
		if (scaled !== oriented) oriented.recycle()

		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			scaled.recycle()
			throw IOException("Could not create wallpaper cache directory")
		}

		val destination = File(outputDirectory, "${slot.name.lowercase()}_wallpaper.jpg")
		val temporaryDestination = File(outputDirectory, "${destination.name}.tmp")
		try {
			FileOutputStream(temporaryDestination).use { output ->
				check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
					"Could not encode prepared wallpaper"
				}
			}
			if (destination.exists() && !destination.delete()) {
				throw IOException("Could not replace prepared wallpaper")
			}
			if (!temporaryDestination.renameTo(destination)) {
				throw IOException("Could not finalize prepared wallpaper")
			}
			Log.i(TAG, "Prepared ${slot.name.lowercase()} wallpaper at ${scaled.width}x${scaled.height}")
			return Uri.fromFile(destination)
		} finally {
			scaled.recycle()
			if (temporaryDestination.exists()) temporaryDestination.delete()
		}
	}

	private fun targetDimensions(): Dimensions {
		val metrics = appContext.resources.displayMetrics
		return Dimensions(
			width = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: metrics.widthPixels,
			height = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: metrics.heightPixels
		)
	}

	private fun readExifOrientation(source: Uri): Int = try {
		contentResolver.openInputStream(source)?.use { stream ->
			ExifInterface(stream).getAttributeInt(
				ExifInterface.TAG_ORIENTATION,
				ExifInterface.ORIENTATION_NORMAL
			)
		} ?: ExifInterface.ORIENTATION_NORMAL
	} catch (_: IOException) {
		ExifInterface.ORIENTATION_NORMAL
	}
}

private data class Dimensions(val width: Int, val height: Int)

private fun Dimensions.fitPixelBudget(): Dimensions {
	val pixels = width.toLong() * height.toLong()
	if (pixels <= MAX_PREPARED_PIXELS) return this
	val scale = kotlin.math.sqrt(MAX_PREPARED_PIXELS.toDouble() / pixels)
	return Dimensions(
		width = max(1, (width * scale).toInt()),
		height = max(1, (height * scale).toInt())
	)
}

private val Int.rotatesDimensions: Boolean
	get() = this == ExifInterface.ORIENTATION_ROTATE_90 ||
		this == ExifInterface.ORIENTATION_ROTATE_270 ||
		this == ExifInterface.ORIENTATION_TRANSPOSE ||
		this == ExifInterface.ORIENTATION_TRANSVERSE

private fun calculateInSampleSize(
	sourceWidth: Int,
	sourceHeight: Int,
	targetWidth: Int,
	targetHeight: Int
): Int {
	var sampleSize = 1
	while (
		sourceWidth / (sampleSize * 2) >= targetWidth &&
		sourceHeight / (sampleSize * 2) >= targetHeight
	) {
		sampleSize *= 2
	}
	return sampleSize
}

private fun Bitmap.scaleDownToFill(targetWidth: Int, targetHeight: Int): Bitmap {
	val scale = minOf(1f, max(targetWidth.toFloat() / width, targetHeight.toFloat() / height))
	if (scale >= 1f) return this
	val scaledWidth = max(1, (width * scale).toInt())
	val scaledHeight = max(1, (height * scale).toInt())
	return scale(scaledWidth, scaledHeight, filter = true)
}

private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
	val matrix = Matrix().apply {
		when (orientation) {
			ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
			ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
			ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
			ExifInterface.ORIENTATION_TRANSPOSE -> {
				setRotate(90f)
				postScale(-1f, 1f)
			}
			ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
			ExifInterface.ORIENTATION_TRANSVERSE -> {
				setRotate(-90f)
				postScale(-1f, 1f)
			}
			ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
		}
	}
	if (matrix.isIdentity) return this
	return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
