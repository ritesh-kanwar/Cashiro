package com.ritesh.cashiro.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    /**
     * Saves an image from a given Uri to the application's internal storage.
     *
     * @param context The application context.
     * @param uri The Uri of the image to save.
     * @param fileNamePrefix A prefix for the generated file name.
     * @return The Uri of the saved file, or null if an error occurred.
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri, fileNamePrefix: String): Uri? {
        // If it's already a file URI, check if it's already in our internal storage
        if (uri.scheme == "file") {
            val internalFilesDir = context.filesDir.absolutePath
            if (uri.path?.startsWith(internalFilesDir) == true) {
                return uri
            }
        }

        // If it's a resource URI (preset avatars), no need to copy
        if (uri.scheme == "android.resource") {
            return uri
        }

        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val extension = getFileExtension(context, uri)
            val fileName = "${fileNamePrefix}_${UUID.randomUUID()}.$extension"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }

            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
    }

    /** Deletes an image file from internal storage. */
    fun deleteImageFromInternalStorage(context: Context, uri: Uri) {
        if (uri.scheme == "file") {
            uri.path?.let { path ->
                val file = File(path)
                if (file.exists() && path.startsWith(context.filesDir.absolutePath)) {
                    file.delete()
                }
            }
        }
    }
}
