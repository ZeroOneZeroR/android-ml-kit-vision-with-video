# Android ML Kit Vision demo with Video
Google's ML-Kit-Vision demo (android) for pre encoded video. Demos for camera preview and still image are also included. This project is actually extension of Google's own sample. Google's sample link: https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart

# Motivation
Google's sample does not show how to work with pre encoded video, though it shows the processes for camera preview and still image.

# About the app
We play a video with exoplayer and get every frame using different techniques mentioned later. Then we process those frames with ML Kit Vision api.

Current launcher activty of the app is ".video.ChooserActivity". To see the the implementaios for camera preview and still image, make ".EntryChoiceActivity" as launcher activity.
To know more about the app read from [here](https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/README.md).

## Video frame grabbing techniques:
* Reading pixels from OpenGL-ES context
* Getting bitmap from TextureView
* Converting Raw decoder data with renderscript

## Preview
![](preview1.gif) ![](preview2.gif)
