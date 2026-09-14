/*
 * Copyright (C) 2024-2026 Focus by Rj
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.focusbyrj.app.ui.screens.notes

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

class AudioMemoManager(private val context: Context) {

    // ==========================================
    // RECORDING STATE
    // ==========================================
    data class RecordingState(
        val isRecording: Boolean = false,
        val elapsedSeconds: Int = 0,
        val currentAmplitude: Float = 0f,
        val liveTranscript: String = "",
        val currentOutputFile: File? = null
    )

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val accumulatedTranscript = StringBuilder()
    private var onTranscriptCallback: ((String) -> Unit)? = null

    fun startRecording(onTranscriptUpdate: (String) -> Unit = {}) {
        stopPlayback()
        cancelRecording()

        try {
            val audioDir = File(context.filesDir, "keep_audio").apply { if (!exists()) mkdirs() }
            val file = File(audioDir, "audio_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder

            _recordingState.value = RecordingState(
                isRecording = true,
                elapsedSeconds = 0,
                currentAmplitude = 0f,
                liveTranscript = "",
                currentOutputFile = file
            )

            // High-precision live timer and amplitude poller
            val startTimeMs = System.currentTimeMillis()
            recordingJob = scope.launch {
                while (isActive && _recordingState.value.isRecording) {
                    delay(60)
                    val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)

                    _recordingState.value = _recordingState.value.copy(
                        elapsedSeconds = elapsed,
                        currentAmplitude = normalizedAmp
                    )
                }
            }

            // Continuous On-Device Speech Recognition
            initSpeechRecognizer(onTranscriptUpdate)

        } catch (e: Exception) {
            e.printStackTrace()
            cancelRecording()
        }
    }

    private fun initSpeechRecognizer(onTranscriptUpdate: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        accumulatedTranscript.clear()
        onTranscriptCallback = onTranscriptUpdate
        startSpeechRecognitionSession()
    }

    private fun startSpeechRecognitionSession() {
        if (!_recordingState.value.isRecording) return
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        if (rmsdB > 0) {
                            val speechAmp = (rmsdB / 10f).coerceIn(0f, 1f)
                            val current = _recordingState.value.currentAmplitude
                            _recordingState.value = _recordingState.value.copy(
                                currentAmplitude = maxOf(current, speechAmp)
                            )
                        }
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        // In Android SpeechRecognizer, brief silences or timeouts trigger onError.
                        // Seamlessly restart recognition if user is still actively recording.
                        if (_recordingState.value.isRecording) {
                            scope.launch(Dispatchers.Main) {
                                delay(200)
                                startSpeechRecognitionSession()
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            if (accumulatedTranscript.isNotEmpty()) {
                                accumulatedTranscript.append(" ")
                            }
                            val formatted = text.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            }
                            accumulatedTranscript.append(formatted)
                            if (!formatted.endsWith(".") && !formatted.endsWith("?") && !formatted.endsWith("!")) {
                                accumulatedTranscript.append(".")
                            }
                            val full = accumulatedTranscript.toString().trim()
                            _recordingState.value = _recordingState.value.copy(liveTranscript = full)
                            onTranscriptCallback?.invoke(full)
                        }
                        if (_recordingState.value.isRecording) {
                            scope.launch(Dispatchers.Main) {
                                startSpeechRecognitionSession()
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            val full = if (accumulatedTranscript.isNotEmpty()) {
                                "${accumulatedTranscript.toString().trim()} $partial"
                            } else {
                                partial
                            }
                            _recordingState.value = _recordingState.value.copy(liveTranscript = full)
                            onTranscriptCallback?.invoke(full)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                startListening(intent)
            }
        } catch (_: Exception) {}
    }

    fun stopRecording(): Pair<String?, String> {
        val file = currentOutputFile
        val transcript = _recordingState.value.liveTranscript

        recordingJob?.cancel()
        recordingJob = null

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        _recordingState.value = RecordingState(isRecording = false)
        currentOutputFile = null

        return if (file != null && file.exists() && file.length() > 0) {
            Pair(file.absolutePath, transcript)
        } else {
            Pair(null, transcript)
        }
    }

    fun cancelRecording() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {} finally {
            mediaRecorder = null
        }

        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null

        _recordingState.value = RecordingState(isRecording = false)
    }

    // ==========================================
    // PLAYBACK STATE
    // ==========================================
    data class PlaybackState(
        val currentPath: String? = null,
        val isPlaying: Boolean = false,
        val currentPositionMs: Int = 0,
        val durationMs: Int = 0,
        val speed: Float = 1.0f
    )

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    fun playOrToggle(audioPath: String) {
        if (_playbackState.value.currentPath == audioPath) {
            if (_playbackState.value.isPlaying) {
                pausePlayback()
            } else {
                resumePlayback()
            }
            return
        }

        stopPlayback()

        try {
            val file = File(audioPath)
            if (!file.exists()) return

            val player = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _playbackState.value.speed != 1.0f) {
                    try {
                        val params = playbackParams
                        params.speed = _playbackState.value.speed
                        playbackParams = params
                    } catch (_: Exception) {}
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0
                    )
                    playbackJob?.cancel()
                }
                start()
            }
            mediaPlayer = player

            _playbackState.value = _playbackState.value.copy(
                currentPath = audioPath,
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = player.duration
            )

            startPlaybackTracking()
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    private fun pausePlayback() {
        try {
            mediaPlayer?.pause()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            playbackJob?.cancel()
        } catch (_: Exception) {}
    }

    private fun resumePlayback() {
        try {
            mediaPlayer?.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startPlaybackTracking()
        } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        } catch (_: Exception) {}
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.let { player ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                }
            }
            _playbackState.value = _playbackState.value.copy(speed = speed)
        } catch (_: Exception) {}
    }

    fun skip(deltaMs: Int) {
        val current = _playbackState.value.currentPositionMs
        val dur = _playbackState.value.durationMs
        val target = (current + deltaMs).coerceIn(0, if (dur > 0) dur else Int.MAX_VALUE)
        seekTo(target)
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {} finally {
            mediaPlayer = null
        }
        _playbackState.value = PlaybackState()
    }

    private fun startPlaybackTracking() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                delay(100)
                val pos = try {
                    mediaPlayer?.currentPosition ?: 0
                } catch (_: Exception) {
                    0
                }
                val dur = try {
                    mediaPlayer?.duration ?: 0
                } catch (_: Exception) {
                    0
                }
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = pos,
                    durationMs = if (dur > 0) dur else _playbackState.value.durationMs
                )
            }
        }
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

        fun getAudioDurationMs(path: String): Long {
            return try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(path)
                val durationStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                mmr.release()
                durationStr?.toLongOrNull() ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}
