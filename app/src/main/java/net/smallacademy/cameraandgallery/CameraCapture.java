package net.smallacademy.cameraandgallery;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executor;

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
    }

    private void onImageAvailable(ImageReader reader) {
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
        }
        image.close();
    }


    public void stopCapture() {
        if (cameraDevice != null) {
            try {
                if (cameraCaptureSession != null) {
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
    }
}
