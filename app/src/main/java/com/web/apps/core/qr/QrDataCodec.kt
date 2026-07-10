package com.web.apps.core.qr

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object QrDataCodec {

    fun encode(jsonString: String): String {
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }
        val compressed = byteStream.toByteArray()
        return Base64.encodeToString(compressed, Base64.NO_WRAP)
    }

    fun decode(encodedString: String): String {
        val compressed = Base64.decode(encodedString, Base64.NO_WRAP)
        val byteStream = java.io.ByteArrayInputStream(compressed)
        return GZIPInputStream(byteStream).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}