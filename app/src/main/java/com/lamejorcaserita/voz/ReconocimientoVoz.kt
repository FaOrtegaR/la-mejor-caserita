package com.lamejorcaserita.voz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Maneja el reconocimiento de voz usando la API nativa de Android.
 *
 * Estados posibles:
 * - LISTO: esperando al usuario
 * - ESCUCHANDO: capturando audio
 * - PROCESANDO: procesando el audio
 * - ERROR: algo falló
 */
class ReconocimientoVoz(private val context: Context) {

    enum class Estado { LISTO, ESCUCHANDO, PROCESANDO, ERROR }

    private val _estado = MutableStateFlow(Estado.LISTO)
    val estado: StateFlow<Estado> = _estado

    private val _textoReconocido = MutableStateFlow<String?>(null)
    val textoReconocido: StateFlow<String?> = _textoReconocido

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Verifica si el servicio de reconocimiento de voz está disponible en el dispositivo.
     */
    fun estaDisponible(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Inicia la captura de voz.
     */
    fun iniciarEscucha() {
        if (!estaDisponible()) {
            _error.value = "El reconocimiento de voz no está disponible en este dispositivo."
            _estado.value = Estado.ERROR
            return
        }

        _textoReconocido.value = null
        _error.value = null
        _estado.value = Estado.ESCUCHANDO

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(crearListener())
            }
        } else {
            // Si ya existe, cancelamos para dejarlo limpio
            speechRecognizer?.cancel()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
    }

    /**
     * Detiene la captura de voz.
     */
    fun detenerEscucha() {
        speechRecognizer?.stopListening()
    }

    /**
     * Libera los recursos. Llamar cuando la pantalla se cierra.
     */
    fun liberar() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Reinicia el estado para permitir una nueva captura.
     */
    fun reiniciar() {
        _estado.value = Estado.LISTO
        _textoReconocido.value = null
        _error.value = null
    }

    /**
     * Cancela el reconocimiento en curso y vuelve al estado LISTO.
     * A diferencia de stopListening(), cancel() no espera resultados
     * y no lanza ERROR_CLIENT.
     */
    fun cancelar() {
        speechRecognizer?.cancel()
        _estado.value = Estado.LISTO
        _textoReconocido.value = null
        _error.value = null
    }

    private fun crearListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _estado.value = Estado.ESCUCHANDO
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _estado.value = Estado.PROCESANDO
            }

            override fun onError(error: Int) {
                val mensaje = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio."
                    SpeechRecognizer.ERROR_CLIENT -> "Se canceló el reconocimiento. Intenta de nuevo."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sin permisos de micrófono."
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red. Verifica tu conexión."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera agotado."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No te entendí. Intenta de nuevo."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado. Espera un momento."
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor. Intenta de nuevo."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No escuché nada. Intenta de nuevo."
                    else -> "Error desconocido."
                }
                _error.value = mensaje
                _estado.value = Estado.ERROR
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _textoReconocido.value = matches[0]
                    _estado.value = Estado.LISTO
                } else {
                    _error.value = "No se reconoció ningún texto."
                    _estado.value = Estado.ERROR
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}