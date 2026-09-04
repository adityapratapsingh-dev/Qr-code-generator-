package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QRCodeUtils {

  /**
   * Generate pure ZXing QRCode Bitmap locally without internet or external requests.
   */
  fun generateQRCodeBitmap(
    content: String,
    size: Int = 512,
    ecc: ErrorCorrectionLevel = ErrorCorrectionLevel.H,
    fgColor: Int = android.graphics.Color.BLACK,
    bgColor: Int = android.graphics.Color.WHITE,
    centerBadge: String? = null
  ): Bitmap? {
    if (content.isBlank()) return null
    return try {
      val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ecc,
        EncodeHintType.MARGIN to 1
      )
      val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
      val width = bitMatrix.width
      val height = bitMatrix.height
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      for (x in 0 until width) {
        for (y in 0 until height) {
          bitmap.setPixel(
            x,
            y,
            if (bitMatrix.get(x, y)) fgColor else bgColor
          )
        }
      }

      // If center badge requested and content is safe
      if (!centerBadge.isNullOrBlank()) {
        val canvas = android.graphics.Canvas(bitmap)
        val badgeSize = (width * 0.2f).toInt()
        val left = (width - badgeSize) / 2f
        val top = (height - badgeSize) / 2f
        val rect = android.graphics.RectF(left, top, left + badgeSize, top + badgeSize)
        val cornerRadius = badgeSize * 0.26f

        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
          color = android.graphics.Color.WHITE
          style = android.graphics.Paint.Style.FILL
        }
        val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
          color = android.graphics.Color.LTGRAY
          style = android.graphics.Paint.Style.STROKE
          strokeWidth = (width * 0.006f).coerceAtLeast(2f)
        }
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
          color = android.graphics.Color.BLACK
          textSize = badgeSize * 0.55f
          textAlign = android.graphics.Paint.Align.CENTER
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        val fontMetrics = textPaint.fontMetrics
        val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
        canvas.drawText(centerBadge, rect.centerX(), baseline, textPaint)
      }

      bitmap
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Generate pure Scalable Vector Graphics (SVG) string for lossless print and designers.
   */
  fun generateQRCodeSvg(
    content: String,
    ecc: ErrorCorrectionLevel = ErrorCorrectionLevel.H,
    fgColorHex: String = "#0f172a",
    bgColorHex: String = "#ffffff",
    margin: Int = 2,
    centerBadge: String? = null
  ): String? {
    if (content.isBlank()) return null
    return try {
      val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ecc,
        EncodeHintType.MARGIN to 0
      )
      val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
      val width = bitMatrix.width
      val height = bitMatrix.height
      val totalSize = width + (margin * 2)

      val centerMin = if (!centerBadge.isNullOrBlank()) (width * 0.38).toInt() else -1
      val centerMax = if (!centerBadge.isNullOrBlank()) (width * 0.62).toInt() else -1

      val sb = StringBuilder()
      sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
      sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $totalSize $totalSize" width="1024" height="1024" shape-rendering="crispEdges">""").append("\n")
      sb.append("""  <rect width="100%" height="100%" fill="$bgColorHex"/>""").append("\n")
      sb.append("""  <path fill="$fgColorHex" d="""")

      for (y in 0 until height) {
        for (x in 0 until width) {
          if (centerMin in 0..centerMax && x in centerMin..centerMax && y in centerMin..centerMax) {
            continue
          }
          if (bitMatrix.get(x, y)) {
            sb.append("M${x + margin},${y + margin}h1v1h-1z ")
          }
        }
      }
      sb.append("""" stroke="none"/>""").append("\n")

      if (!centerBadge.isNullOrBlank() && centerMin in 0..centerMax) {
        val bSize = (centerMax - centerMin + 1) + 0.4
        val bX = centerMin + margin - 0.2
        val bY = centerMin + margin - 0.2
        sb.append("""  <rect x="$bX" y="$bY" width="$bSize" height="$bSize" rx="1.5" fill="#ffffff" stroke="#cbd5e1" stroke-width="0.2"/>""").append("\n")
        sb.append("""  <text x="${bX + bSize / 2}" y="${bY + bSize / 2 + 0.5}" font-size="${bSize * 0.6}" text-anchor="middle" dominant-baseline="middle">$centerBadge</text>""").append("\n")
      }

      sb.append("""</svg>""")
      sb.toString()
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Save high-quality bitmap to device gallery (Pictures/QRCodes) via MediaStore.
   */
  fun saveBitmapToGallery(context: Context, bitmap: Bitmap, contentText: String): Boolean {
    val cleanName = contentText.filter { it.isLetterOrDigit() }.take(16).ifBlank { "qrcode" }
    val filename = "qr_${cleanName}_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRCodes")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false

    return try {
      resolver.openOutputStream(uri)?.use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Save vector SVG to Downloads/Documents or Pictures.
   */
  fun saveSvgToDocuments(context: Context, svgString: String, contentText: String): Boolean {
    val cleanName = contentText.filter { it.isLetterOrDigit() }.take(16).ifBlank { "vector_qr" }
    val filename = "qr_${cleanName}_${System.currentTimeMillis()}.svg"
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/svg+xml")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QRCodes")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }
    }

    val resolver = context.contentResolver
    val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
      MediaStore.Files.getContentUri("external")
    }

    val uri = resolver.insert(collectionUri, contentValues) ?: return false

    return try {
      resolver.openOutputStream(uri)?.use { stream ->
        stream.write(svgString.toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Share QR Code Bitmap via system share intent.
   */
  fun shareBitmap(context: Context, bitmap: Bitmap, contentText: String) {
    try {
      val cachePath = File(context.cacheDir, "images")
      cachePath.mkdirs()
      val file = File(cachePath, "shared_qr.png")
      FileOutputStream(file).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
      }
      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_TEXT, contentText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    } catch (e: Exception) {
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, contentText)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share Content"))
    }
  }

  /**
   * Share SVG file via system share intent.
   */
  fun shareSvg(context: Context, svgString: String, contentText: String) {
    try {
      val cachePath = File(context.cacheDir, "svgs")
      cachePath.mkdirs()
      val file = File(cachePath, "shared_qr.svg")
      file.writeText(svgString, Charsets.UTF_8)

      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/svg+xml"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_TEXT, contentText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share Vector SVG"))
    } catch (e: Exception) {
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, svgString)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share SVG Text"))
    }
  }
}
