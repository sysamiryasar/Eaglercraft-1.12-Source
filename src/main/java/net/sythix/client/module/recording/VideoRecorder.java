package net.sythix.client.module.recording;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.sythix.client.core.Notifications;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;

/**
 * Built-in screen recorder. Every captured frame is read from the game's framebuffer on the
 * render thread, then a background thread turns it into JPEG and appends it to an AVI file in
 * the "recordings" folder. Video only - sound is not recorded.
 */
public final class VideoRecorder
{
    private static final Logger LOGGER = LogManager.getLogger();

    /** A captured frame waiting to be encoded (null pixels = stop). */
    private static final class Frame
    {
        final int[] pixels;
        final int width;
        final int height;
        final int repeat;

        Frame(int[] pixels, int width, int height, int repeat)
        {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
            this.repeat = repeat;
        }
    }

    private static volatile boolean recording;
    private static BlockingQueue<Frame> queue;
    private static Thread encoder;
    private static IntBuffer buffer;
    private static long startNanos;
    private static long framesDue;
    private static int fps;
    private static int dropped;
    private static File output;

    private VideoRecorder()
    {
    }

    public static boolean isRecording()
    {
        return recording;
    }

    public static File getOutput()
    {
        return output;
    }

    public static long elapsedMillis()
    {
        return recording ? (System.nanoTime() - startNanos) / 1000000L : 0L;
    }

    /** Starts recording; scale 1 = full size, 0.5 = half. */
    public static void start(int framesPerSecond, float scale, float quality)
    {
        if (recording)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        File dir = new File(mc.mcDataDir, "recordings");
        dir.mkdirs();
        output = new File(dir, new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ".avi");
        // Even sizes keep video players happy
        int width = Math.max(2, Math.round((float)mc.displayWidth * scale)) & ~1;
        int height = Math.max(2, Math.round((float)mc.displayHeight * scale)) & ~1;
        final AviWriter writer;

        try
        {
            writer = new AviWriter(output, width, height, framesPerSecond);
        }
        catch (Exception e)
        {
            Notifications.post("Recording failed", "Could not create the video file: " + e.getMessage(), Notifications.Kind.ERROR);
            return;
        }

        fps = framesPerSecond;
        dropped = 0;
        framesDue = 0L;
        startNanos = System.nanoTime();
        queue = new ArrayBlockingQueue<Frame>(6);
        final float jpegQuality = quality;
        encoder = new Thread(() -> encode(writer, width, height, jpegQuality), "Sythix video encoder");
        encoder.setDaemon(true);
        encoder.start();
        recording = true;
        Notifications.post("Recording started", "Saving to recordings/" + output.getName(), Notifications.Kind.SUCCESS);
    }

    public static void stop()
    {
        if (!recording)
        {
            return;
        }

        recording = false;

        try
        {
            queue.put(new Frame(null, 0, 0, 0));
        }
        catch (InterruptedException ignored)
        {
            Thread.currentThread().interrupt();
        }
    }

    /** Stops and waits (up to 5 s) until the file is complete, e.g. when the game closes. */
    public static void stopAndWait()
    {
        Thread thread = encoder;
        stop();

        if (thread != null)
        {
            try
            {
                thread.join(5000L);
            }
            catch (InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Called every rendered frame on the render thread. */
    public static void onFrame()
    {
        if (!recording)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        long due = (System.nanoTime() - startNanos) * (long)fps / 1000000000L + 1L;

        if (due <= framesDue)
        {
            return;
        }

        // If the game runs slower than the video, repeat this frame so the timing stays right
        int repeat = (int)Math.min(30L, due - framesDue);
        long previousDue = framesDue;
        framesDue = due;
        int w = mc.displayWidth;
        int h = mc.displayHeight;

        if (buffer == null || buffer.capacity() < w * h)
        {
            buffer = BufferUtils.createIntBuffer(w * h);
        }

        buffer.clear();
        GlStateManager.glPixelStorei(3333, 1);
        GlStateManager.glReadPixels(0, 0, w, h, 32993, 33639, buffer);
        int[] pixels = new int[w * h];
        buffer.get(pixels);

        if (!queue.offer(new Frame(pixels, w, h, repeat)))
        {
            // The encoder is behind (slow CPU): skip this frame rather than freeze the game; the
            // next frame fills its time so the video length still matches real time
            framesDue = previousDue;
            ++dropped;
        }
    }

    private static void encode(AviWriter writer, int width, int height, float quality)
    {
        ImageWriter jpeg = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = jpeg.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        byte[] last = null;
        String error = null;

        try
        {
            while (true)
            {
                Frame frame = queue.take();

                if (frame.pixels == null)
                {
                    break;
                }

                // OpenGL rows start at the bottom: flip while copying
                BufferedImage source = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB);

                for (int y = 0; y < frame.height; ++y)
                {
                    source.setRGB(0, frame.height - 1 - y, frame.width, 1, frame.pixels, y * frame.width, frame.width);
                }

                Graphics2D g = target.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source, 0, 0, width, height, null);
                g.dispose();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();

                try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes))
                {
                    jpeg.setOutput(out);
                    jpeg.write(null, new IIOImage(target, null, null), param);
                }

                last = bytes.toByteArray();

                for (int i = 0; i < frame.repeat; ++i)
                {
                    writer.addFrame(last);
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("[Sythix] Video encoding failed", e);
            error = e.toString();
            recording = false;
        }
        finally
        {
            jpeg.dispose();

            try
            {
                writer.close();
            }
            catch (Exception e)
            {
                error = e.toString();
            }
        }

        int frames = writer.getFrames();
        String message = error != null ? "Error: " + error : output.getName() + " - " + frames / Math.max(1, fps) + "s" + (dropped > 0 ? ", " + dropped + " frames skipped (try a lower FPS or Half size)" : "");
        boolean ok = error == null;
        Minecraft.getMinecraft().addScheduledTask(() -> Notifications.post(ok ? "Recording saved" : "Recording failed", message, ok ? Notifications.Kind.SUCCESS : Notifications.Kind.ERROR));
    }
}
