package net.smallacademy.cameraandgallery;


import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.graphics.SurfaceTexture;
import androidx.annotation.NonNull;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraCapture {
    private static final String TAG = "CameraCapture";
    private Context context;
    private Executor executor;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Executor cameraExecutor;
    private boolean canCaptureFrame = true;
    private int numFramesCaptured = 0;

    private int numProcessingThreads = 6;


    private static final int MAX_IMAGES = 30;
    private static final int MAX_IMAGES_PER_SECOND = 30;
    private static final int CAPTURE_INTERVAL_MS = 100; // 100 ms between each frame for 10 FPS
    private static final int CAPTURE_DURATION_SECONDS = 30;

    private final Handler captureDelayHandler = new Handler(Looper.getMainLooper());
    private final Runnable captureDelayRunnable = new Runnable() {
        @Override
        public void run() {
            canCaptureFrame = true;
        }
    };

    private final ExecutorService imageProcessingExecutor = Executors.newFixedThreadPool(numProcessingThreads);

    private final CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            Log.d(TAG, "Opening camera");
            try {
                Log.d(TAG, "Creating preview request builder with surface = " + surface);
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(surface);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                Log.d(TAG, "Preview request builder created: " + previewRequestBuilder);
                Log.d(TAG, "Creating capture session");
                cameraExecutor.execute(() -> {
                    try {
                        cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), captureSessionStateCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error creating capture session", e);
                    }
                });
                // Add a logging statement to indicate that the camera has successfully opened
                Log.d(TAG, "Camera has successfully opened");
            } catch (CameraAccessException e) {
                // Add a more descriptive error message to indicate what went wrong
                Log.e(TAG, "Error setting up camera preview - CameraAccessException: " + e.getMessage(), e);
            } catch (IllegalArgumentException e) {
                // Add a more descriptive error message to indicate what went wrong
                Log.e(TAG, "Error setting up camera preview - IllegalArgumentException: " + e.getMessage(), e);
            } catch (SecurityException e) {
                // Add a more descriptive error message to indicate what went wrong
                Log.e(TAG, "Error setting up camera preview - SecurityException: " + e.getMessage(), e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
            Log.e(TAG, "Camera device was disconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            Log.e(TAG, "Camera device error: " + error);
        }
    };
    private Range<Integer> getBestFpsRange() {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIdList[0]);
            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Range<Integer> bestRange = null;
            int maxUpperBound = 0;
            for (Range<Integer> range : fpsRanges) {
                int upperBound = range.getUpper();
                if (upperBound >= MAX_IMAGES_PER_SECOND && upperBound >= maxUpperBound) {
                    maxUpperBound = upperBound;
                    bestRange = range;
                }
            }
            return bestRange;
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to access camera - CameraAccessException: " + e.getMessage(), e);
        }
        return new Range<>(MAX_IMAGES_PER_SECOND, MAX_IMAGES_PER_SECOND);
    }

    private final CameraCaptureSession.StateCallback captureSessionStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(surface);
                        builder.addTarget(imageReader.getSurface());
                        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getBestFpsRange());
                        // Logging statements to confirm that the builder parameters have been set
                        Log.d(TAG, "Preview capture request builder parameters set:");
                        Log.d(TAG, "Target surface: " + surface);
                        Log.d(TAG, "Image reader surface: " + imageReader.getSurface());
                        Log.d(TAG, "Control mode: " + builder.get(CaptureRequest.CONTROL_MODE));
                        Log.d(TAG, "Auto-exposure mode: " + builder.get(CaptureRequest.CONTROL_AE_MODE));
                        Log.d(TAG, "Auto-white balance mode: " + builder.get(CaptureRequest.CONTROL_AWB_MODE));
                        Log.d(TAG, "Fps range: " + builder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE));
                        CaptureRequest previewRequest = builder.build();
                        cameraCaptureSession = session;
                        session.setRepeatingRequest(previewRequest, null, null);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error setting up preview", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Failed to configure the camera");
                }
            };

    public void startCapture(Context context, Executor executor) {
        Log.d(TAG, "startCapture called with context = " + context + " and executor = " + executor);
        // check if backgroundThread is null
        if (backgroundThread == null) {
            startBackgroundThread();
            Log.d(TAG, "Background Thread started "); //debugging
        } else {
            Log.d(TAG, "Background Thread is not null"); // debugging
        }
        cameraExecutor = Executors.newSingleThreadExecutor();
        this.context = context;
        this.executor = executor;
        surfaceTexture = new SurfaceTexture(/*randomTexture=*/ 0, /*unused=*/ false);
        surfaceTexture.setDefaultBufferSize(/*width=*/ 640, /*height=*/ 480);
        surface = new Surface(surfaceTexture);
        Log.d(TAG, "Surface created: " + surface);

        Log.d(TAG, "Creating ImageReader with surface = " + surface + " and executor = " + executor);
        try {
            Log.d(TAG, "Creating ImageReader");
            imageReader = ImageReader.newInstance(/*width=*/ 640, /*height=*/ 480, /*format=*/ ImageFormat.YUV_420_888, /*maxImages=*/ MAX_IMAGES);
            Log.d(TAG, "ImageReader created: " + imageReader);
            Log.d(TAG, "ImageReader configuration: width = " + imageReader.getWidth() + ", height = " + imageReader.getHeight() + ", format = " + imageReader.getImageFormat() + ", maxImages = " + imageReader.getMaxImages());
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireNextImage();
                    if (image != null) {
                        imageProcessingExecutor.submit(() -> {
                            Log.d(TAG, "Image Sent to Processing");
                            // Process your YUV_420_888 image here with the Processing class
                            // ...
                            image.close();
                            Log.d(TAG, "Image Closed");
                            imageProcessingExecutor.shutdown();
                            Log.d(TAG, "ExecutorService Thread Released");
                        });
                    }
                }
            }, backgroundHandler);
            Log.d(TAG, "onImageAvailable listener set: " + this.getClass().getSimpleName() + "::onImageAvailable");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create ImageReader: " + e.getMessage(), e);
            return;
        }


        Log.d(TAG, "startCapture: capturing frames for " + CAPTURE_DURATION_SECONDS + " seconds");

        // start capturing frames at specified interval
        new Handler(backgroundThread.getLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (numFramesCaptured < (CAPTURE_DURATION_SECONDS * MAX_IMAGES_PER_SECOND)) {
                    if (canCaptureFrame) {
                        canCaptureFrame = false;
                        numFramesCaptured++;
                        Log.d(TAG, "Capturing frame " + numFramesCaptured);
                        try {
                            if (cameraCaptureSession != null) {
                                previewRequestBuilder.addTarget(imageReader.getSurface());
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                cameraCaptureSession.capture(previewRequestBuilder.build(), null, backgroundHandler);
                                previewRequestBuilder.removeTarget(imageReader.getSurface());
                            } else {
                                Log.e(TAG, "CameraCaptureSession is null, cannot capture frame " + numFramesCaptured);
                            }
                        } catch (CameraAccessException e) {
                            Log.e(TAG, "Error capturing frame " + numFramesCaptured, e);
                        }
                    }
                    captureDelayHandler.postDelayed(captureDelayRunnable, CAPTURE_INTERVAL_MS);
                    startCapture(context, executor);
                } else {
                    Log.d(TAG, "Max number of frames captured");
                    stopCapture();
                }
            }
        }, CAPTURE_INTERVAL_MS);


        // Stop capturing images after the desired duration
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                stopCapture();
            }
        }, CAPTURE_DURATION_SECONDS * 1000);
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted when stopping the background thread", e);
            }
        }
    }

    public void stopCapture() {
        if (cameraDevice != null) {
            try {
                if (cameraCaptureSession != null) {
                    // Stop repeating request for collecting frames
                    cameraCaptureSession.stopRepeating();
                    cameraCaptureSession.abortCaptures();
                    cameraCaptureSession.close();
                    cameraCaptureSession = null;
                }
                cameraDevice.close();
                cameraDevice = null;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        // Stop the background thread for camera operations
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while stopping background thread", e);
            }
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    public void release() {
        stopBackgroundThread();
        if (imageProcessingExecutor != null) {
            imageProcessingExecutor.shutdown();
        }
    }
}
