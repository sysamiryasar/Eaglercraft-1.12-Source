package net.sythix.client.module.recording;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * Writes Motion-JPEG video in an AVI file (plays in VLC, Windows Media Player, most editors).
 * Frames are JPEG images; the header and index are filled in when the file is closed.
 */
public class AviWriter
{
    private final RandomAccessFile file;
    private final int width;
    private final int height;
    private final int fps;
    private final ByteArrayOutputStream index = new ByteArrayOutputStream();
    private long moviStart;
    private int frames;
    private int maxFrameSize;

    public AviWriter(File output, int width, int height, int fps) throws IOException
    {
        this.file = new RandomAccessFile(output, "rw");
        this.file.setLength(0L);
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.writeHeader();
    }

    private void fourcc(String s) throws IOException
    {
        this.file.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private void int32(int v) throws IOException
    {
        this.file.write(v & 255);
        this.file.write(v >>> 8 & 255);
        this.file.write(v >>> 16 & 255);
        this.file.write(v >>> 24 & 255);
    }

    private void int16(int v) throws IOException
    {
        this.file.write(v & 255);
        this.file.write(v >>> 8 & 255);
    }

    private static void int32(ByteArrayOutputStream out, int v)
    {
        out.write(v & 255);
        out.write(v >>> 8 & 255);
        out.write(v >>> 16 & 255);
        out.write(v >>> 24 & 255);
    }

    /** Header layout (offsets are fixed, so close() can patch the counts). */
    private void writeHeader() throws IOException
    {
        this.fourcc("RIFF");
        this.int32(0); // file size - 8, patched
        this.fourcc("AVI ");
        this.fourcc("LIST");
        this.int32(4 + 8 + 56 + 8 + 4 + 8 + 56 + 8 + 40);
        this.fourcc("hdrl");
        this.fourcc("avih");
        this.int32(56);
        this.int32(1000000 / this.fps); // microseconds per frame
        this.int32(0); // max bytes per second
        this.int32(0); // padding
        this.int32(0x10); // AVIF_HASINDEX
        this.int32(0); // total frames, patched (offset 48)
        this.int32(0);
        this.int32(1); // streams
        this.int32(0); // suggested buffer size, patched (offset 60)
        this.int32(this.width);
        this.int32(this.height);
        this.int32(0);
        this.int32(0);
        this.int32(0);
        this.int32(0);
        this.fourcc("LIST");
        this.int32(4 + 8 + 56 + 8 + 40);
        this.fourcc("strl");
        this.fourcc("strh");
        this.int32(56);
        this.fourcc("vids");
        this.fourcc("MJPG");
        this.int32(0);
        this.int16(0);
        this.int16(0);
        this.int32(0);
        this.int32(1); // scale
        this.int32(this.fps); // rate
        this.int32(0); // start
        this.int32(0); // length in frames, patched (offset 140)
        this.int32(0); // suggested buffer size, patched (offset 144)
        this.int32(-1); // quality
        this.int32(0);
        this.int16(0);
        this.int16(0);
        this.int16(this.width);
        this.int16(this.height);
        this.fourcc("strf");
        this.int32(40);
        this.int32(40);
        this.int32(this.width);
        this.int32(this.height);
        this.int16(1);
        this.int16(24);
        this.fourcc("MJPG");
        this.int32(this.width * this.height * 3);
        this.int32(0);
        this.int32(0);
        this.int32(0);
        this.int32(0);
        this.fourcc("LIST");
        this.int32(0); // movi size, patched
        this.moviStart = this.file.getFilePointer();
        this.fourcc("movi");
    }

    /** Adds one JPEG frame. */
    public synchronized void addFrame(byte[] jpeg) throws IOException
    {
        long offset = this.file.getFilePointer() - this.moviStart;
        this.fourcc("00dc");
        this.int32(jpeg.length);
        this.file.write(jpeg);

        if ((jpeg.length & 1) == 1)
        {
            this.file.write(0);
        }

        this.index.write("00dc".getBytes(StandardCharsets.US_ASCII), 0, 4);
        int32(this.index, 0x10); // keyframe
        int32(this.index, (int)offset);
        int32(this.index, jpeg.length);
        ++this.frames;
        this.maxFrameSize = Math.max(this.maxFrameSize, jpeg.length);
    }

    public int getFrames()
    {
        return this.frames;
    }

    public synchronized void close() throws IOException
    {
        long moviEnd = this.file.getFilePointer();
        this.fourcc("idx1");
        this.int32(this.index.size());
        this.file.write(this.index.toByteArray());
        long end = this.file.getFilePointer();
        this.file.seek(4);
        this.int32((int)(end - 8));
        this.file.seek(48);
        this.int32(this.frames);
        this.file.seek(60);
        this.int32(this.maxFrameSize);
        this.file.seek(140);
        this.int32(this.frames);
        this.int32(this.maxFrameSize);
        this.file.seek(this.moviStart - 4);
        this.int32((int)(moviEnd - this.moviStart));
        this.file.close();
    }
}
