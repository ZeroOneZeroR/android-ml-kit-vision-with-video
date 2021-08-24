package com.google.mlkit.vision.demo.video;

import android.content.Context;
import android.media.Image;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CustomRenderersFactory extends DefaultRenderersFactory {
    private static final String TAG = CustomRenderersFactory.class.getName();
    private VideoFrameDataListener videoFrameDataListener;

    public CustomRenderersFactory(Context context) {
        super(context);
    }

    public CustomRenderersFactory setVideoFrameDataListener(VideoFrameDataListener videoFrameDataListener) {
        this.videoFrameDataListener = videoFrameDataListener;
        return this;
    }

    @Override
    protected void buildVideoRenderers(Context context,
                                       int extensionRendererMode,
                                       MediaCodecSelector mediaCodecSelector,
                                       boolean enableDecoderFallback,
                                       Handler eventHandler,
                                       VideoRendererEventListener eventListener,
                                       long allowedVideoJoiningTimeMs,
                                       ArrayList<Renderer> out) {

        CustomMediaCodecVideoRenderer videoRenderer =
                new CustomMediaCodecVideoRenderer(
                        context,
                        mediaCodecSelector,
                        allowedVideoJoiningTimeMs,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);
        videoRenderer.experimentalSetAsynchronousBufferQueueingEnabled(false);
        videoRenderer.experimentalSetForceAsyncQueueingSynchronizationWorkaround(false);
        videoRenderer.experimentalSetSynchronizeCodecInteractionsWithQueueingEnabled(false);
        out.add(videoRenderer);

        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return;
        }
        int extensionRendererIndex = out.size();
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--;
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            Class<?> clazz = Class.forName("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer");
            Constructor<?> constructor =
                    clazz.getConstructor(
                            long.class,
                            Handler.class,
                            VideoRendererEventListener.class,
                            int.class);
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)
            Renderer renderer =
                    (Renderer)
                            constructor.newInstance(
                                    allowedVideoJoiningTimeMs,
                                    eventHandler,
                                    eventListener,
                                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);
            out.add(extensionRendererIndex++, renderer);
            com.google.android.exoplayer2.util.Log.i(TAG, "Loaded LibvpxVideoRenderer.");
        } catch (ClassNotFoundException e) {
            // Expected if the app was built without the extension.
        } catch (Exception e) {
            // The extension is present, but instantiation failed.
            throw new RuntimeException("Error instantiating VP9 extension", e);
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            Class<?> clazz = Class.forName("com.google.android.exoplayer2.ext.av1.Libgav1VideoRenderer");
            Constructor<?> constructor =
                    clazz.getConstructor(
                            long.class,
                            Handler.class,
                            VideoRendererEventListener.class,
                            int.class);
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)
            Renderer renderer =
                    (Renderer)
                            constructor.newInstance(
                                    allowedVideoJoiningTimeMs,
                                    eventHandler,
                                    eventListener,
                                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);
            out.add(extensionRendererIndex++, renderer);
            Log.i(TAG, "Loaded Libgav1VideoRenderer.");
        } catch (ClassNotFoundException e) {
            // Expected if the app was built without the extension.
        } catch (Exception e) {
            // The extension is present, but instantiation failed.
            throw new RuntimeException("Error instantiating AV1 extension", e);
        }

        /*super.buildVideoRenderers(context,
                extensionRendererMode,
                mediaCodecSelector, enableDecoderFallback,
                eventHandler,
                eventListener,
                allowedVideoJoiningTimeMs, out);*/

    }

    private class CustomMediaCodecVideoRenderer extends MediaCodecVideoRenderer {

        public CustomMediaCodecVideoRenderer(Context context,
                                             MediaCodecSelector mediaCodecSelector,
                                             long allowedJoiningTimeMs,
                                             boolean enableDecoderFallback,
                                             @Nullable Handler eventHandler,
                                             @Nullable VideoRendererEventListener eventListener,
                                             int maxDroppedFramesToNotify) {
            super(context,
                    new CustomMediaCodecAdapter.Factory(),
                    mediaCodecSelector,
                    allowedJoiningTimeMs,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    maxDroppedFramesToNotify);
        }

        @Override
        protected boolean processOutputBuffer(long positionUs,
                                              long elapsedRealtimeUs,
                                              @Nullable MediaCodecAdapter codec,
                                              @Nullable ByteBuffer buffer,
                                              int bufferIndex,
                                              int bufferFlags,
                                              int sampleCount,
                                              long bufferPresentationTimeUs,
                                              boolean isDecodeOnlyBuffer,
                                              boolean isLastBuffer,
                                              Format format) throws ExoPlaybackException {
            if(videoFrameDataListener != null && codec != null){
                CustomMediaCodecAdapter codecAdapter = (CustomMediaCodecAdapter) codec;
                //Image image = codecAdapter.getOutputImage(bufferIndex);
                //buffer = codec.getOutputBuffer(bufferIndex);
                videoFrameDataListener.onFrame(buffer, codec.getOutputFormat(), format);
            }
            boolean process = super.processOutputBuffer(
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
                    format);
            return process;
        }
    }

    public interface VideoFrameDataListener{
        void onFrame(@Nullable ByteBuffer data, MediaFormat androidMediaFormat, Format playerFormat);
        //void onFrame(Image image, MediaFormat androidMediaFormat, Format playerFormat);
    }
}
