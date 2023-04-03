package net.smallacademy.cameraandgallery;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;

public class ImageUtils {

    public static Image yuv420888ToImage(byte[] yuvData, int width, int height, long timeStamp) {
        ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Plane[] planes = image.getPlanes();
                planes[0].getBuffer().put(yuvData, 0, width * height);
                planes[1].getBuffer().put(yuvData, width * height, width * height / 4);
                planes[2].getBuffer().put(yuvData, width * height * 5 / 4, width * height / 4);
                image.setTimestamp(timeStamp);
            }
        }, null);
        return imageReader.acquireLatestImage();
    }
}
