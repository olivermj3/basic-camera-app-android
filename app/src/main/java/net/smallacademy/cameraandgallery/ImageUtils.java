package net.smallacademy.cameraandgallery;


import android.graphics.ImageFormat;
import android.media.Image;
import android.media.Image.Plane;
import java.nio.ByteBuffer;

public class ImageUtils {
    public static class MyPlane {
        private final ByteBuffer buffer;
        private final int pixelStride;
        private final int rowStride;

        public MyPlane(ByteBuffer buffer, int pixelStride, int rowStride) {
            this.buffer = buffer;
            this.pixelStride = pixelStride;
            this.rowStride = rowStride;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public int getPixelStride() {
            return pixelStride;
        }

        public int getRowStride() {
            return rowStride;
        }
    }
    public static Image yuv420888ToImage(byte[] yuvData, int width, int height, long timeStamp) {
        ByteBuffer buffer = ByteBuffer.wrap(yuvData);
        Plane[] planes = new Plane[3];

        // Y plane
        planes[0] = new Plane() {
            @Override
            public ByteBuffer getBuffer() {
                return buffer.slice();
            }

            @Override
            public int getPixelStride() {
                return 1;
            }

            @Override
            public int getRowStride() {
                return width;
            }
        };

        // U plane
        planes[1] = new Plane() {
            @Override
            public ByteBuffer getBuffer() {
                ByteBuffer newBuffer = buffer.slice();
                newBuffer.position(width * height);
                return newBuffer;
            }

            @Override
            public int getPixelStride() {
                return 2;
            }

            @Override
            public int getRowStride() {
                return width;
            }
        };

        // V plane
        planes[2] = new Plane() {
            @Override
            public ByteBuffer getBuffer() {
                ByteBuffer newBuffer = buffer.slice();
                newBuffer.position(width * height + 1);
                return newBuffer;
            }

            @Override
            public int getPixelStride() {
                return 2;
            }

            @Override
            public int getRowStride() {
                return width;
            }
        };

        Image image = new Image() {
            @Override
            public int getFormat() {
                return ImageFormat.YUV_420_888;
            }

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public long getTimestamp() {
                return timeStamp;
            }

            @Override
            public Plane[] getPlanes() {
                return planes;
            }

            @Override
            public void close() {
                // Do nothing
            }
        };

        return image;
    }
    public static byte[] imageToByteArray(Image image) {
        MyPlane[] planes = new MyPlane[3];

        for (int i = 0; i < 3; i++) {
            Image.Plane plane = image.getPlanes()[i];
            planes[i] = new MyPlane(plane.getBuffer(), plane.getPixelStride(), plane.getRowStride());
        }

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] byteArray = new byte[ySize + uSize + vSize];
        yBuffer.get(byteArray, 0, ySize);
        uBuffer.get(byteArray, ySize, uSize);
        vBuffer.get(byteArray, ySize + uSize, vSize);
        return byteArray;
    }
}
