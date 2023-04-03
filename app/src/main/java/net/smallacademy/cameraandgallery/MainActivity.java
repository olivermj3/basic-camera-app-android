package net.smallacademy.cameraandgallery;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private CameraCapture cameraCapture;
    private Executor executor = Executors.newSingleThreadExecutor();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private void checkAndRequestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not yet granted");
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "Permission granted");
                startCamera();
            }
        } else {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not yet granted");
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "Permission granted");
                startCamera();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Make sure you have the correct layout file here

        checkAndRequestCameraPermission();

        Button cameraBtn = findViewById(R.id.cameraBtn);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestCameraPermission();
            }
        });
    }

    private void startCamera() {
        cameraCapture = new CameraCapture();
        Log.d(TAG, "startCapture method called");
        cameraCapture.startCapture(this, executor);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cameraCapture.stopCapture();
            }
        }, 3000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                // Handle denied camera permission
            }
        }
    }

}
