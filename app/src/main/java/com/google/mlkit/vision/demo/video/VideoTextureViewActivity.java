package com.google.mlkit.vision.demo.video;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.SimpleExoPlayer;

public class VideoTextureViewActivity extends VideoBaseActivity implements TextureView.SurfaceTextureListener {

    private SimpleExoPlayer player;
    private TextureView textureView;
    private Surface playerSurface;
    private SurfaceTexture surfaceTexture;

    @Override
    protected SimpleExoPlayer createPlayer() {
        player = new SimpleExoPlayer.Builder(this).build();
        return player;
    }

    @Override
    protected View createVideoFrameView() {
        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(this);
        return textureView;
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        surfaceTexture = surface;
        playerSurface = new Surface(surface);
        player.setVideoSurface(playerSurface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        player.setVideoSurface(null);
        playerSurface.release();
        playerSurface = null;
        surfaceTexture.release();
        surfaceTexture = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        Size size = getSizeForDesiredSize(textureView.getWidth(), textureView.getHeight(), 500);
        processFrame(textureView.getBitmap(size.getWidth(), size.getHeight()));
    }
}
