package net.smallacademy.cameraandgallery;

import android.media.Image;
import android.media.Image.Plane;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import android.util.Log;

public class Processing {
    private static final String TAG = "Processing";
    private Image image;
    private Gson gson;

    // Defined Points
    static int[] left_eye = { 130, 247, 30, 29, 28, 27, 56, 190, 243, 112, 26, 22, 23, 24, 110, 25, 33, 246, 161, 160, 159, 158, 157, 173, 155, 154, 153, 145, 144, 163, 7};
    static int[] right_eye = {463, 414, 286, 258, 257, 259, 260, 467, 359, 255, 339, 254, 253, 252, 256, 341, 362, 398, 384, 385, 386, 387, 388, 466, 263, 249, 390, 373, 374, 380, 381, 382};
    static int[] mouth = {0, 11, 12, 13, 14, 15, 16, 17, 37, 72, 38, 82, 87, 86, 85, 84, 39, 73, 41, 81, 178, 179, 180, 181, 40, 74, 42, 80, 88, 89, 90, 91, 185, 184, 183, 191, 95, 96, 77, 146, 67, 76, 62, 78, 267, 302, 268, 312, 317, 316, 315, 314, 269, 303, 271, 311, 402, 403, 404, 405, 270, 304, 272, 310, 318, 319, 320, 321, 409, 408, 407, 415, 324, 325, 307, 375, 308, 291, 306, 292};
    static int[] left_fh_t = {54, 68, 104, 69, 67, 103};
    static int[] left_fh_b = {68, 63, 105, 66, 69, 104};
    static int[] center_fh_lt = {67, 69, 108, 151, 10, 109};
    static int[] center_fh_lb = {69, 66, 107, 9, 151, 108};
    static int[] center_fh_rt = {10, 151, 337, 299, 297, 338};
    static int[] center_fh_rb = {151, 9, 336, 296, 299, 337};
    static int[] right_fh_t = {297, 299, 333, 298, 284, 332};
    static int[] right_fh_b = {299, 296, 334, 293, 298, 333};
    static int[] center_fh_b = {107, 55, 193, 168, 417, 285, 336, 9};
    static int[] nose_top = {193, 122, 196, 197, 419, 351, 417, 168};
    static int[] nose_bot = {196, 3, 51, 45, 275, 281, 248, 419, 197};
    static int[] lc_t = {31, 117, 50, 101, 100, 47, 114, 121, 230, 229};
    static int[] lc_b = {50, 187, 207, 206, 203, 129, 142, 101};
    static int[] rc_t = {261, 346, 280, 330, 329, 277, 343, 350, 450, 449, 448};
    static int[] rc_b = {280, 411, 427, 426, 423, 358, 371, 330};
    static int[][] all_rois = {left_eye, right_eye,mouth, left_fh_t, left_fh_b, center_fh_lt, center_fh_lb, center_fh_rt, center_fh_rb, right_fh_t, right_fh_b, center_fh_b, nose_top, nose_bot, lc_t, lc_b, rc_t, rc_b};

    private Processing(@NonNull Image image) {
        this.image = image;
        this.gson = new Gson();

        InputImage inputImage = InputImage.fromMediaImage(image, 0);
        extractColorValues(inputImage);
    }

    public static void processYuv420888(byte[] yuvData, int width, int height, long timeStamp) {
        Log.d(TAG, "processing YUV data...");
        Image image = ImageUtils.yuv420888ToImage(yuvData, width, height, timeStamp);
        new Processing(image);
    }

    private void extractColorValues(InputImage inputImage) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 1) {
                        Face face = faces.get(0);
                        HashMap<String, Integer> colorValues = new HashMap<>();
                        for (int[] roi : all_rois) {
                            for (int point : roi) {
                                int x = (point * face.getBoundingBox().width()) / 1000;
                                int y = (point * face.getBoundingBox().height()) / 1000;
                                int colorValue = getColorValue(inputImage, x, y);
                                colorValues.put(String.valueOf(point), colorValue);
                            }
                        }

                        saveColorValuesToJson(colorValues);
                        printColorValues(colorValues);
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace)
                .addOnCompleteListener(task -> detector.close());
    }

    private int getColorValue(InputImage inputImage, int x, int y) {
        Plane[] planes = inputImage.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yIndex = y * inputImage.getWidth() + x;
        int uvIndex = (y / 2) * planes[1].getRowStride() + (x / 2);

        int yValue = yBuffer.get(yIndex) & 0xFF;
        int uValue = uBuffer.get(uvIndex) & 0xFF;
        int vValue = vBuffer.get(uvIndex) & 0xFF;

        int r = (int) (1.164 * (yValue - 16) + 1.596 * (vValue - 128));
        int g = (int) (1.164 * (yValue - 16) - 0.813 * (vValue - 128) - 0.392 * (uValue - 128));
        int b = (int) (1.164 * (yValue - 16) + 2.017 * (uValue - 128));

        return 0xFF000000 | (clip(r) << 16) | (clip(g) << 8) | clip(b);
    }



    private int clip(int value) {
        return Math.min(Math.max(value, 0), 255);
    }

    private void saveColorValuesToJson(HashMap<String, Integer> colorValues) {
        String jsonString = gson.toJson(colorValues);
        File file = new File("color_values.json");
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(jsonString.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error writing color values to file", e);
            e.printStackTrace();
        }
        Log.d(TAG, "Color values saved to file");
    }

    private void printColorValues(HashMap<String, Integer> colorValues) {
        for (HashMap.Entry<String, Integer> entry : colorValues.entrySet()) {
            System.out.println("Color value at " + entry.getKey() + ": " + entry.getValue());
        }
    }
}