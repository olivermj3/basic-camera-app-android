
package net.smallacademy.cameraandgallery;// MainActivity.java

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraCaptureSession;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.glutil.EglManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executor;

public class CameraCapture {
    private static final String TAG = "CameraCapture";
    private Context context;
    private Executor executor;
    private EglManager eglManager;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private int frameCount;
    private boolean capturing;

    // declare the camera capture session variable
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;

    private final CameraCaptureSession.StateCallback captureSessionStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        CaptureRequest previewRequest = previewRequestBuilder.build();
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

    private final CameraDevice.StateCallback cameraDeviceStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewRequestBuilder.addTarget(surface);
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                                captureSessionStateCallback, null);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error setting up camera preview", e);
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

    public void startCapture(Context context, Executor executor) {
        this.context = context;
        this.executor = executor;
        eglManager = new EglManager(null);
        surfaceTexture = new SurfaceTexture(/*randomTexture=*/ 0, /*unused=*/ false);
        surfaceTexture.setDefaultBufferSize(/*width=*/ 640, /*height=*/ 480);
        surface = new Surface(surfaceTexture);
        imageReader = ImageReader.newInstance(/*width=*/ 640, /*height=*/ 480, /*format=*/ ImageFormat.YUV_420_888, /*maxImages=*/ 10);
        imageReader.setOnImageAvailableListener(this::onImageAvailable, new Handler(Looper.getMainLooper()));
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        CaptureRequest.Builder previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewRequestBuilder.addTarget(surface);
                        camera.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession session) {
                                        try {
                                            CaptureRequest previewRequest = previewRequestBuilder.build();
                                            session.setRepeatingRequest(previewRequest, null, null);
                                        } catch (CameraAccessException e) {
                                            Log.e(TAG, "Error setting up preview", e);
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                        Log.e(TAG, "Failed to configure the camera");
                                    }
                                }, null);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error setting up camera preview", e);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void onImageAvailable(ImageReader reader) {
        // You can handle the available image here, e.g., for further processing or saving.
    }
    public void stopCapture() {
        if (cameraDevice != null) {
            try {
                if (cameraCaptureSession != null) {
                    // add a state callback to check if the camera capture session is still active
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
    }

}