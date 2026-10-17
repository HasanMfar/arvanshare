package com.arvanshare.app.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Microphone recorder producing compact AAC/M4A voice notes
 * (small uploads; the WAV-recording desktop renders the same field as
 * `media_type="audio"`, so notes are interchangeable across platforms).
 *
 * Not thread-safe by design — a recorder instance lives inside one screen.
 */
class VoiceRecorder(context: Context) {

    private var recorder: MediaRecorder? = null
    private var outFile: File? = null

    /** True from [start] until [stop]/[cancel]/[release]. */
    var isRecording: Boolean = false
        private set

    /** Starts recording and returns the file being written. */
    fun start(): File {
        check(!isRecording) { "Already recording" }
        val f = File(context.cacheDir, "voice_${Keys.nowStamp()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
        } catch (t: Throwable) {
            r.release()
            f.delete()
            throw t
        }
        recorder = r
        outFile = f
        isRecording = true
        return f
    }

    /**
     * Stops and finalises the file. Returns the finished voice note, or null
     * when the recording was too short to encode anything.
     */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        isRecording = false
        val f = outFile
        outFile = null
        return try {
            r.stop()
            f?.takeIf { it.exists() && it.length() > 0L }
        } catch (t: Throwable) {
            f?.delete()
            null
        } finally {
            r.release()
        }
    }

    /** Discards the in-progress recording (no file is kept). */
    fun cancel() {
        val r = recorder ?: return
        recorder = null
        isRecording = false
        try {
            r.stop()
        } catch (_: Throwable) {
            // A stop right after start can fail — that's fine, we discard.
        } finally {
            r.release()
        }
        outFile?.delete()
        outFile = null
    }

    /** Lets the UI drop the recorder with the screen (safe when idle). */
    fun release() {
        cancel()
    }
}
