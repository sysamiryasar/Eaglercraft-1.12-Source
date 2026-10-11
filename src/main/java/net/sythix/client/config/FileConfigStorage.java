package net.sythix.client.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;

public class FileConfigStorage implements ConfigStorage
{
    private final File file;

    public FileConfigStorage(File file)
    {
        this.file = file;
    }

    public File getFile()
    {
        return this.file;
    }

    public String read() throws IOException
    {
        return this.file.isFile() ? FileUtils.readFileToString(this.file, StandardCharsets.UTF_8) : null;
    }

    public void write(String text) throws IOException
    {
        File parent = this.file.getParentFile();

        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
        {
            throw new IOException("Could not create " + parent);
        }

        // Write to a temporary file first so a crash mid-save cannot corrupt the config
        File temp = new File(this.file.getPath() + ".tmp");
        FileUtils.writeStringToFile(temp, text, StandardCharsets.UTF_8);

        if (this.file.exists() && !this.file.delete())
        {
            throw new IOException("Could not replace " + this.file);
        }

        if (!temp.renameTo(this.file))
        {
            throw new IOException("Could not move " + temp + " to " + this.file);
        }
    }
}
