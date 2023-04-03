# basic-camera-app-android

This app was forked off a basic camera app on github to take advantage of structure
but has otherwise been entirely rewritten. The original app was called smallacademy.cameraandgallery
which is why you see that throughout. ignore and rename for yourself.

It has two scripts at this point: MainActivity and CameraCapture which are in 
app/java/net.smallacademy.cameraandgallery. The build.gradle files at the app and project
levels have both been updated to match. I also stripped the gallery button from the ui.

The MainActivity kicks off app, sets layout, checks for camera permission, and kicks off
the CameraCapture class when the camera button is pushed.

The CameraCapture class is an Android camera utility that helps capture images from the device's 
camera using the Camera2 API. This class can be used to start and stop capturing images, and it 
processes the images in the YUV_420_888 format.

#CameraCapture
Here's a breakdown of the class members and methods in CameraCapture:

##Class Variables:

TAG: A String used for logging purposes.
context: The application context.
executor: An Executor for running tasks in the background.
surfaceTexture, surface: SurfaceTexture and Surface objects used for preview.
cameraDevice: A CameraDevice instance representing the camera.
imageReader: An ImageReader instance for reading images from the camera.
cameraCaptureSession: A CameraCaptureSession instance for the camera session.
previewRequestBuilder: A CaptureRequest.Builder for building the preview request.

##Callbacks:

captureSessionStateCallback: A callback object for handling CameraCaptureSession state changes. 
The onConfigured method sets up the camera preview, while the onConfigureFailed method logs any 
configuration failures.
cameraDeviceStateCallback: A callback object for handling CameraDevice state changes. 
The onOpened method sets up the camera preview, the onDisconnected method closes the camera and 
logs disconnection, and the onError method handles any errors that occur.

##Public Methods:

startCapture(Context context, Executor executor): Initializes the camera and starts capturing 
images. It sets up the SurfaceTexture, Surface, and ImageReader instances and opens the camera.
stopCapture(): Stops capturing images, aborts ongoing captures, and closes the camera session, 
camera device, and image reader.

##Private Methods:

onImageAvailable(ImageReader reader): A callback method that's invoked when a new image is 
available. It reads the YUV_420_888 image data from the image planes, extracts the timestamp, 
and sends the image data and timestamp to the Processing class for further analysis.

#Processing
The Processing class  is an Android image processing utility that 
helps analyze images using Google's ML Kit Face Detection API. The class extracts color values 
at specific points of interest on a detected face and saves the color values as a JSON file.

Here's a breakdown of the class members and methods:

##Class Variables:

image: An Image instance representing the input image.
gson: A Gson instance for converting data to JSON format.
Constructor:

Processing(@NonNull Image image): Initializes the Processing instance with the input image and 
creates an InputImage instance for ML Kit processing.

##Private Methods:

extractColorValues(InputImage inputImage): Sets up the FaceDetector with performance mode, 
processes the input image to detect faces, extracts the color values at the points of interest, 
and saves the color values as a JSON file.
getColorValue(InputImage inputImage, int x, int y): Returns the color value at the given (x, y) 
coordinates in the input image. It calculates the RGB values using the YUV data from the input image.
clip(int value): Clamps the input value to the range [0, 255].
saveColorValuesToJson(HashMap<String, Integer> colorValues): Saves the extracted color values as a JSON file.
printColorValues(HashMap<String, Integer> colorValues): Prints the extracted color values to the console.
