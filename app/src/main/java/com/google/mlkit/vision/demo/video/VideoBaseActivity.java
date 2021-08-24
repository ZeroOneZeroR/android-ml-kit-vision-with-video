package com.google.mlkit.vision.demo.video;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.java.VisionProcessorBase;
import com.google.mlkit.vision.demo.java.barcodescanner.BarcodeScannerProcessor;
import com.google.mlkit.vision.demo.java.facedetector.FaceDetectorProcessor;
import com.google.mlkit.vision.demo.java.labeldetector.LabelDetectorProcessor;
import com.google.mlkit.vision.demo.java.segmenter.SegmenterProcessor;
import com.google.mlkit.vision.demo.java.textdetector.TextRecognitionProcessor;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.List;

public abstract class VideoBaseActivity extends AppCompatActivity{
    private static final String TAG = VideoBaseActivity.class.getSimpleName();

    private static final int REQUEST_CHOOSE_VIDEO = 1001;

    private static final String FACE_DETECTION = "Face Detection";
    private static final String BARCODE_SCANNING = "Barcode Scanning";
    private static final String TEXT_RECOGNITION = "Text Recognition";
    private static final String IMAGE_LABELING = "Image Labeling";
    private static final String IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)";
    private static final String CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)";
    private static final String SELFIE_SEGMENTATION = "Selfie Segmentation";

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private GraphicOverlay graphicOverlay;

    private VisionProcessorBase imageProcessor;
    private String selectedProcessor = FACE_DETECTION;

    private int frameWidth, frameHeight;

    private boolean processing;
    private boolean pending;
    private Bitmap lastFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_video);

        player = createPlayer();

        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);
        FrameLayout contentFrame = playerView.findViewById(R.id.exo_content_frame);
        View videoFrameView = createVideoFrameView();
        if(videoFrameView != null) contentFrame.addView(videoFrameView);

        graphicOverlay = new GraphicOverlay(this, null);
        contentFrame.addView(graphicOverlay);

        populateProcessorSelector();
        findViewById(R.id.choose_btn).setOnClickListener(v -> {
            startChooseVideoIntentForResult();
        });
    }

    protected abstract @NonNull SimpleExoPlayer createPlayer();
    protected abstract @Nullable View createVideoFrameView();

    protected Size getSizeForDesiredSize(int width, int height, int desiredSize){
        int w, h;
        if(width > height){
            w = desiredSize;
            h = Math.round((height/(float)width) * w);
        }else{
            h = desiredSize;
            w = Math.round((width/(float)height) * h);
        }
        return new Size(w, h);
    }

    protected void processFrame(Bitmap frame){
        lastFrame = frame;
        if(imageProcessor != null){
            pending = processing;
            if(!processing){
                processing = true;
                if(frameWidth != frame.getWidth() || frameHeight != frame.getHeight()){
                    frameWidth = frame.getWidth();
                    frameHeight = frame.getHeight();
                    graphicOverlay.setImageSourceInfo(frameWidth, frameHeight, false);
                }
                imageProcessor.setOnProcessingCompleteListener(new VisionProcessorBase.OnProcessingCompleteListener() {
                    @Override
                    public void onProcessingComplete() {
                        processing = false;
                        onProcessComplete(frame);
                        if(pending) processFrame(lastFrame);
                    }
                });
                imageProcessor.processBitmap(frame, graphicOverlay);
            }
        }
    }

    protected void onProcessComplete(Bitmap frame){ }

    @Override
    protected void onResume() {
        super.onResume();
        createImageProcessor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.pause();
        stopImageProcessor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
    }

    private void setupPlayer(Uri uri){
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.stop();
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void startChooseVideoIntentForResult() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_CHOOSE_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHOOSE_VIDEO && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            setupPlayer(data.getData());
        }
    }

    private void populateProcessorSelector() {
        Spinner featureSpinner = findViewById(R.id.processor_selector);
        List<String> options = new ArrayList<>();
        options.add(FACE_DETECTION);
        options.add(BARCODE_SCANNING);
        options.add(TEXT_RECOGNITION);
        options.add(IMAGE_LABELING);
        options.add(IMAGE_LABELING_CUSTOM);
        options.add(CUSTOM_AUTOML_LABELING);
        options.add(SELFIE_SEGMENTATION);

        // Creating adapter for featureSpinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        featureSpinner.setAdapter(dataAdapter);
        featureSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                        selectedProcessor = parentView.getItemAtPosition(pos).toString();
                        createImageProcessor();
                        if(lastFrame != null) processFrame(lastFrame);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {}
                });
    }

    private void createImageProcessor() {
        stopImageProcessor();

        try {
            switch (selectedProcessor) {
                case FACE_DETECTION:
                    imageProcessor = new FaceDetectorProcessor(this);
                    break;
                case BARCODE_SCANNING:
                    imageProcessor = new BarcodeScannerProcessor(this);
                    break;
                case TEXT_RECOGNITION:
                    imageProcessor = new TextRecognitionProcessor(this);
                    break;
                case IMAGE_LABELING:
                    imageProcessor = new LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS);
                    break;
                case IMAGE_LABELING_CUSTOM:
                    LocalModel localClassifier =
                            new LocalModel.Builder()
                                    .setAssetFilePath("custom_models/bird_classifier.tflite")
                                    .build();
                    CustomImageLabelerOptions customImageLabelerOptions =
                            new CustomImageLabelerOptions.Builder(localClassifier).build();
                    imageProcessor = new LabelDetectorProcessor(this, customImageLabelerOptions);
                    break;
                case CUSTOM_AUTOML_LABELING:
                    LocalModel customAutoMLLabelLocalModel =
                            new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();
                    CustomImageLabelerOptions customAutoMLLabelOptions =
                            new CustomImageLabelerOptions.Builder(customAutoMLLabelLocalModel)
                                    .setConfidenceThreshold(0)
                                    .build();
                    imageProcessor = new LabelDetectorProcessor(this, customAutoMLLabelOptions);
                    break;
                case SELFIE_SEGMENTATION:
                    imageProcessor = new SegmenterProcessor(this, /* isStreamMode= */ true);
                    break;
                default:
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + selectedProcessor, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void stopImageProcessor(){
        if(imageProcessor != null){
            imageProcessor.stop();
            imageProcessor = null;
            processing = false;
            pending = false;
        }
    }
}