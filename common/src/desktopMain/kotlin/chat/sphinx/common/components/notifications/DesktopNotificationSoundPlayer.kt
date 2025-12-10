package chat.sphinx.common.components.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

object DesktopNotificationSoundPlayer {

    suspend fun playNotificationSound() {
        withContext(Dispatchers.IO) {
            try {
                val audioInputStream = javaClass.getResourceAsStream("/notification.wav")
                    ?: throw IllegalStateException("Could not find triangle_short.wav in resources")

                val bufferedInputStream = BufferedInputStream(audioInputStream)
                val audioStream = AudioSystem.getAudioInputStream(bufferedInputStream)

                val clip: Clip = AudioSystem.getClip()
                clip.open(audioStream)

                clip.addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP) {
                        clip.close()
                        audioStream.close()
                    }
                }

                clip.start()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playNotificationSoundSync() {
        try {
            val audioInputStream = javaClass.getResourceAsStream("/notification.wav")
                ?: throw IllegalStateException("Could not find triangle_short.wav in resources")

            val bufferedInputStream = BufferedInputStream(audioInputStream)
            val audioStream = AudioSystem.getAudioInputStream(bufferedInputStream)

            val clip: Clip = AudioSystem.getClip()
            clip.open(audioStream)

            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                    audioStream.close()
                }
            }

            clip.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}