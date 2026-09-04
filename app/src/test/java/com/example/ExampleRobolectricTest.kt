package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.QRCodeUtils
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("QR Code Generator", appName)
  }

  @Test
  fun `generate qr code with valid url returns valid bitmap`() {
    val bitmap = QRCodeUtils.generateQRCodeBitmap("https://ai.studio", 256, ErrorCorrectionLevel.H)
    assertNotNull("Generated bitmap should not be null", bitmap)
    assertEquals(256, bitmap!!.width)
    assertEquals(256, bitmap.height)
  }

  @Test
  fun `generate qr code with blank content returns null`() {
    val emptyBitmap = QRCodeUtils.generateQRCodeBitmap("", 256, ErrorCorrectionLevel.M)
    assertNull("Empty content should return null", emptyBitmap)

    val whitespaceBitmap = QRCodeUtils.generateQRCodeBitmap("   ", 256, ErrorCorrectionLevel.M)
    assertNull("Whitespace content should return null", whitespaceBitmap)
  }

  @Test
  fun `generate qr code with different sizes`() {
    val smallBitmap = QRCodeUtils.generateQRCodeBitmap("test-small", 128, ErrorCorrectionLevel.L)
    assertNotNull(smallBitmap)
    assertEquals(128, smallBitmap!!.width)

    val largeBitmap = QRCodeUtils.generateQRCodeBitmap("test-large", 512, ErrorCorrectionLevel.Q)
    assertNotNull(largeBitmap)
    assertEquals(512, largeBitmap!!.width)
  }
}
