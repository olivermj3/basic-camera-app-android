package net.smallacademy.cameraandgallery;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
// test comment
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private CameraCapture cameraCapture;
    private Executor executor = Executors.newSingleThreadExecutor();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity layout
        setContentView(R.layout.activity_main);

        // Check for camera permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                startCamera();
            }
        } else {
            startCamera();
        }

        // Find the camera button in the layout
        Button cameraBtn = findViewById(R.id.cameraBtn);

        // Set a click listener for the camera button
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the startCamera() method when the camera button is pressed
                startCamera();
            }
        });
    }

    private void startCamera() {
        // Create a new instance of CameraCapture and start capturing images
        cameraCapture = new CameraCapture();
        cameraCapture.startCapture(this, executor);

        // Schedule to stop capturing images after 3 seconds using a Handler
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
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                // Handle denied camera permission
            }
        }
    }
}
