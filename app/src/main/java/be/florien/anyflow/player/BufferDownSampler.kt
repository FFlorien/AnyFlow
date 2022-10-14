package be.florien.anyflow.player

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import java.nio.ByteBuffer
import kotlin.math.absoluteValue

class AnyFlowRenderersFactory(
    context: Context,
    private val onBufferDownSamplingListener: BufferDownSamplingListener
) : DefaultRenderersFactory(context) {

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        out.add(
            BufferDownSamplingListenerAudioRenderer(
                context,
                mediaCodecSelector,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                DefaultAudioSink.Builder().setAudioCapabilities(
                    AudioCapabilities.getCapabilities(context)
                ).build(),
                onBufferDownSamplingListener::onBufferDownSampling
            )
        )

        super.buildAudioRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            audioSink,
            eventHandler,
            eventListener,
            out
        )
    }
}
interface BufferDownSamplingListener {
    fun onBufferDownSampling(downSample: Double, positionUs: Long)
}

class BufferDownSamplingListenerAudioRenderer(
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    enableDecoderFallback: Boolean,
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink,
    private val onDownSamplingReady: (Double, Long) -> Unit
) : MediaCodecAudioRenderer(
    context,
    mediaCodecSelector,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    audioSink
) {

    private var lastPositionUs = 0L
    private var mediaOffset = -1L
    private var shouldUpdateOffset = false

    override fun onProcessedStreamChange() {
        super.onProcessedStreamChange()
        shouldUpdateOffset = true
    }

    override fun processOutputBuffer(
        positionUs: Long,
        elapsedRealtimeUs: Long,
        codec: MediaCodecAdapter?,
        buffer: ByteBuffer?,
        bufferIndex: Int,
        bufferFlags: Int,
        sampleCount: Int,
        bufferPresentationTimeUs: Long,
        isDecodeOnlyBuffer: Boolean,
        isLastBuffer: Boolean,
        format: Format
    ): Boolean {
        if (mediaOffset == -1L) {
            mediaOffset = positionUs
        }
        if (shouldUpdateOffset) {
            shouldUpdateOffset = false
            mediaOffset = positionUs
        }
        if (lastPositionUs != positionUs) {
            lastPositionUs = positionUs
            val mediaPositionUs = positionUs - mediaOffset

            val bufferCopy = deepCopy(buffer)
            if (bufferCopy != null) {
                val decoded = bufferCopy.downSample() //todo this is not what we want, but good enough for now
                onDownSamplingReady(decoded, mediaPositionUs)
            }
        }
        return super.processOutputBuffer(
            positionUs,
            elapsedRealtimeUs,
            codec,
            buffer,
            bufferIndex,
            bufferFlags,
            sampleCount,
            bufferPresentationTimeUs,
            isDecodeOnlyBuffer,
            isLastBuffer,
            format
        )
    }

    private fun deepCopy(orig: ByteBuffer?): ByteBuffer? {
        if (orig == null) {
            return null
        }
        val pos = orig.position()
        val lim = orig.limit()
        return try {
            orig.position(0).limit(orig.capacity()) // set range to entire buffer
            val toReturn = deepCopyVisible(orig) // deep copy range
            toReturn.position(pos).limit(lim) // set range to original
            toReturn
        } finally { // do in finally in case something goes wrong we don't bork the orig
            orig.position(pos).limit(lim) // restore original
        }
    }

    private fun deepCopyVisible(orig: ByteBuffer): ByteBuffer {
        val pos = orig.position()
        return try {
            val toReturn = if (orig.isDirect) {
                ByteBuffer.allocateDirect(orig.remaining())
            } else {
                ByteBuffer.allocate(orig.remaining())
            }
            toReturn.put(orig)
            toReturn.order(orig.order())
            toReturn.position(0) as ByteBuffer
        } finally {
            orig.position(pos)
        }
    }

    private fun ByteBuffer.downSample(): Double {
        return array().average().absoluteValue
    }
}