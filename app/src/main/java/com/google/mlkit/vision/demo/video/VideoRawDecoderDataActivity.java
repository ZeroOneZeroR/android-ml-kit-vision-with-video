package com.google.mlkit.vision.demo.video;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.mlkit.vision.demo.java.videoactivity.YuvToRgbConverter;

import java.nio.ByteBuffer;

public class VideoRawDecoderDataActivity extends VideoBaseActivity implements
        CustomRenderersFactory.VideoFrameDataListener{

    private ImageView frameImageView;
    private YuvToRgbConverter yuvToRgbConverter;
    private int frameWidth, frameHeight;

    @NonNull
    @Override
    protected SimpleExoPlayer createPlayer() {
        CustomRenderersFactory renderersFactory = new CustomRenderersFactory(this).setVideoFrameDataListener(this);
        return new SimpleExoPlayer.Builder(this, renderersFactory).build();
    }

    @Override
    protected View createVideoFrameView() {
        frameImageView = new ImageView(this);
        return frameImageView;
    }

    @Override
    protected void onProcessComplete(Bitmap frame) {
        super.onProcessComplete(frame);
        frameImageView.setImageBitmap(frame);
    }

    @Override
    public void onFrame(@Nullable ByteBuffer data, MediaFormat androidMediaFormat, Format playerFormat) {
        // Not in main thread
        if(data != null){
            /*
            * Color formats of different decoders are different.
            * We have to apply different raw-data to Bitmap(argb) conversion systems according to color format.
            * Here we just show YUV to RGB conversion assuming data is YUV formatted.
            * Following conversion system might not give proper result for all videos.
            */

            try {
                int width = playerFormat.width;
                int height = playerFormat.height;
                int rotation = playerFormat.rotationDegrees;
                int colorFormat = androidMediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                /*if(rotation == 90 || rotation == 270){
                    int t = width;
                    width = height;
                    height = t;
                }*/

                data.rewind();
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                if(frameWidth != bitmap.getWidth() || frameHeight != bitmap.getHeight()){
                    frameWidth = bitmap.getWidth();
                    frameHeight = bitmap.getHeight();
                    if(yuvToRgbConverter != null) yuvToRgbConverter.release();
                    yuvToRgbConverter = new YuvToRgbConverter(this);
                }
                yuvToRgbConverter.yuvToRgb(bytes, bitmap, ImageFormat.NV21);

                /*Bitmap bitmap = BitmapUtils.getBitmap(data, new FrameMetadata.Builder()
                        .setWidth(width)
                        .setHeight(height)
                        .setRotation(rotation)
                        .build());*/

                /*Renderscript tool-kit can also be used for conversion*/

                Size size = getSizeForDesiredSize(width, height, 500);
                Bitmap finalBitmap = Bitmap.createScaledBitmap(bitmap, size.getWidth(), size.getHeight(), true);

                runOnUiThread(()->{
                    //frameImageView.setImageBitmap(finalBitmap);
                    processFrame(finalBitmap);
                });
            }catch (Exception e){
                Log.e("TAG", "onFrame: error: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(yuvToRgbConverter != null){
            yuvToRgbConverter.release();
        }
    }
}
