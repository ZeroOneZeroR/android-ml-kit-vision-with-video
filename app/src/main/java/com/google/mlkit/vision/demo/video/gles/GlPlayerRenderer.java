package com.google.mlkit.vision.demo.video.gles;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import com.google.android.exoplayer2.SimpleExoPlayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;

public class GlPlayerRenderer implements SurfaceTexture.OnFrameAvailableListener, GLSurfaceView.Renderer {
    private static final String TAG = GlPlayerRenderer.class.getSimpleName();

    private float[] MVPMatrix = new float[16];
    private float[] ProjMatrix = new float[16];
    private float[] MMatrix = new float[16];
    private float[] VMatrix = new float[16];
    private float[] STMatrix = new float[16];

    private int texName;
    private SurfaceTexture previewTexture;
    private boolean updateSurface = false;

    private float aspectRatio = 1f;
    private int width, height;

    private GlPreviewFilter previewFilter;
    private final GlPlayerView glPreview;
    private SimpleExoPlayer simpleExoPlayer;
    private FrameListener frameListener;

    private Handler handler;

    GlPlayerRenderer(GlPlayerView glPreview) {
        super();
        Matrix.setIdentityM(STMatrix, 0);
        this.glPreview = glPreview;
        handler = new Handler(Looper.getMainLooper());
    }

    void setSimpleExoPlayer(SimpleExoPlayer simpleExoPlayer) {
        this.simpleExoPlayer = simpleExoPlayer;
    }

    public void setFrameListener(FrameListener frameListener) {
        this.frameListener = frameListener;
    }

    private int createExternalTexture(){
        int[] texId = new int[1];
        GLES20.glGenTextures(1, IntBuffer.wrap(texId));
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId[0]);
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return texId[0];
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        texName = createExternalTexture();

        previewTexture = new SurfaceTexture(texName);
        previewTexture.setOnFrameAvailableListener(this);
        Surface surface = new Surface(previewTexture);

        synchronized (this) {
            updateSurface = false;
        }

        previewFilter = new GlPreviewFilter(GlPreviewFilter.GL_TEXTURE_EXTERNAL_OES);
        previewFilter.setup();

        handler.post(()->{
            if(simpleExoPlayer != null){
                simpleExoPlayer.setVideoSurface(surface);
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        aspectRatio = (float) width / height;

        previewFilter.setFrameSize(width, height);

        Matrix.frustumM(ProjMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 5, 7);
        Matrix.setIdentityM(MMatrix, 0);
        Matrix.setLookAtM(VMatrix, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );

        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (updateSurface) {
                previewTexture.updateTexImage();
                previewTexture.getTransformMatrix(STMatrix);
                updateSurface = false;
            }
        }

        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        Matrix.multiplyMM(MVPMatrix, 0, VMatrix, 0, MMatrix, 0);
        Matrix.multiplyMM(MVPMatrix, 0, ProjMatrix, 0, MVPMatrix, 0);

        previewFilter.draw(texName, MVPMatrix, STMatrix, aspectRatio);

        if(frameListener != null){
            ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);
            pixelBuffer.order(ByteOrder.LITTLE_ENDIAN);
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
            pixelBuffer.rewind();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(pixelBuffer);

            // for vertical flip
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setScale( 1,-1);
            matrix.postTranslate( 0, bitmap.getHeight());
            Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            handler.post(()->{
                frameListener.onFrame(finalBitmap);
            });
        }
    }

    @Override
    public synchronized void onFrameAvailable(final SurfaceTexture previewTexture) {
        updateSurface = true;
        glPreview.requestRender();
    }

    void release() {
        if (previewTexture != null) {
            previewTexture.release();
        }
    }

    public interface FrameListener{
        void onFrame(Bitmap bitmap);
    }
}
