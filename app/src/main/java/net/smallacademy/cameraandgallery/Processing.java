package net.smallacademy.cameraandgallery;

import android.util.Log;

public class Processing {
    private static final String TAG = "Processing";

    private Processing() {}

    public static void processYuv420888(byte[] yuvData, int width, int height, long timeStamp) {
        Log.d(TAG, "Received YUV data for processing");
        Log.d(TAG, "Width: " + width + ", Height: " + height + ", Timestamp: " + timeStamp);
    }
}
