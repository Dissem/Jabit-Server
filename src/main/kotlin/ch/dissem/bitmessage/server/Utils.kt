/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.server

import ch.dissem.bitmessage.entity.BitmessageAddress
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.util.*

object Utils {
    private val LOG = LoggerFactory.getLogger(Utils::class.java)

    fun readOrCreateList(filename: String, content: String): MutableSet<String> {
        val file = File(filename).apply {
            if (!exists()) {
                if (createNewFile()) {
                    FileWriter(this).use { fw -> fw.write(content) }
                }
            }
        }
        return readList(file)
    }

    fun saveList(filename: String, content: Collection<String>) {
        FileWriter(filename).use { fw ->
            content.forEach { l ->
                fw.write(l)
                fw.write(System.lineSeparator())
            }
        }
    }

    fun readList(file: File) = Files.readAllLines(file.toPath())
            .map { it.trim { it <= ' ' } }
            .filter { it.startsWith("BM-") }
            .toMutableSet()

    fun getURL(address: BitmessageAddress, includeKey: Boolean): String {
        val attributes = mutableListOf<String>()
        if (address.alias != null) {
            attributes.add("label=${address.alias}")
        }
        if (includeKey) {
            address.pubkey?.let { pubkey ->
                val out = ByteArrayOutputStream()
                pubkey.writer().writeUnencrypted(out)
                attributes.add("pubkey=${Base64.getUrlEncoder().encodeToString(out.toByteArray())}")
            }
        }

        return if (attributes.isEmpty()) {
            "bitmessage:${address.address}"
        } else {
            "bitmessage:${address.address}?${attributes.joinToString(separator = "&")}"
        }
    }

    fun qrCode(address: BitmessageAddress): String {
        val code: QRCode
        try {
            code = Encoder.encode(getURL(address, false), ErrorCorrectionLevel.L, null)
        } catch (e: WriterException) {
            LOG.error(e.message, e)
            return ""
        }

        val matrix = code.matrix
        val result = StringBuilder()
        for (i in 0..1) {
            for (j in 0 until matrix.width + 8) {
                result.append('█')
            }
            result.append('\n')
        }
        run {
            var i = 0
            while (i < matrix.height) {
                result.append("████")
                for (j in 0 until matrix.width) {
                    if (matrix.get(i, j) > 0) {
                        if (matrix.height > i + 1 && matrix.get(i + 1, j) > 0) {
                            result.append(' ')
                        } else {
                            result.append('▄')
                        }
                    } else {
                        if (matrix.height > i + 1 && matrix.get(i + 1, j) > 0) {
                            result.append('▀')
                        } else {
                            result.append('█')
                        }
                    }
                }
                result.append("████\n")
                i += 2
            }
        }
        for (i in 0..1) {
            for (j in 0 until matrix.width + 8) {
                result.append('█')
            }
            result.append('\n')
        }
        return result.toString()
    }

    fun zero(nonce: ByteArray) = nonce.none { it.toInt() != 0 }
}
