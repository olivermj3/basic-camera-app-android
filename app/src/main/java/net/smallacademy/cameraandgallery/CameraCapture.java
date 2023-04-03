package net.smallacademy.cameraandgallery;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;

import android.Manifest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import android.util.Log;
import android.util.Size;

import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executor;
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

        private static final int MAX_IMAGES = 30;

        private static final int MAX_IMAGES_PER_SECOND = 10;

        private static final int CAPTURE_INTERVAL_MS = 100; // 100 ms between each frame for 10 FPS

        private static final int CAPTURE_DURATION_SECONDS = 3;
        private int numFramesCaptured = 0;
        private Executor cameraExecutor;


        private final Handler captureDelayHandler = new Handler(Looper.getMainLooper());
        private final Runnable captureDelayRunnable = new Runnable() {
            @Override
            public void run() {
                canCaptureFrame = true;
            }
        };
        private boolean canCaptureFrame = true;

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


        private final CameraCaptureSession.StateCallback captureSessionStateCallback =
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            CaptureRequest previewRequest = previewRequestBuilder.build();
                            Log.e(TAG, "The camera CaptureRequest previewRequest was built");
                            cameraCaptureSession = session;
                            session.setRepeatingRequest(previewRequest, null, null);
                            Log.e(TAG, "The camera is configured");
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

            cameraExecutor = Executors.newSingleThreadExecutor();
            this.context = context;
            this.executor = executor;
            surfaceTexture = new SurfaceTexture(/*randomTexture=*/ 0, /*unused=*/ false);
            surfaceTexture.setDefaultBufferSize(/*width=*/ 640, /*height=*/ 480);
            surface = new Surface(surfaceTexture);
            Log.d(TAG, "Surface created: " + surface);

            Log.d(TAG, "Creating ImageReader with surface = " + surface + " and executor = " + executor);
            try {
                imageReader = ImageReader.newInstance(/*width=*/ 640, /*height=*/ 480, /*format=*/ ImageFormat.YUV_420_888, /*maxImages=*/ MAX_IMAGES);
                Log.d(TAG, "ImageReader configuration: width = " + imageReader.getWidth() + ", height = " + imageReader.getHeight() + ", format = " + imageReader.getImageFormat() + ", maxImages = " + imageReader.getMaxImages());
                imageReader.setOnImageAvailableListener(this::onImageAvailable, new Handler(Looper.getMainLooper()));
            } catch (Exception e) {
                Log.e(TAG, "Failed to create ImageReader: " + e.getMessage(), e);
                return;
            }

            Log.d(TAG, "startCapture: capturing frames for " + CAPTURE_DURATION_SECONDS + " seconds");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopCapture();
                }
            }, CAPTURE_DURATION_SECONDS * 1000);

            Log.d(TAG, "Getting CameraManager instance");
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                Log.e(TAG, "Failed to get CameraManager instance");
                return;
            }

            Log.d(TAG, "Getting camera ID list");
            try {
                String[] cameraIdList = cameraManager.getCameraIdList();
                if (cameraIdList.length == 0) {
                    Log.e(TAG, "No camera devices found");
                    return;
                }

                String cameraId = cameraIdList[0];
                Log.d(TAG, "Camera ID: " + cameraId);

                Log.d(TAG, "Getting camera characteristics");
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (streamConfigurationMap == null) {
                    Log.e(TAG, "Failed to get stream configuration map");
                    return;
                }

                Size[] outputSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
                if (outputSizes == null || outputSizes.length == 0) {
                    Log.e(TAG, "Failed to get output sizes");
                    return;
                }
                for (Size size : outputSizes) {
                    Log.d(TAG, "Output size: " + size.toString());
                }

                int[] formats = streamConfigurationMap.getOutputFormats();
                for (int format : formats) {
                    Log.d(TAG, "Output format: " + format);
                }

                int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                for (int capability : capabilities) {
                    Log.d(TAG, "Camera capability: " + capability);
                }
                // Start background thread for camera operations
                backgroundThread = new HandlerThread("CameraBackground");
                backgroundThread.start();
                backgroundHandler = new Handler(backgroundThread.getLooper());

                // Open camera with the backgroundHandler
                cameraManager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);

                // Add a logging statement to indicate that the camera device has been successfully opened
                Log.d(TAG, "Camera device has been successfully opened");

            } catch (CameraAccessException e) {
                // Add a more descriptive error message to indicate what went wrong
                Log.e(TAG, "Failed to access camera - CameraAccessException: " + e.getMessage(), e);
            } catch (SecurityException e) {
                // Add a more descriptive error message to indicate what went wrong
                Log.e(TAG, "No camera permission - SecurityException: " + e.getMessage(), e);
            }

            // Stop capturing images after the desired duration
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopCapture();
                }
            }, CAPTURE_DURATION_SECONDS * 1000);
        }



        private void onImageAvailable(ImageReader reader) {
            if (!canCaptureFrame) {
                return;
            }

            numFramesCaptured++; // Increment the counter for debugging
            Log.d(TAG, "onImageAvailable called");
            Image image = reader.acquireNextImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length == 3) {
                    // Get the YUV_420_888 data from the planes
                    ByteBuffer yBuffer = planes[0].getBuffer();
                    ByteBuffer uBuffer = planes[1].getBuffer();
                    ByteBuffer vBuffer = planes[2].getBuffer();
                    int ySize = yBuffer.remaining();
                    int uSize = uBuffer.remaining();
                    int vSize = vBuffer.remaining();
                    byte[] yuvData = new byte[ySize + uSize + vSize];
                    yBuffer.get(yuvData, 0, ySize);
                    uBuffer.get(yuvData, ySize, uSize);
                    vBuffer.get(yuvData, ySize + uSize, vSize);

                    // Extract timestamp from image
                    long timeStamp = image.getTimestamp();
                    Processing.processYuv420888(yuvData, image.getWidth(), image.getHeight(), timeStamp);
                }
                Log.d(TAG, "onImageAvailable: captured frame " + numFramesCaptured); // for debugging
            }
            image.close();

            // Set a delay before the next frame can be captured
            canCaptureFrame = false;
            captureDelayHandler.postDelayed(captureDelayRunnable, CAPTURE_INTERVAL_MS);
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

}
