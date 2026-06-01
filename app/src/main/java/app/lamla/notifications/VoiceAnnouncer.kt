package app.lamla.notifications

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wrapper around Android TTS for "speak class name" announcements.
 *
 * - Respects ringer mode: silent → no speech, vibrate → no speech (only haptic),
 *   normal → speak.
 * - Lazily initializes TTS engine on first use; survives for the process lifetime.
 * - Calls are queued (QUEUE_ADD) so back-to-back announcements don't drop.
 */
@Singleton
class VoiceAnnouncer @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready: Boolean = false

    fun announce(phrase: String) {
        val audio = ContextCompat.getSystemService(appContext, AudioManager::class.java) ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        scope.launch {
            if (!ready) initBlocking()
            if (ready) {
                tts?.speak(phrase, TextToSpeech.QUEUE_ADD, null, phrase.hashCode().toString())
            }
        }
    }

    private suspend fun initBlocking() {
        if (ready) return
        val ok = suspendCancellableCoroutine<Boolean> { cont ->
            val engine = TextToSpeech(appContext) { status ->
                cont.resume(status == TextToSpeech.SUCCESS)
            }
            tts = engine
            cont.invokeOnCancellation { engine.shutdown() }
        }
        if (ok) {
            tts?.language = Locale.getDefault()
            ready = true
        }
    }
}
