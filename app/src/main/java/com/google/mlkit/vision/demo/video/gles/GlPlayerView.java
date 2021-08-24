package com.google.mlkit.vision.demo.video.gles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.video.VideoListener;

public class GlPlayerView extends GLSurfaceView implements VideoListener {

    private final static String TAG = GlPlayerView.class.getSimpleName();

    private static final int EGL_CONTEXT_CLIENT_VERSION = 2;

    public enum ScaleType {
        RESIZE_FIT_WIDTH,  //
        RESIZE_FIT_HEIGHT, //
        RESIZE_NONE   // The specified aspect ratio is ignored.
    }

    private final GlPlayerRenderer renderer;
    private SimpleExoPlayer player;

    private float videoAspect = 1f;
    private ScaleType glPlayerViewScaleType = ScaleType.RESIZE_FIT_WIDTH;

    public GlPlayerView(Context context) {
        this(context, null);
    }

    public GlPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextFactory(new DefaultContextFactory(EGL_CONTEXT_CLIENT_VERSION));
        setEGLConfigChooser(new DefaultConfigChooser(false, EGL_CONTEXT_CLIENT_VERSION));

        renderer = new GlPlayerRenderer(this);
        setRenderer(renderer);

    }

    public GlPlayerView setSimpleExoPlayer(SimpleExoPlayer player) {
        if (this.player != null) {
            this.player.release();
            this.player = null;
        }
        this.player = player;
        this.player.addVideoListener(this);
        this.renderer.setSimpleExoPlayer(player);
        return this;
    }

    public void setPlayerScaleType(ScaleType glPlayerViewScaleType) {
        this.glPlayerViewScaleType = glPlayerViewScaleType;
        requestLayout();
    }

    public void setFrameListener(GlPlayerRenderer.FrameListener frameListener){
        renderer.setFrameListener(frameListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();

        int viewWidth = measuredWidth;
        int viewHeight = measuredHeight;

        switch (glPlayerViewScaleType) {
            case RESIZE_FIT_WIDTH:
                viewHeight = (int) (measuredWidth / videoAspect);
                break;
            case RESIZE_FIT_HEIGHT:
                viewWidth = (int) (measuredHeight * videoAspect);
                break;
        }

        setMeasuredDimension(viewWidth, viewHeight);
    }

    @Override
    public void onPause() {
        super.onPause();
        renderer.release();
    }

    // SimpleExoPlayer.VideoListener
    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        videoAspect = ((float) width / height) * pixelWidthHeightRatio;
        requestLayout();
    }

    @Override
    public void onRenderedFirstFrame() {
        // do nothing
    }
}

