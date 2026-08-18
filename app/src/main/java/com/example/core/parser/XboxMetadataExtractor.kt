package com.example.core.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object XboxMetadataExtractor {
    private const val TAG = "Xenia_Metadata"

    data class ParsedMetadata(
        val titleId: String,
        val mediaId: String,
        val titleName: String,
        val discNumber: Int = 1,
        val discCount: Int = 1,
        val region: String = "Region Free",
        val fileFormat: String
    )

    fun extract(context: Context, uri: Uri, fileName: String): ParsedMetadata {
        val lowerName = fileName.lowercase()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(8192)
                val bytesRead = stream.read(header)
                if (bytesRead >= 4) {
                    // Check STFS Container (CON, LIVE, PIRS)
                    val magicStr = String(header, 0, 4, Charsets.US_ASCII)
                    if (magicStr == "CON " || magicStr == "LIVE" || magicStr == "PIRS") {
                        val stfsMeta = parseStfsContainer(header, bytesRead, fileName)
                        if (stfsMeta != null) return stfsMeta
                    }

                    // Check XEX2 Executable
                    if (header[0] == 0x58.toByte() && header[1] == 0x45.toByte() &&
                        header[2] == 0x58.toByte() && header[3] == 0x32.toByte()
                    ) {
                        val xexMeta = parseXexHeader(header, bytesRead, fileName)
                        if (xexMeta != null) return xexMeta
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fast header parse failed for $fileName: ${e.message}")
        }

        // Check if ISO / Disc Image by extension or fallback
        val format = when {
            lowerName.endsWith(".iso") -> "ISO"
            lowerName.endsWith(".xex") -> "XEX"
            lowerName.endsWith(".stfs") || lowerName.endsWith(".god") -> "STFS"
            else -> "X360 Package"
        }

        return fallbackExtraction(fileName, format)
    }

    private fun parseStfsContainer(header: ByteArray, length: Int, fileName: String): ParsedMetadata? {
        if (length < 0x400) return null
        try {
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)

            // Title ID at offset 0x360 (4 bytes)
            val titleIdInt = buffer.getInt(0x360)
            val titleIdHex = String.format("%08X", titleIdInt)

            // Media ID at offset 0x354 (4 bytes)
            val mediaIdInt = buffer.getInt(0x354)
            val mediaIdHex = if (mediaIdInt != 0) String.format("%08X", mediaIdInt) else ""

            // Title Name UTF-16BE often located around 0x410 or extracted from fallback
            var extractedName = extractUtf16String(header, 0x410, 128)
            if (extractedName.isBlank() || extractedName.contains("\u0000")) {
                extractedName = cleanGameTitleFromFilename(fileName)
            }

            return ParsedMetadata(
                titleId = if (titleIdHex != "00000000") titleIdHex else generateDeterministicTitleId(fileName),
                mediaId = mediaIdHex,
                titleName = extractedName,
                fileFormat = "STFS"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseStfsContainer: ${e.message}")
            return null
        }
    }

    private fun parseXexHeader(header: ByteArray, length: Int, fileName: String): ParsedMetadata? {
        if (length < 64) return null
        try {
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)
            // XEX header structure:
            // 0x00: Magic 'XEX2'
            // 0x04: Module Flags
            // 0x08: PE Data Offset
            // 0x10: Security Info Offset
            // 0x14: Optional Header Count
            val optHeaderCount = buffer.getInt(0x14)
            var titleIdHex = ""
            var mediaIdHex = ""
            var discNumber = 1
            var discCount = 1

            var currentOffset = 0x18
            for (i in 0 until optHeaderCount) {
                if (currentOffset + 8 > length) break
                val key = buffer.getInt(currentOffset)
                val value = buffer.getInt(currentOffset + 4)
                currentOffset += 8

                // XEX_HEADER_EXECUTION_INFO key is 0x00040006
                if (key == 0x00040006) {
                    val infoOffset = value
                    if (infoOffset + 24 <= length) {
                        val mediaIdVal = buffer.getInt(infoOffset)
                        val titleIdVal = buffer.getInt(infoOffset + 8)
                        val discNum = (buffer.get(infoOffset + 14).toInt() and 0xFF)
                        val discTotal = (buffer.get(infoOffset + 15).toInt() and 0xFF)

                        titleIdHex = String.format("%08X", titleIdVal)
                        mediaIdHex = String.format("%08X", mediaIdVal)
                        if (discNum > 0) discNumber = discNum
                        if (discTotal > 0) discCount = discTotal
                    }
                }
            }

            val titleName = cleanGameTitleFromFilename(fileName)
            return ParsedMetadata(
                titleId = if (titleIdHex.isNotBlank() && titleIdHex != "00000000") titleIdHex else generateDeterministicTitleId(fileName),
                mediaId = mediaIdHex,
                titleName = titleName,
                discNumber = discNumber,
                discCount = discCount,
                fileFormat = "XEX"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseXexHeader: ${e.message}")
            return null
        }
    }

    private fun extractUtf16String(bytes: ByteArray, offset: Int, maxLen: Int): String {
        if (offset + maxLen > bytes.size) return ""
        val sb = StringBuilder()
        var i = offset
        while (i < offset + maxLen - 1) {
            val high = bytes[i].toInt() and 0xFF
            val low = bytes[i + 1].toInt() and 0xFF
            val ch = ((high shl 8) or low).toChar()
            if (ch == '\u0000') break
            if (ch.code in 32..126 || ch.isLetterOrDigit()) {
                sb.append(ch)
            }
            i += 2
        }
        return sb.toString().trim()
    }

    fun cleanGameTitleFromFilename(rawName: String): String {
        var clean = rawName
        // Strip common extensions
        clean = clean.replace(Regex("\\.(iso|xex|stfs|god|bin|img|rar|zip)$", RegexOption.IGNORE_CASE), "")
        // Remove release tags like [USA], (Disc 1), [XBLA], (En,Fr,De)
        clean = clean.replace(Regex("\\[.*?\\]"), " ")
        clean = clean.replace(Regex("\\(Disc\\s*\\d+.*?\\)", RegexOption.IGNORE_CASE), " ")
        clean = clean.replace(Regex("\\(.*?\\)"), " ")
        clean = clean.replace(Regex("[_.-]"), " ")
        clean = clean.replace(Regex("\\s+"), " ").trim()
        return if (clean.isNotBlank()) clean else "Xbox 360 Title"
    }

    fun generateDeterministicTitleId(name: String): String {
        val hash = (name.hashCode().toLong() and 0xFFFFFFFFL)
        return String.format("4D53%04X", (hash and 0xFFFFL))
    }

    private fun fallbackExtraction(fileName: String, format: String): ParsedMetadata {
        val cleanName = cleanGameTitleFromFilename(fileName)
        val titleId = generateDeterministicTitleId(fileName)
        return ParsedMetadata(
            titleId = titleId,
            mediaId = "",
            titleName = cleanName,
            fileFormat = format
        )
    }
}
