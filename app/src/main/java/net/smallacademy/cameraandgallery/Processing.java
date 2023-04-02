package net.smallacademy.cameraandgallery;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import com.google.mediapipe.framework.Graph;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.glutil.EglManager;

import org.webrtc.EglBase;


public class Processing {
    private Graph graph;
    private EglManager eglManager;
    private EglBase eglBase;
    private Packet yPacket;
    private Packet uPacket;
    private Packet vPacket;
    private ExecutorService executorService;
    private Landmarks landmarks;

    public void init() {
        graph = new Graph();
        eglManager = new EglManager(null);
        eglBase = EglBase.create();
        executorService = Executors.newSingleThreadExecutor();
        landmarks = new Landmarks();

        // Set up input and output streams
        graph.addPacketCallback("y", (packet) -> {
            yPacket = packet;
        });
        graph.addPacketCallback("u", (packet) -> {
            uPacket = packet;
        });
        graph.addPacketCallback("v", (packet) -> {
            vPacket = packet;
        });

        graph.addPacketCallback("output_image", (outputPacket) -> {
            executorService.submit(() -> {
                // Get the output texture frame
                TextureFrame outputTextureFrame = PacketGetter.getTextureFrame(outputPacket);

                // Convert the texture frame to a Bitmap
                Bitmap bitmap = textureFrameToBitmap(outputTextureFrame);

                // Process the Bitmap image data here
            });
        });
        graph.startRunningGraph();
    }

    public void processImage(ByteBuffer yBuffer, ByteBuffer uBuffer, ByteBuffer vBuffer) {
        // Convert ByteBuffers to byte arrays
        byte[] yByteArray = new byte[yBuffer.remaining()];
        yBuffer.get(yByteArray);
        byte[] uByteArray = new byte[uBuffer.remaining()];
        uBuffer.get(uByteArray);
        byte[] vByteArray = new byte[vBuffer.remaining()];
        vBuffer.get(vByteArray);

        // Create input packets from the YUV byte arrays
        yPacket = byteArrayToPacket(yByteArray);
        uPacket = byteArrayToPacket(uByteArray);
        vPacket = byteArrayToPacket(vByteArray);

        // Generate the timestamp
        long timestamp = System.nanoTime() / 1000;

        // Send the input packets to the graph
        graph.addPacketToInputStream("yuv", yPacket, timestamp);
        graph.addPacketToInputStream("yuv", uPacket, timestamp);
        graph.addPacketToInputStream("yuv", vPacket, timestamp);
    }

    private Packet byteArrayToPacket(byte[] byteArray) {
        try {
            Method createByteArrayMethod = Packet.class.getDeclaredMethod("createByteArray", byte[].class);
            createByteArrayMethod.setAccessible(true);
            return (Packet) createByteArrayMethod.invoke(null, (Object) byteArray);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create packet from byte array", e);
        }
    }

    private Bitmap textureFrameToBitmap(TextureFrame textureFrame) {
        eglBase.makeCurrent();

        int width = textureFrame.getWidth();
        int height = textureFrame.getHeight();

        int[] frameBuffer = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        int[] renderBuffer = new int[1];
        GLES20.glGenRenderbuffers(1, renderBuffer, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGBA, width, height);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, renderBuffer[0]);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureFrame.getTextureName(), 0);

        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteRenderbuffers(1, renderBuffer, 0);
        GLES20.glDeleteFramebuffers(1, frameBuffer, 0);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        pixelBuffer.rewind();
        bitmap.copyPixelsFromBuffer(pixelBuffer);

        landmarks.processImage(bitmap);

        return bitmap;
    }


    public void stop() {
        // Stop running the graph
        graph.cancelGraph();

        // Release the input packets
        yPacket.release();
        uPacket.release();
        vPacket.release();
    }
}