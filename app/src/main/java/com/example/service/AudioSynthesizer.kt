package com.example.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioSynthesizer {

    suspend fun getOrCreateDemoAudioFile(
        context: Context,
        songId: String,
        baseFrequency: Double = 440.0,
        bpm: Int = 120,
        durationSeconds: Int = 15
    ): File = withContext(Dispatchers.IO) {
        val audioDir = File(context.cacheDir, "demo_audio").apply { if (!exists()) mkdirs() }
        val audioFile = File(audioDir, "${songId}.wav")
        if (!audioFile.exists() || audioFile.length() == 0L) {
            generateWavFile(audioFile, baseFrequency, bpm, durationSeconds)
        }
        audioFile
    }

    private fun generateWavFile(
        file: File,
        rootFreq: Double,
        bpm: Int,
        durationSeconds: Int
    ) {
        val sampleRate = 44100
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSeconds
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)

        val chordProgressions = listOf(
            doubleArrayOf(rootFreq, rootFreq * 1.25, rootFreq * 1.5),
            doubleArrayOf(rootFreq * 0.8, rootFreq, rootFreq * 1.2),
            doubleArrayOf(rootFreq * 1.333, rootFreq * 1.666, rootFreq * 2.0),
            doubleArrayOf(rootFreq * 1.125, rootFreq * 1.35, rootFreq * 1.68)
        )

        val beatDuration = 60.0 / bpm
        val samplesPerBeat = (sampleRate * beatDuration).toInt()

        FileOutputStream(file).use { out ->
            writeWavHeader(out, dataSize, sampleRate, numChannels, bitsPerSample)

            val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until totalSamples) {
                val time = i.toDouble() / sampleRate
                val currentBeat = (i / samplesPerBeat)
                val chordIndex = (currentBeat / 4) % chordProgressions.size
                val currentChord = chordProgressions[chordIndex]

                var sampleVal = 0.0
                for (freq in currentChord) {
                    sampleVal += Math.sin(2.0 * Math.PI * freq * time)
                }
                sampleVal /= currentChord.size

                val envelope = Math.min(1.0, Math.min(time * 2.0, (durationSeconds - time) * 2.0))
                val sampleShort = (sampleVal * 12000.0 * envelope).toInt().coerceIn(-32768, 32767).toShort()

                if (buffer.remaining() < 4) {
                    out.write(buffer.array(), 0, buffer.position())
                    buffer.clear()
                }

                buffer.putShort(sampleShort)
                buffer.putShort(sampleShort)
            }

            if (buffer.position() > 0) {
                out.write(buffer.array(), 0, buffer.position())
            }
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = blockAlign.toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
