package com.example.emotionlink.data

import java.io.File
import java.io.FileOutputStream

class AudioConvert {
    companion object {
        fun convertPcmToWav(pcmFile: File, wavFile: File) {
            val sampleRate = 16000
            val channels = 1
            val byteRate = sampleRate * channels * 16 / 8

            val pcmSize = pcmFile.length().toInt()
            val wavOut = FileOutputStream(wavFile)
            val header = ByteArray(44)

            // RIFF/WAVE header
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] =
                'F'.code.toByte(); header[3] = 'F'.code.toByte()
            writeInt(header, 4, pcmSize + 36)
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] =
                'V'.code.toByte(); header[11] = 'E'.code.toByte()

            // fmt subchunk
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] =
                't'.code.toByte(); header[15] = ' '.code.toByte()
            writeInt(header, 16, 16) // Subchunk1Size for PCM
            writeShort(header, 20, 1.toShort()) // AudioFormat = 1
            writeShort(header, 22, channels.toShort())
            writeInt(header, 24, sampleRate)
            writeInt(header, 28, byteRate)
            writeShort(header, 32, (channels * 16 / 8).toShort()) // BlockAlign
            writeShort(header, 34, 16.toShort()) // BitsPerSample

            // data subchunk
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] =
                't'.code.toByte(); header[39] = 'a'.code.toByte()
            writeInt(header, 40, pcmSize)

            wavOut.write(header)

            val pcmIn = pcmFile.inputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (pcmIn.read(buffer).also { bytesRead = it } != -1) {
                wavOut.write(buffer, 0, bytesRead)
            }
            pcmIn.close()
            wavOut.close()
        }
        // 工具函数
        fun writeInt(b: ByteArray, offset: Int, value: Int) {
            b[offset] = (value and 0xff).toByte()
            b[offset + 1] = ((value shr 8) and 0xff).toByte()
            b[offset + 2] = ((value shr 16) and 0xff).toByte()
            b[offset + 3] = ((value shr 24) and 0xff).toByte()
        }

        fun writeShort(b: ByteArray, offset: Int, value: Short) {
            b[offset] = (value.toInt() and 0xff).toByte()
            b[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
        }
    }

}
