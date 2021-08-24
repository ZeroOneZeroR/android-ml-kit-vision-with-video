package com.google.mlkit.vision.demo.video;

import android.graphics.Bitmap;
import android.view.View;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.mlkit.vision.demo.video.gles.GlPlayerRenderer;
import com.google.mlkit.vision.demo.video.gles.GlPlayerView;

public class VideoGLESActivity extends VideoBaseActivity implements GlPlayerRenderer.FrameListener {

    private SimpleExoPlayer player;
    private GlPlayerView glPlayerView;

    @Override
    protected SimpleExoPlayer createPlayer() {
        player = new SimpleExoPlayer.Builder(this).build();
        return player;
    }

    @Override
    protected View createVideoFrameView() {
        glPlayerView = new GlPlayerView(this);
        glPlayerView.setSimpleExoPlayer(player);
        glPlayerView.setFrameListener(this);
        return glPlayerView;
    }

    @Override
    public void onFrame(Bitmap bitmap) {
        processFrame(bitmap);
    }
}
