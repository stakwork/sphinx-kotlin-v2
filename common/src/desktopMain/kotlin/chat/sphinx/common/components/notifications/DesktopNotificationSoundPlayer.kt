package chat.sphinx.common.components.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

object DesktopNotificationSoundPlayer {
    private val playbackMutex = Mutex()
    private var isPlaying = false
    private var currentClip: Clip? = null

    suspend fun playNotificationSound() {

        if (isPlaying) {
            return
        }

        playbackMutex.withLock {
            if (isPlaying) {
                return
            }
            isPlaying = true
        }

        withContext(Dispatchers.IO) {
            try {
                val audioInputStream = javaClass.getResourceAsStream("/notification.wav")
                    ?: throw IllegalStateException("Could not find notification.wav in resources")

                val bufferedInputStream = BufferedInputStream(audioInputStream)
                val audioStream = AudioSystem.getAudioInputStream(bufferedInputStream)

                val clip: Clip = AudioSystem.getClip()
                currentClip = clip
                clip.open(audioStream)

                clip.addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP) {
                        clip.close()
                        audioStream.close()
                        currentClip = null
                        isPlaying = false
                    }
                }

                clip.start()

            } catch (e: Exception) {
                e.printStackTrace()
                isPlaying = false
                currentClip = null
            }
        }
    }

    fun playNotificationSoundSync() {
        if (isPlaying) {
            return
        }

        isPlaying = true

        try {
            val audioInputStream = javaClass.getResourceAsStream("/notification.wav")
                ?: throw IllegalStateException("Could not find notification.wav in resources")

            val bufferedInputStream = BufferedInputStream(audioInputStream)
            val audioStream = AudioSystem.getAudioInputStream(bufferedInputStream)

            val clip: Clip = AudioSystem.getClip()
            currentClip = clip
            clip.open(audioStream)

            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                    audioStream.close()
                    currentClip = null
                    isPlaying = false
                }
            }

            clip.start()

        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
            currentClip = null
        }
    }

    fun stopCurrentSound() {
        currentClip?.let { clip ->
            if (clip.isRunning) {
                clip.stop()
            }
            clip.close()
            currentClip = null
            isPlaying = false
        }
    }
}
