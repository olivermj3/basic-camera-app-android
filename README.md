# basic-camera-app-android

This app was forked off a basic camera app on github to take advantage of structure
but has otherwise been entirely rewritten. The original app was called smallacademy.cameraandgallery
which is why you see that throughout. ignore and rename for yourself.

It has two scripts at this point: MainActivity and CameraCapture which are in 
app/java/net.smallacademy.cameraandgallery. The build.gradle files at the app and project
levels have both been updated to match. I also stripped the gallery button from the ui.

The MainActivity kicks off app, sets layout, checks for camera permission, and kicks off
the CameraCapture class when the camera button is pushed.

CameraCapture has some basic error handling since the project currently has the camera
disconnecting.

The CameraCapture class has a method called startCapture, which captures individual images in
the ImageFormat.YUV_420_888 format. The variables are currently set for collection of
three seconds to shorten the run time during app building. 

The next steps are to debug the camera disconnection issue. then adjust the acquisition to force
a specific frame rate.

There is a method held in CameraCapture called OnImageAvailable which is a placeholder for all of 
the preprocessing code. We will call out individual classes for the major preprocessing steps
for simplicity in debugging.